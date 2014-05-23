package org.jboss.aerogear.android.impl.simplepush;

import org.jboss.aerogear.android.unifiedpush.PushType;

public enum SimplePushTypes implements PushType {

    TYPE;
    
    @Override
    public String getName() {
        return "SimplePush";
    }
    
}
