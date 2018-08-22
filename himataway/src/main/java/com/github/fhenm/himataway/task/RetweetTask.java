package com.github.fhenm.himataway.task;

import android.os.AsyncTask;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.model.FavRetweetManager;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.TwitterException;

public class RetweetTask extends AsyncTask<Void, Void, TwitterException> {

    private long mStatusId;
    private static final int ERROR_CODE_DUPLICATE = 37;

    public RetweetTask(long statusId) {
        mStatusId = statusId;
        FavRetweetManager.setRtId(statusId, (long) 0);
        EventBus.getDefault().post(new StatusActionEvent());
    }

    @Override
    protected TwitterException doInBackground(Void... params) {
        try {
            twitter4j.Status status = TwitterManager.getTwitter().retweetStatus(mStatusId);
            FavRetweetManager.setRtId(status.getRetweetedStatus().getId(), status.getId());
            return null;
        } catch (TwitterException e) {
            FavRetweetManager.setRtId(mStatusId, null);
            e.printStackTrace();
            return e;
        }
    }

    @Override
    protected void onPostExecute(TwitterException e) {
        if (e == null) {
            MessageUtil.showToast(R.string.toast_retweet_success);
        } else if (e.getErrorCode() == ERROR_CODE_DUPLICATE) {
            MessageUtil.showToast(R.string.toast_retweet_already);
        } else {
            EventBus.getDefault().post(new StatusActionEvent());
            MessageUtil.showToast(R.string.toast_retweet_failure);
        }
    }
}