package com.github.fhenm.himataway.task;

import android.os.AsyncTask;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.event.model.StreamingDestroyStatusEvent;
import com.github.fhenm.himataway.model.FavRetweetManager;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.TwitterException;

public class UnRetweetTask extends AsyncTask<Void, Void, TwitterException> {

    private long mRetweetedStatusId;
    private long mStatusId;
    private static final int ERROR_CODE_DUPLICATE = 34;

    public UnRetweetTask(long retweetedStatusId, long statusId) {
        mRetweetedStatusId = retweetedStatusId;
        mStatusId = statusId;
        if (mRetweetedStatusId > 0) {
            FavRetweetManager.setRtId(mRetweetedStatusId, null);
            EventBus.getDefault().post(new StatusActionEvent());
        }
    }

    @Override
    protected TwitterException doInBackground(Void... params) {
        try {
            TwitterManager.getTwitter().destroyStatus(mStatusId);
            return null;
        } catch (TwitterException e) {
            e.printStackTrace();
            return e;
        }
    }

    @Override
    protected void onPostExecute(TwitterException e) {
        if (e == null) {
            MessageUtil.showToast(R.string.toast_destroy_retweet_success);
            EventBus.getDefault().post(new StreamingDestroyStatusEvent(mStatusId));
        } else if (e.getErrorCode() == ERROR_CODE_DUPLICATE) {
            MessageUtil.showToast(R.string.toast_destroy_retweet_already);
        } else {
            if (mRetweetedStatusId > 0) {
                FavRetweetManager.setRtId(mRetweetedStatusId, mStatusId);
                EventBus.getDefault().post(new StatusActionEvent());
            }
            MessageUtil.showToast(R.string.toast_destroy_retweet_failure);
        }
    }
}