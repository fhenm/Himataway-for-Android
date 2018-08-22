package com.github.fhenm.himataway.util;

import android.content.Context;
import android.content.Intent;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.MainActivity;
import com.github.fhenm.himataway.PostActivity;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.action.OpenEditorEvent;
import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.model.FavRetweetManager;
import com.github.fhenm.himataway.task.DestroyDirectMessageTask;
import com.github.fhenm.himataway.task.DestroyStatusTask;
import com.github.fhenm.himataway.task.FavoriteTask;
import com.github.fhenm.himataway.task.RetweetTask;
import com.github.fhenm.himataway.task.UnFavoriteTask;
import com.github.fhenm.himataway.task.UnRetweetTask;
import twitter4j.DirectMessage;
import twitter4j.UserMentionEntity;

public class ActionUtil {
    public static void doFavorite(Long statusId) {
        new FavoriteTask(statusId).execute();
    }

    public static void doDestroyFavorite(Long statusId) {
        new UnFavoriteTask(statusId).execute();
    }

    public static void doDestroyStatus(Long statusId) {
        new DestroyStatusTask(statusId).execute();
    }

    public static void doRetweet(Long statusId) {
        new RetweetTask(statusId).execute();
    }

    public static void doDestroyRetweet(twitter4j.Status status) {
        twitter4j.Status retweet = status.getRetweetedStatus();
        if (status.getUser().getId() == AccessTokenManager.getUserId() && retweet != null) {
            // 自分がRTしたStatus
            new UnRetweetTask(retweet.getId(), status.getId()).execute();
        } else {
            // 他人のStatusで、それを自分がRTしている

            // 被リツイート
            Long retweetedStatusId = -1L;

            // リツイート
            Long statusId = FavRetweetManager.getRtId(status.getId());
            if (statusId != null && statusId > 0) {
                // そのStatusそのものをRTしている
                retweetedStatusId = status.getId();
            } else {
                if (retweet != null) {
                    statusId = FavRetweetManager.getRtId(retweet.getId());
                    if (statusId != null && statusId > 0) {
                        // そのStatusがRTした元StatusをRTしている
                        retweetedStatusId = retweet.getId();
                    }
                }
            }

            if (statusId != null && statusId == 0L) {
                // 処理中は 0
                MessageUtil.showToast(R.string.toast_destroy_retweet_progress);
            } else if (statusId != null && statusId > 0 && retweetedStatusId > 0) {
                new UnRetweetTask(retweetedStatusId, statusId).execute();
            }
        }
    }

    public static void doReply(twitter4j.Status status, Context context) {
        Long userId = AccessTokenManager.getUserId();
        UserMentionEntity[] mentions = status.getUserMentionEntities();
        String text;
        if (status.getUser().getId() == userId && mentions.length == 1) {
            text = "@" + mentions[0].getScreenName() + " ";
        } else {
            text = "@" + status.getUser().getScreenName() + " ";
        }
        if (context instanceof MainActivity) {
            EventBus.getDefault().post(new OpenEditorEvent(text, status, text.length(), null));
        } else {
            Intent intent = new Intent(context, PostActivity.class);
            intent.putExtra("status", text);
            intent.putExtra("selection", text.length());
            intent.putExtra("inReplyToStatus", status);
            context.startActivity(intent);
        }
    }

    public static void doReplyAll(twitter4j.Status status, Context context) {
        long userId = AccessTokenManager.getUserId();
        UserMentionEntity[] mentions = status.getUserMentionEntities();
        String text = "";
        int selection_start = 0;
        if (status.getUser().getId() != userId) {
            text = "@" + status.getUser().getScreenName() + " ";
            selection_start = text.length();
        }
        for (UserMentionEntity mention : mentions) {
            if (status.getUser().getId() == mention.getId()) {
                continue;
            }
            if (userId == mention.getId()) {
                continue;
            }
            text = text.concat("@" + mention.getScreenName() + " ");
            if (selection_start == 0) {
                selection_start = text.length();
            }
        }
        if (context instanceof MainActivity) {
            EventBus.getDefault().post(new OpenEditorEvent(text, status, selection_start, text.length()));
        } else {
            Intent intent = new Intent(context, PostActivity.class);
            intent.putExtra("status", text);
            intent.putExtra("selection", selection_start);
            intent.putExtra("selection_stop", text.length());
            intent.putExtra("inReplyToStatus", status);
            context.startActivity(intent);
        }
    }

    public static void doReplyDirectMessage(DirectMessage directMessage, Context context) {
        String text;
        if (AccessTokenManager.getUserId() == directMessage.getSender().getId()) {
            text = "D " + directMessage.getRecipient().getScreenName() + " ";
        } else {
            text = "D " + directMessage.getSender().getScreenName() + " ";
        }
        if (context instanceof MainActivity) {
            EventBus.getDefault().post(new OpenEditorEvent(text, null, text.length(), null));
        } else {
            Intent intent = new Intent(context, PostActivity.class);
            intent.putExtra("status", text);
            intent.putExtra("selection", text.length());
            context.startActivity(intent);
        }
    }

    public static void doDestroyDirectMessage(long id) {
        new DestroyDirectMessageTask().execute(id);
    }

    public static void doQuote(twitter4j.Status status, Context context) {
        String text = " https://twitter.com/"
                + status.getUser().getScreenName()
                + "/status/" + String.valueOf(status.getId());
        if (context instanceof MainActivity) {
            EventBus.getDefault().post(new OpenEditorEvent(text, status, null, null));
        } else {
            Intent intent = new Intent(context, PostActivity.class);
            intent.putExtra("status", text);
            intent.putExtra("inReplyToStatus", status);
            context.startActivity(intent);
        }
    }
}
