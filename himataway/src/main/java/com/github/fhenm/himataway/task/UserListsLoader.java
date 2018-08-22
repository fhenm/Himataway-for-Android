package com.github.fhenm.himataway.task;

import android.content.Context;

import com.github.fhenm.himataway.model.AccessTokenManager;
import com.github.fhenm.himataway.model.TwitterManager;
import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.UserList;

public class UserListsLoader extends AbstractAsyncTaskLoader<ResponseList<UserList>> {

    public UserListsLoader(Context context) {
        super(context);
    }

    @Override
    public ResponseList<UserList> loadInBackground() {
        try {
            return TwitterManager.getTwitter().getUserLists(AccessTokenManager.getUserId());
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
