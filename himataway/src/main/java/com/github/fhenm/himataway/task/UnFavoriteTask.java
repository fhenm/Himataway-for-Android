package com.github.fhenm.himataway.task;

import android.os.AsyncTask;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.action.StatusActionEvent;
import com.github.fhenm.himataway.model.FavRetweetManager;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.TwitterException;

public class UnFavoriteTask extends AsyncTask<Void, Void, TwitterException> {

    private static final int ERROR_CODE_DUPLICATE = 34;

    private long mStatusId;

    public UnFavoriteTask(long statusId) {
        mStatusId = statusId;

        /**
         * 先にremoveFavしておかないとViewの星が戻ってしまう、
         * 重複エラー以外の理由で失敗し場合（通信エラー等）は戻す
         */
        FavRetweetManager.removeFav(mStatusId);
        EventBus.getDefault().post(new StatusActionEvent());
    }

    @Override
    protected TwitterException doInBackground(Void... params) {
        try {
            TwitterManager.getTwitter().destroyFavorite(mStatusId);
        } catch (TwitterException e) {
            return e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(TwitterException e) {
        if (e == null) {
            MessageUtil.showToast(R.string.toast_destroy_favorite_success);
        } else if (e.getErrorCode() == ERROR_CODE_DUPLICATE) {
            MessageUtil.showToast(R.string.toast_destroy_favorite_already);
        } else {
            FavRetweetManager.setFav(mStatusId);
            EventBus.getDefault().post(new StatusActionEvent());
            MessageUtil.showToast(R.string.toast_destroy_favorite_failure);
        }
    }
}
