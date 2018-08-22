package com.github.fhenm.himataway.task;

import android.os.AsyncTask;

import de.greenrobot.event.EventBus;
import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.event.model.DestroyUserListEvent;
import com.github.fhenm.himataway.model.TwitterManager;
import com.github.fhenm.himataway.model.UserListCache;
import com.github.fhenm.himataway.util.MessageUtil;
import twitter4j.UserList;

public class DestroyUserListSubscriptionTask extends AsyncTask<Void, Void, Boolean> {

    UserList mUserList;

    public DestroyUserListSubscriptionTask(UserList userList) {
        mUserList = userList;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            TwitterManager.getTwitter().destroyUserListSubscription(mUserList.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_success);
            EventBus.getDefault().post(new DestroyUserListEvent(mUserList.getId()));
            UserListCache.getUserLists().remove(mUserList);
        } else {
            MessageUtil.showToast(R.string.toast_destroy_user_list_subscription_failure);
        }
    }
}
