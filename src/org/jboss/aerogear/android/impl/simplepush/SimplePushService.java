package org.jboss.aerogear.android.impl.simplepush;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.aerogear.android.Callback;

public class SimplePushService extends Service {

    private int serviceId;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private SimplePushWebsocketClient client;
    private static final String TAG = SimplePushService.class.getSimpleName();
    private CountDownLatch connectionLatch = new CountDownLatch(1);
    private Handler handler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.serviceId = startId;

        synchronized (this) {
            if (handler == null) {
                HandlerThread thread = new HandlerThread(TAG);
                thread.start();
                handler = new Handler(thread.getLooper());
            }
        }

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
        client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket"));
        client.connect(connectionLatch);
    }

    public Future<PushChannel> register(String category) throws TimeoutException, InterruptedException {
        final CountDownLatch registerLatch = new CountDownLatch(1);
        final AtomicReference<PushChannel> pushChannelReference = new AtomicReference<PushChannel>();
        if (!connectionLatch.await(9000, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Could not register the channel.");
        }
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

        return EXECUTOR.submit(new Callable<PushChannel>() {

            @Override
            public PushChannel call() throws Exception {
                try {
                    if (!registerLatch.await(3000, TimeUnit.SECONDS)) {
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
        });
    }

    public class SimplePushBinder extends Binder {

        public SimplePushService getService() {
            return SimplePushService.this;
        }

    }

}
