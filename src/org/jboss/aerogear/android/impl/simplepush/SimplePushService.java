package org.jboss.aerogear.android.impl.simplepush;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimplePushService extends Service {

    private int serviceId;

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
            SimplePushWebsocketClient client = new SimplePushWebsocketClient(URI.create("wss://push-coffeeregister.rhcloud.com:8443/simplepush/websocket"));
            client.connectBlocking();
        } catch (InterruptedException ex) {
            Logger.getLogger(SimplePushService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public class SimplePushBinder extends Binder {

        public SimplePushService getService() {
            return SimplePushService.this;
        }

    }


}
