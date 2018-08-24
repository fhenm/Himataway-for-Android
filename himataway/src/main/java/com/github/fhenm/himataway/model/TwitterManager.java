package com.github.fhenm.himataway.model;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.github.fhenm.himataway.MainActivity;
import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.BuildConfig;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.adapter.MyUserStreamAdapter;
import com.github.fhenm.himataway.event.action.AccountChangeEvent;
import com.github.fhenm.himataway.event.connection.StreamingConnectionEvent;
import com.github.fhenm.himataway.settings.BasicSettings;
import com.github.fhenm.himataway.util.MessageUtil;

import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Twitterインスタンス管理
 */
public class TwitterManager {
    private static final MyUserStreamAdapter sUserStreamAdapter = new MyUserStreamAdapter();
    private static TwitterStream sTwitterStream;
    private static boolean sTwitterStreamConnected;

    @NonNull
    public static MyUserStreamAdapter getUserStreamAdapter() {
        return sUserStreamAdapter;
    }

    public static void switchAccessToken(final AccessToken accessToken) {
        AccessTokenManager.setAccessToken(accessToken);
        if (BasicSettings.getStreamingMode()) {
            MessageUtil.showToast(R.string.toast_destroy_streaming);
            sUserStreamAdapter.stop();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    sTwitterStream.cleanUp();
                    sTwitterStream.shutdown();
                    return null;
                }

                @Override
                protected void onPostExecute(Void status) {
                    sTwitterStream.setOAuthAccessToken(accessToken);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MessageUtil.showToast(R.string.toast_create_streaming);
                            sUserStreamAdapter.start();
                            new AsyncTask<Void, Void, IDs>() {
                                @Override
                                protected IDs doInBackground(Void... voids) {
                                    try {
                                        return getTwitter().getFriendsIDs(-1L);
                                    } catch (TwitterException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }

                                @Override
                                protected void onPostExecute(IDs iDs) {
                                    FilterQuery filterquery = new FilterQuery(iDs.getIDs());
                                    sTwitterStream.filter(filterquery);
                                }
                            }.execute();
                        }
                    }, 5000);
                }
            }.execute();
        }
        EventBus.getDefault().post(new AccountChangeEvent());
    }

    private static String getConsumerKey() {
        return BuildConfig.TwitterConsumerKey;
    }

    private static String getConsumerSecret() {
        return BuildConfig.TwitterConsumerSecret;
    }

    public static Twitter getTwitter() {
        Twitter twitter = getTwitterInstance();
        AccessToken token = AccessTokenManager.getAccessToken();
        twitter.setOAuthAccessToken(token);
        return twitter;
    }

    public static Twitter getTwitterInstance() {

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        twitter4j.conf.Configuration conf = configurationBuilder
                .setOAuthConsumerKey(getConsumerKey())
                .setOAuthConsumerSecret(getConsumerSecret())
                .setTweetModeExtended(true)
                .build();

        TwitterFactory factory = new TwitterFactory(conf);
        Twitter twitter = factory.getInstance();

        return twitter;
    }

    public static TwitterStream getTwitterStream() {
        AccessToken token = AccessTokenManager.getAccessToken();
        if (token == null) {
            return null;
        }
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        twitter4j.conf.Configuration conf = configurationBuilder
                .setOAuthConsumerKey(getConsumerKey())
                .setOAuthConsumerSecret(getConsumerSecret())
                .setOAuthAccessToken(token.getToken())
                .setOAuthAccessTokenSecret(token.getTokenSecret())
                .setTweetModeExtended(true)
                .build();
        return new TwitterStreamFactory(conf).getInstance();
    }

    public static boolean getTwitterStreamConnected() {
        return sTwitterStreamConnected;
    }

    public static void startStreaming() {
        if (sTwitterStream != null) {
            if (!sTwitterStreamConnected) {
                sUserStreamAdapter.start();
                sTwitterStream.setOAuthAccessToken(AccessTokenManager.getAccessToken());
                new AsyncTask<Void, Void, IDs>() {
                    @Override
                    protected IDs doInBackground(Void... voids) {
                        try {
                            return getTwitter().getFriendsIDs(-1L);
                        } catch (TwitterException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(IDs iDs) {
                        FilterQuery filterquery = new FilterQuery(iDs.getIDs());
                        sTwitterStream.filter(filterquery);
                    }
                }.execute();
            }
            return;
        }
        sTwitterStream = getTwitterStream();
//        sUserStreamAdapter = new MyUserStreamAdapter();
        sTwitterStream.addListener(sUserStreamAdapter);
        sTwitterStream.addConnectionLifeCycleListener(new MyConnectionLifeCycleListener());
        new AsyncTask<Void, Void, IDs>() {
            @Override
            protected IDs doInBackground(Void... voids) {
                try {
                    return getTwitter().getFriendsIDs(-1L);
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(IDs iDs) {
                FilterQuery filterquery = new FilterQuery(iDs.getIDs());
                sTwitterStream.filter(filterquery);
            }
        }.execute();
        BasicSettings.resetNotification();
    }

    public static void stopStreaming() {
        if (sTwitterStream == null) {
            return;
        }
        BasicSettings.setStreamingMode(false);
        sUserStreamAdapter.stop();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                sTwitterStream.cleanUp();
                sTwitterStream.shutdown();
                return null;
            }

            @Override
            protected void onPostExecute(Void status) {

            }
        }.execute();
    }

    public static void pauseStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter.pause();
        }
    }

    public static void resumeStreaming() {
        if (sUserStreamAdapter != null) {
            sUserStreamAdapter.resume();
        }
    }

    public static class MyConnectionLifeCycleListener implements ConnectionLifeCycleListener {
        @Override
        public void onConnect() {
            sTwitterStreamConnected = true;
            EventBus.getDefault().post(StreamingConnectionEvent.onConnect());
        }

        @Override
        public void onDisconnect() {
            sTwitterStreamConnected = false;
            EventBus.getDefault().post(StreamingConnectionEvent.onDisconnect());
        }

        @Override
        public void onCleanUp() {
            sTwitterStreamConnected = false;
            EventBus.getDefault().post(StreamingConnectionEvent.onCleanUp());
        }
    }
}
