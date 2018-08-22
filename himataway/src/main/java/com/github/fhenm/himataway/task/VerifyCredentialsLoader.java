package com.github.fhenm.himataway.task;

import android.content.Context;

import com.github.fhenm.himataway.model.TwitterManager;
import twitter4j.TwitterException;
import twitter4j.User;

public class VerifyCredentialsLoader extends AbstractAsyncTaskLoader<User> {

    public VerifyCredentialsLoader(Context context) {
        super(context);
    }

    @Override
    public User loadInBackground() {
        try {
            return TwitterManager.getTwitter().verifyCredentials();
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }
}