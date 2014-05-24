package org.jboss.aerogear.android.impl.simplepush;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import static com.google.android.gms.cast.CastStatusCodes.TIMEOUT;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.impl.http.HttpRestProviderForPush;
import org.jboss.aerogear.android.impl.util.UrlUtils;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;

public class AeroGearSimplePushRegistrar implements PushRegistrar {

    private final SimplePushConfig config;
    private SimplePushService service;
    private static final String registryDeviceEndpoint = "/rest/registry/device";
    private static final String TAG = AeroGearSimplePushRegistrar.class.toString();
    private Provider<HttpRestProviderForPush> httpProviderProvider = new Provider<HttpRestProviderForPush>() {

        @Override
        public HttpRestProviderForPush get(Object... in) {
            return new HttpRestProviderForPush((URL) in[0], (Integer) in[1]);
        }
    };

    public AeroGearSimplePushRegistrar(SimplePushConfig config) {
        this.config = config;
    }

    @Override
    public void register(final Context context, final Callback<Void> callback) {
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
                                List<Pair<String, PushChannel>> categoryEndpoints = new ArrayList<Pair<String, PushChannel>>();
                                for (String category : config.getCategories()) {
                                    PushChannel channel;
                                    try {
                                        channel = service.register(category);
                                    } catch (final Exception ex) {
                                        Log.e(TAG, ex.getMessage(), ex);
                                        new Handler(callerLooper).post(new Runnable() {

                                            @Override
                                            public void run() {
                                                callback.onFailure(ex);
                                            }
                                        });
                                        return;
                                    }
                                    categoryEndpoints.add(Pair.create(category, channel));
                                }

                                for (Pair<String, PushChannel> categoryEndpoint : categoryEndpoints) {
                                    URL deviceRegistryURL;
                                    try {
                                        deviceRegistryURL = UrlUtils.appendToBaseURL(config.getPushServerURI().toURL(), registryDeviceEndpoint);
                                    } catch (final MalformedURLException ex) {
                                        Log.e(TAG, ex.getMessage(), ex);
                                        new Handler(callerLooper).post(new Runnable() {

                                            @Override
                                            public void run() {
                                                callback.onFailure(ex);
                                            }
                                        });
                                        return;
                                    }
                                    HttpRestProviderForPush httpProvider = httpProviderProvider.get(deviceRegistryURL, TIMEOUT);
                                    httpProvider.setPasswordAuthentication(config.getVariantID(), config.getSecret());

                                    Gson gson = new GsonBuilder().setExclusionStrategies(
                                            new ExclusionStrategy() {
                                                private final ImmutableSet<String> fields;

                                                {
                                                    fields = ImmutableSet.<String>builder()
                                                    .add("deviceToken")
                                                    .add("deviceType")
                                                    .add("alias")
                                                    .add("operatingSystem")
                                                    .add("osVersion")
                                                    .add("categories")
                                                    .build();
                                                }

                                                @Override
                                                public boolean shouldSkipField(FieldAttributes f) {
                                                    return !(f.getDeclaringClass() == PushConfig.class && fields
                                                    .contains(f.getName()));
                                                }

                                                @Override
                                                public boolean shouldSkipClass(Class<?> arg0) {
                                                    return false;
                                                }
                                            }).create();
                                    try {
                                        config.setDeviceToken(categoryEndpoint.second.getChannelId());
                                        config.setSimplePushEndpoint(categoryEndpoint.second.getEndpoint());
                                        config.setCategories(Lists.newArrayList(categoryEndpoint.first));
                                        httpProvider.post(gson.toJson(config));

                                    } catch (final HttpException ex) {
                                        Log.e(TAG, ex.getMessage(), ex);
                                        new Handler(callerLooper).post(new Runnable() {

                                            @Override
                                            public void run() {
                                                callback.onFailure(ex);
                                            }
                                        });
                                        return;
                                    }
                                }

                                new Handler(callerLooper).post(new Runnable() {

                                    @Override
                                    public void run() {
                                        callback.onSuccess(null);
                                    }
                                });

                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                service = null;
                            }
                        }, Context.BIND_AUTO_CREATE);
                    } else {
                        List<Pair<String, PushChannel>> categoryEndpoints = new ArrayList<Pair<String, PushChannel>>();
                        for (String category : config.getCategories()) {
                            PushChannel channel;
                            try {
                                channel = service.register(category);
                            } catch (final Exception ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                new Handler(callerLooper).post(new Runnable() {

                                    @Override
                                    public void run() {
                                        callback.onFailure(ex);
                                    }
                                });
                                return;
                            }
                            categoryEndpoints.add(Pair.create(category, channel));
                        }

                        for (Pair<String, PushChannel> categoryEndpoint : categoryEndpoints) {
                            URL deviceRegistryURL;
                            try {
                                deviceRegistryURL = UrlUtils.appendToBaseURL(config.getPushServerURI().toURL(), registryDeviceEndpoint);
                            } catch (final MalformedURLException ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                new Handler(callerLooper).post(new Runnable() {

                                    @Override
                                    public void run() {
                                        callback.onFailure(ex);
                                    }
                                });
                                return;
                            }
                            HttpRestProviderForPush httpProvider = httpProviderProvider.get(deviceRegistryURL, TIMEOUT);
                            httpProvider.setPasswordAuthentication(config.getVariantID(), config.getSecret());

                            Gson gson = new GsonBuilder().setExclusionStrategies(
                                    new ExclusionStrategy() {
                                        private final ImmutableSet<String> fields;

                                        {
                                            fields = ImmutableSet.<String>builder()
                                            .add("deviceToken")
                                            .add("deviceType")
                                            .add("alias")
                                            .add("operatingSystem")
                                            .add("osVersion")
                                            .add("categories")
                                            .build();
                                        }

                                        @Override
                                        public boolean shouldSkipField(FieldAttributes f) {
                                            return !(f.getDeclaringClass() == PushConfig.class && fields
                                            .contains(f.getName()));
                                        }

                                        @Override
                                        public boolean shouldSkipClass(Class<?> arg0) {
                                            return false;
                                        }
                                    }).create();
                            try {
                                config.setDeviceToken(categoryEndpoint.second.getChannelId());
                                config.setSimplePushEndpoint(categoryEndpoint.second.getEndpoint());
                                config.setCategories(Lists.newArrayList(categoryEndpoint.first));
                                httpProvider.post(gson.toJson(config));

                            } catch (final HttpException ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                new Handler(callerLooper).post(new Runnable() {

                                    @Override
                                    public void run() {
                                        callback.onFailure(ex);
                                    }
                                });
                                return;
                            }
                        }

                        new Handler(callerLooper).post(new Runnable() {

                            @Override
                            public void run() {
                                callback.onSuccess(null);
                            }
                        });

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
