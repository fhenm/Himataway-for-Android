package com.github.fhenm.himataway.task;

import android.os.AsyncTask;

import com.github.fhenm.himataway.model.TwitterManager;

public class FollowTask extends AsyncTask<Long, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Long... params) {
        try {
            TwitterManager.getTwitter().createFriendship(params[0]);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}