package org.jboss.aerogear.android.impl.simplepush;

import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushType;

public class SimplePushConfig extends PushConfig {

    String simplePushEndpoint;

    @Override
    public PushType getType() {
        return SimplePushTypes.TYPE;
    }

    public String getSimplePushEndpoint() {
        return simplePushEndpoint;
    }

    public void setSimplePushEndpoint(String endpoint) {
        this.simplePushEndpoint = endpoint;
    }

}
