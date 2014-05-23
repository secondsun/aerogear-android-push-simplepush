package org.jboss.aerogear.android.impl.simplepush;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;

public class AeroGearSimplePushRegistrar implements PushRegistrar {

    private final PushConfig config;
    private SimplePushService service;

    public AeroGearSimplePushRegistrar(PushConfig config) {
        this.config = config;
    }

    @Override
    public void register(final Context context, Callback<Void> callback) {
        final Looper callerLooper = Looper.myLooper();

        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {

            @Override
            public void run() {
                synchronized (AeroGearSimplePushRegistrar.class) {
                    if (service == null) {
                        context.startService(new Intent(context, SimplePushService.class));
                        context.bindService(new Intent(context, SimplePushService.class), new ServiceConnection() {

                            @Override
                            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                                SimplePushService.SimplePushBinder binder = (SimplePushService.SimplePushBinder) iBinder;
                                AeroGearSimplePushRegistrar.this.service = binder.getService();
                                throw new IllegalStateException("Not yet implemented");
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                service = null;
                            }
                        }, Context.BIND_AUTO_CREATE);
                    } else {
                        service.
                    }
                }
            }
        });

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregister(Context context, Callback<Void> callback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
