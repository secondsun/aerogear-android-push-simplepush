package org.jboss.aerogear.android.impl.simplepush;

import java.net.MalformedURLException;
import java.net.URL;

public class PushChannel {

    private final URL endpoint;
    private final String channelId;

    public PushChannel(String endpoint, String channelId) {
        try {
            this.endpoint = new URL(endpoint);
            this.channelId = channelId;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public URL getEndpoint() {
        return endpoint;
    }

    public String getChannelId() {
        return channelId;
    }
    
    
    
}
