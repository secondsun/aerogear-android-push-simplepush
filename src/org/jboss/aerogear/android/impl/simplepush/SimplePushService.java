package org.jboss.aerogear.android.impl.simplepush;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.aerogear.android.Callback;

public class SimplePushService extends Service {

    private int serviceId;

    SimplePushWebsocketClient client;
    private static final String TAG = SimplePushService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.serviceId = startId;

        if (intent == null) {
            handleRestart();
        } else {
            startNewWebSocket();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new SimplePushBinder();
    }

    private void handleRestart() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void startNewWebSocket() {
        try {
            client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket"));
            client.connectBlocking();
        } catch (InterruptedException ex) {
            Logger.getLogger(SimplePushService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public PushChannel register(String category) throws TimeoutException {
        final CountDownLatch registerLatch = new CountDownLatch(1);
        final AtomicReference<PushChannel> pushChannelReference = new AtomicReference<PushChannel>();
        client.registerChannel(new Callback<PushChannel>() {

            @Override
            public void onSuccess(PushChannel data) {
                pushChannelReference.set(data);
                registerLatch.countDown();
            }

            @Override
            public void onFailure(Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                registerLatch.countDown();
            }
        });
        try {
            if (!registerLatch.await(30, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }

        } catch (InterruptedException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
        
        PushChannel urlResult = pushChannelReference.get();
        if (urlResult == null) {
            throw new IllegalStateException("Could not register the channel.");
        }
        
        return urlResult;
    }

    public class SimplePushBinder extends Binder {

        public SimplePushService getService() {
            return SimplePushService.this;
        }

    }

}
