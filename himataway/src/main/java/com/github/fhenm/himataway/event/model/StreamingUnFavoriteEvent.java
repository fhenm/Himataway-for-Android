package com.github.fhenm.himataway.event.model;

import twitter4j.Status;
import twitter4j.User;

public class StreamingUnFavoriteEvent {

    private final User mUser;
    private final Status mStatus;

    public StreamingUnFavoriteEvent(final User user, final Status status) {
        mUser = user;
        mStatus = status;
    }

    public User getUser() {
        return mUser;
    }

    public Status getStatus() {
        return mStatus;
    }
}
