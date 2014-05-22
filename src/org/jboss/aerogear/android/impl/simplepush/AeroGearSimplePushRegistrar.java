package org.jboss.aerogear.android.impl.simplepush;

import android.content.Context;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;

public class AeroGearSimplePushRegistrar implements PushRegistrar {

    public AeroGearSimplePushRegistrar(PushConfig config) {
    }

    @Override
    public void register(Context context, Callback<Void> callback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unregister(Context context, Callback<Void> callback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
