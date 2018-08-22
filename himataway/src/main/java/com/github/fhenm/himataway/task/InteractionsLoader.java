package com.github.fhenm.himataway.task;

import android.content.Context;

import com.github.fhenm.himataway.model.TwitterManager;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

public class InteractionsLoader extends AbstractAsyncTaskLoader<ResponseList<Status>> {

    public InteractionsLoader(Context context) {
        super(context);
    }

    @Override
    public ResponseList<Status> loadInBackground() {
        try {
            return TwitterManager.getTwitter().getMentionsTimeline();
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
