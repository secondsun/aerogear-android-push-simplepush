package org.jboss.aerogear.android.impl.simplepush;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ServiceTests {

    @Test
    public void onStartCommandReturnsSticky() {
        
        Robolectric.getShadowApplication().startService(new Intent(Robolectric.getShadowApplication().getApplicationContext(), SimplePushService.class));
        //int start = service.onStartCommand(new Intent(), 0, 0);
        assertEquals(Service.START_STICKY, 0);
    }
    
    /*see https://wiki.mozilla.org/WebAPI/SimplePush/Protocol#Handshake*/
    @Test
    public void testHandshake() throws InterruptedException {
        
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        final AtomicReference<SimplePushService> serviceRef = new AtomicReference<SimplePushService>();
        ServiceConnection testConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceRef.set(((SimplePushService.SimplePushBinder) service).getService());
                connectionLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                connectionLatch.countDown();
            }
        };
        
        Robolectric.getShadowApplication().bindService(new Intent(Robolectric.getShadowApplication().getApplicationContext(), SimplePushService.class), 
                                                        testConnection, Context.BIND_AUTO_CREATE);
        
        if (!connectionLatch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Not Connected");
        }
        
        throw new IllegalStateException("Not implemented");
    }

    /*see https://wiki.mozilla.org/WebAPI/SimplePush/Protocol#Ping*/
    @Test
    public void testPing() {
        throw new IllegalStateException("Not implemented");
    }

    @Test
    public void testNotification() {
        throw new IllegalStateException("Not implemented");
    }

}
