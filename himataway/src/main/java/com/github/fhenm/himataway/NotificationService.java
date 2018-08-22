package com.github.fhenm.himataway;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.model.NotificationEvent;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.model.Row;
import twitter4j.Status;
import twitter4j.UserMentionEntity;

public class NotificationService extends Service {

    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    public static boolean mStarted;

    public static void start() {
        if (mStarted) {
            return;
        }
        himatawayApplication application = himatawayApplication.getApplication();
        Intent intent = new Intent();
        intent.setClass(application, NotificationService.class);
        application.startService(intent);
        mStarted = true;
    }

    public static void stop() {
        if (!mStarted) {
            return;
        }
        himatawayApplication application = himatawayApplication.getApplication();
        Intent intent = new Intent();
        intent.setClass(application, NotificationService.class);
        application.stopService(intent);
        mStarted = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(NotificationEvent event) {
        SharedPreferences preferences = getSharedPreferences("settings", Context.MODE_PRIVATE);

        long userId = AccessTokenManager.getUserId();

        Row row = event.getRow();
        Status status = row.getStatus();
        Status retweet = status != null ? status.getRetweetedStatus() : null;

        String url;
        String title;
        String text;
        String ticker;
        int smallIcon;
        long id;
        if (row.isDirectMessage() && row.getMessage().getSender().getId() != userId) {
            if (!preferences.getBoolean("notification_message_on", true)) {
                return;
            }
            url = row.getMessage().getSender().getBiggerProfileImageURL();
            title = row.getMessage().getSender().getScreenName();
            text = row.getMessage().getText();
            ticker = text;
            smallIcon = R.drawable.ic_notification_mail;
            id = row.getMessage().getId();
        } else if (status != null && row.isFavorite()) {
            if (!preferences.getBoolean("notification_favorite_on", true)) {
                return;
            }
            url = row.getSource().getBiggerProfileImageURL();
            title = row.getSource().getScreenName();
            text = getString(R.string.notification_favorite) + status.getText();
            ticker = title + getString(R.string.notification_favorite_ticker) + status.getText();
            smallIcon = R.drawable.ic_notification_star;
            id = status.getId();
        } else if (status != null && status.getInReplyToUserId() == userId) {
            if (!preferences.getBoolean("notification_reply_on", true)) {
                return;
            }
            url = status.getUser().getBiggerProfileImageURL();
            title = status.getUser().getScreenName();
            text = status.getText();
            ticker = text;
            smallIcon = R.drawable.ic_notification_at;
            id = status.getId();
        } else if (retweet != null && retweet.getUser().getId() == userId) {
            if (!preferences.getBoolean("notification_retweet_on", true)) {
                return;
            }
            url = status.getUser().getBiggerProfileImageURL();
            title = status.getUser().getScreenName();
            text = getString(R.string.notification_retweet) + status.getText();
            ticker = title + getString(R.string.notification_retweet_ticker) + status.getText();
            smallIcon = R.drawable.ic_notification_rt;
            id = status.getId();
        } else {
            return;
        }

        sendNotification(url, title, text, ticker, smallIcon, id, status);
    }

    private void sendNotification(String url, String title, String text, String ticker, int smallIcon, long id, Status status) {
        long userId = AccessTokenManager.getUserId();
        SharedPreferences preferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        himatawayApplication application = himatawayApplication.getApplication();
        Resources resources = application.getResources();
//        int width = (int) resources.getDimension(android.R.dimen.notification_large_icon_width) / 3 * 2;
//        int height = (int) resources.getDimension(android.R.dimen.notification_large_icon_height) / 3 * 2;

//        Bitmap icon = ImageLoader.getInstance().loadImageSync(url);
//        icon = Bitmap.createScaledBitmap(icon, width, height, true);

        final String appName = getString(R.string.app_name);
        final String GROUP_KEY = "group";
        final int SUMMARY_NOTIFICATION_ID = 999;

        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Intent mainIntent = new Intent(this, MainActivity.class);
        final PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // サマリーは24以上でしか出さなくていい
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final Notification summaryNotification = new NotificationCompat.Builder(this, appName)
                    .setGroupSummary(true)
                    .setGroup(GROUP_KEY)
                    .setContentTitle(title)
                    .setContentText(title)
                    .setSmallIcon(R.drawable.ic_launcher)
//                    .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round))
//                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentIntent(mainPendingIntent)
                    .setAutoCancel(true) // 重要。各通知がすべて消えた時に、サマリーも自動で消える
                    .build();
            manager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification);
        }

        final NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .setSummaryText(appName)
                .bigText(text);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, appName)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setSmallIcon(smallIcon)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(mainPendingIntent)
                .setAutoCancel(true)
                .setStyle(style)
                .setGroup(GROUP_KEY)
//                .setColor(getColor(R.color.colorPri))
//                .setLargeIcon(icon)
//                .setGroup(getString(R.string.app_name))
//                .setGroupSummary(true)
                .setWhen(System.currentTimeMillis())
        ;

        boolean vibrate = preferences.getBoolean("notification_vibrate_on", true);
        boolean sound = preferences.getBoolean("notification_sound_on", true);
        if (vibrate && sound) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
        } else if (vibrate) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (sound) {
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }

        if (status != null && status.getInReplyToUserId() == userId) {
            Intent statusIntent = new Intent(this, StatusActivity.class);
            statusIntent.putExtra("status", status);
            statusIntent.putExtra("notification", true);

            NotificationCompat.Action statusAction = new NotificationCompat.Action(R.drawable.ic_notification_twitter,
                    getString(R.string.menu_open),
                    PendingIntent.getActivity(this, 1, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            builder.addAction(statusAction);

            Intent replyIntent = new Intent(this, PostActivity.class);
            replyIntent.putExtra("inReplyToStatus", status);
            replyIntent.putExtra("notification", true);
            UserMentionEntity[] mentions = status.getUserMentionEntities();
            if (status.getUser().getId() == userId && mentions.length == 1) {
                text = "@" + mentions[0].getScreenName() + " ";
            } else {
                text = "@" + status.getUser().getScreenName() + " ";
            }
            replyIntent.putExtra("status", text);
            replyIntent.putExtra("selection", text.length());
            replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                    .setLabel(getResources().getString(R.string.context_menu_reply))
                    .build();

            NotificationCompat.Action wearReplyAction = new NotificationCompat.Action.Builder(R.drawable.ic_notification_at,
                    getString(R.string.context_menu_reply),
                    PendingIntent.getActivity(this, 1, replyIntent, PendingIntent.FLAG_CANCEL_CURRENT))
                    .addRemoteInput(remoteInput)
                    .build();
            builder.addAction(wearReplyAction);

            Intent favoriteIntent = new Intent(this, FavoriteActivity.class);
            favoriteIntent.putExtra("statusId", status.getId());
            favoriteIntent.putExtra("notification", true);

            NotificationCompat.Action wearFavoriteAction = new NotificationCompat.Action.Builder(R.drawable.ic_notification_star,
                    getString(R.string.context_menu_create_favorite),
                    PendingIntent.getActivity(this, 1, favoriteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .build();
            builder.addAction(wearFavoriteAction);

            builder.extend(new NotificationCompat.WearableExtender().addAction(wearReplyAction).addAction(wearFavoriteAction).addAction(statusAction));
        }

        manager.notify(((int) (id > 0 ? id : 1)), builder.build());
    }
}
