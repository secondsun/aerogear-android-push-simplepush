package org.jboss.aerogear.android.impl.simplepush;

import java.net.URL;
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

    public void setSimplePushEndpoint(URL endpoint) {
        this.simplePushEndpoint = endpoint.toString();
    }

}
