package org.jboss.aerogear.android.impl.simplepush;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.jboss.aerogear.android.Callback;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class WebSocketClientTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
  //you other setup here
    }

    @Test(timeout = 10000)
    public void testDefaultOpenConnection() throws InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, JSONException {

        final AtomicReference<String> stringRef = new AtomicReference<String>();

        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket")) {

            @Override
            public void send(String text) throws NotYetConnectedException {
                stringRef.set(text);
                super.send(text);
            }

        };

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        client.connect();
        Thread.sleep(1000);
        client.close();

        JSONObject expected = new JSONObject("{\"messageType\" : \"hello\", \"uaid\" : \"\"}");
        JSONObject result = new JSONObject(stringRef.get());

        assertEquals(expected.toString(), result.toString());

    }

    @Test(timeout = 10000)
    public void testDefaultOpenConnectionSetsUAID() throws InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, JSONException {

        final AtomicReference<String> stringRef = new AtomicReference<String>();

        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket"));

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        Thread.sleep(1000);
        client.connect();
        Thread.sleep(1000);
        client.close();

        Assert.assertTrue(client.getUAID().isPresent());

    }

    @Test(timeout = 10000)
    public void testSettingAnUAIWillBeSentOnOpen() throws InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, JSONException {

        final AtomicReference<String> stringRef = new AtomicReference<String>();

        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket")) {

            @Override
            public void send(String text) throws NotYetConnectedException {
                stringRef.set(text);
            }

        };

        client.setUAID("testUAID");

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        client.connect();
        Thread.sleep(1000);
        client.close();

        JSONObject expected = new JSONObject("{\"messageType\" : \"hello\", \"uaid\" : \"testUAID\"}");
        JSONObject result = new JSONObject(stringRef.get());

        assertEquals(expected.toString(), result.toString());

    }

    @Test(timeout = 10000)
    public void testSendDefaultChannelIDsOnConnect() throws InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, JSONException {

        final AtomicReference<String> stringRef = new AtomicReference<String>();

        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket")) {

            @Override
            public void send(String text) throws NotYetConnectedException {
                stringRef.set(text);
            }

        };

        client.setUAID("testUAID");
        client.addChannelID("testChannel1");
        client.addChannelID("testChannel2");

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        client.connect();
        Thread.sleep(1000);
        client.close();

        JSONObject expected = new JSONObject("{\"messageType\" : \"hello\", \"uaid\" : \"testUAID\", \"channelIDs\" : [\"testChannel1\", \"testChannel2\"]}");
        JSONObject result = new JSONObject(stringRef.get());

        assertEquals(expected.toString(), result.toString());

    }

    @Test(timeout = 10000)
    public void testSendRegister() throws InterruptedException, NoSuchAlgorithmException {
        final AtomicReference<String> sendStringRef = new AtomicReference<String>();
        final AtomicReference<String> channelIDRef = new AtomicReference<String>();
        final CountDownLatch latch = new CountDownLatch(1);

        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket")) {

            @Override
            public void send(String text) throws NotYetConnectedException {
                sendStringRef.set(text);
                super.send(text);
            }

        };

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        client.connect();
        Thread.sleep(1000);

        client.registerChannel(new Callback<PushChannel>() {

            @Override
            public void onSuccess(PushChannel channel) {
                channelIDRef.set(channel.getChannelId());
                Assert.assertTrue(channel.getEndpoint().toString().startsWith("https://push"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                System.err.print(e);
                latch.countDown();
            }
        });

        latch.await(3, TimeUnit.SECONDS);

        client.close();

        String expectedSentMessage = "{\"messageType\":\"register\",\"channelID\":\"" + channelIDRef.get() + "\"}";

        assertEquals(expectedSentMessage, sendStringRef.get());

    }

    @Test(timeout = 15000)//The test includes a 10 second timeout in the ping method
    public void testPingSend() throws InterruptedException, NoSuchAlgorithmException {
        final AtomicReference<String> sendStringRef = new AtomicReference<String>();
        SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket")) {
            @Override
            public void send(String text) throws NotYetConnectedException {
                sendStringRef.set(text);
                super.send(text);
            }
        };

        SSLContext sslContext = SSLContext.getDefault();
        client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

        client.connect();
        Thread.sleep(2000);

        long pingSentAt = System.currentTimeMillis();
        client.ping();
        assertEquals("{}", sendStringRef.get());
        Thread.sleep(5000);
        client.closeBlocking();
        Assert.assertTrue(client.lastMessageTimestamp() > pingSentAt);
    }

}
