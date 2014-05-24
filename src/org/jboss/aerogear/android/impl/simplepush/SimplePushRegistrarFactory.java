package org.jboss.aerogear.android.impl.simplepush;

import org.jboss.aerogear.android.impl.unifiedpush.DefaultPushRegistrarFactory;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;


public class SimplePushRegistrarFactory extends DefaultPushRegistrarFactory {

    @Override
    public PushRegistrar createPushRegistrar(PushConfig config) {
        if (config.getType().equals(SimplePushTypes.TYPE)) {
            return new AeroGearSimplePushRegistrar((SimplePushConfig)config);
        } else {
            return super.createPushRegistrar(config); 
        }
    }
   
    
    
}
