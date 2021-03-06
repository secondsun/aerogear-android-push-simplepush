package org.jboss.aerogear.android.impl.simplepush;

import android.util.Log;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpStatus;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.http.HttpException;

public class SimplePushWebsocketClient extends WebSocketClient {

    private static final String TAG = SimplePushWebsocketClient.class.getSimpleName();
    private static final String MESSAGE_TYPE = "messageType";
    private static final String CHANNEL_IDS = "channelIDs";
    private static final String CHANNEL_ID = "channelID";
    private static final String PUSH_ENDPOINT = "pushEndpoint";
    private static final String UAID = "uaid";
    private static final String STATUS = "status";

    private String uaid = null;
    private List<String> channelIDs = ImmutableList.of();
    private Map<String, Callback<PushChannel>> registrationMap = new HashMap<String, Callback<PushChannel>>();
    private long lastMessage = -1;
    private CountDownLatch connectionLatch;

    public void connect(CountDownLatch connectionLatch) {
        this.connectionLatch = connectionLatch;
        new Thread(new Runnable() {

            @Override
            public void run() {
                connect();
            }
        }).start();
        
    }

    private enum MessageType {

        HELLO, REGISTER
    }

    public SimplePushWebsocketClient(URI uri) {
        super(uri, new Draft_17());
    }

    @Override
    public void onOpen(ServerHandshake sh) {
        JsonObject message = new JsonObject();
        Log.d(TAG, "Channel open");
        connectionLatch.countDown();
        message.addProperty(MESSAGE_TYPE, MessageType.HELLO.name().toLowerCase());
        message.addProperty(UAID, Strings.nullToEmpty(uaid));
        if (!channelIDs.isEmpty()) {
            List<String> channels = channelIDs;
            JsonArray channelIdsArray = new JsonArray();
            for (String channel : channels) {
                channelIdsArray.add(new JsonPrimitive(channel));
            }
            message.add(CHANNEL_IDS, channelIdsArray);
        }
        this.send(message.toString());
    }

    @Override
    public void onMessage(String string) {
        Log.d(TAG, string);
        lastMessage = now();

        try {
            JsonObject response = new JsonParser().parse(string).getAsJsonObject();

            if (!response.has(MESSAGE_TYPE)) {
                return;
            }

            MessageType messageType = MessageType.valueOf(response.get(MESSAGE_TYPE).getAsString().toUpperCase());
            switch (messageType) {
                case HELLO:
                    uaid = response.get(UAID).getAsString();
                    connectionLatch.countDown();
                    break;
                case REGISTER:
                    int status = response.get(STATUS).getAsInt();
                    String channelID = response.get(CHANNEL_ID).getAsString();
                    String pushEndpoint = response.get(PUSH_ENDPOINT).getAsString();
                    Callback<PushChannel> callback = registrationMap.get(channelID);
                    switch (status) {
                        case HttpStatus.SC_OK:

                            if (callback == null) {
                                //do Nothing but do not fail
                            } else {
                                registrationMap.remove(channelID);
                                callback.onSuccess(new PushChannel(pushEndpoint, channelID));
                            }
                            break;
                        case HttpStatus.SC_CONFLICT:
                            if (callback == null) {
                                //do Nothing but do not fail
                            } else {
                                //retry with new UUID
                                registrationMap.remove(channelID);
                                registerChannel(callback);
                            }
                            break;
                        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                            if (callback == null) {
                                Log.e(TAG, "The server returned a 500 error for channel:" + channelID);
                            } else {
                                //retry with new UUID
                                registrationMap.remove(channelID);
                                callback.onFailure(new HttpException(string.getBytes(), status));
                            }
                            break;
                        default:
                            if (callback == null) {
                                Log.e(TAG, "The server returned a " + status + " error for channel:" + channelID);
                            } else {
                                //retry with new UUID
                                registrationMap.remove(channelID);
                                callback.onFailure(new HttpException(string.getBytes(), status));
                            }
                    }

                    break;
                default:
                    throw new AssertionError(messageType.name());
            }
        } catch (JsonSyntaxException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        } catch (NullPointerException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }

    @Override
    public void onClose(int i, String string, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void onError(Exception excptn) {
        Log.e(TAG, excptn.getMessage(), excptn);
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    /**
     * A globally unique UserAgent ID. Used by the PushServer to associate
     * channelIDs with a client. Stored by the UserAgent, but opaque to it.
     *
     * @return an optional UAID.
     */
    public Optional<String> getUAID() {
        return Optional.fromNullable(uaid);
    }

    /**
     * A globally unique UserAgent ID. Used by the PushServer to associate
     * channelIDs with a client. Stored by the UserAgent, but opaque to it.
     *
     * @param uaid a new UAID, may be null.
     */
    public void setUAID(String uaid) {
        this.uaid = uaid;
    }

    /**
     * Unique identifier for a Channel. Generated by UserAgent for a particular
     * application. Opaque identifier for both UserAgent and PushServer. This
     * MUST NOT be exposed to an application.
     *
     * @return a copied, mutable List of channelIDs
     */
    public List<String> getChannelIDs() {
        return new ArrayList<String>(channelIDs);
    }

    /**
     * Unique identifier for a Channel. Generated by UserAgent for a particular
     * application. Opaque identifier for both UserAgent and PushServer. This
     * MUST NOT be exposed to an application.
     *
     * @param newChannelID a new Channel ID to add
     */
    public void addChannelID(String newChannelID) {
        channelIDs = ImmutableList.<String>builder().addAll(channelIDs).add(newChannelID).build();
    }

    /**
     * Unique identifier for a Channel. Generated by UserAgent for a particular
     * application. Opaque identifier for both UserAgent and PushServer. This
     * MUST NOT be exposed to an application.
     *
     * @param newChannelID a channelID to remove
     */
    public void removeChannelID(String newChannelID) {
        List<String> tempIDs = new ArrayList<String>(channelIDs);
        tempIDs.remove(newChannelID);
        channelIDs = ImmutableList.<String>builder().addAll(tempIDs).build();
    }

    /**
     * Unique identifier for a Channel. Generated by UserAgent for a particular
     * application. Opaque identifier for both UserAgent and PushServer. This
     * MUST NOT be exposed to an application.
     *
     * This method clears the set ChannelIDs
     */
    public void clearChannelIDs() {
        channelIDs = ImmutableList.of();
    }

    public void registerChannel(final Callback<PushChannel> callback) {
        JsonObject message = new JsonObject();
        message.addProperty(MESSAGE_TYPE, MessageType.REGISTER.name().toLowerCase());

        String channelID = UUID.randomUUID().toString();
        message.addProperty(CHANNEL_ID, channelID);

        registrationMap.put(channelID, callback);

        send(message.toString());
    }

    @Override
    public void send(final String text) throws NotYetConnectedException {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    connectionLatch.await(9000, TimeUnit.SECONDS); 
                    SimplePushWebsocketClient.super.send(text);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimplePushWebsocketClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        
    }

    
    
    public void ping() {
        send("{}");
    }

    /**
     *
     * It is useful to know when the last message was received in order to
     * performing ping maintenance.
     *
     * @return the timestamp of when the last message was received.
     */
    public long lastMessageTimestamp() {
        return lastMessage;
    }

    private long now() {
        return System.currentTimeMillis();
    }

}
