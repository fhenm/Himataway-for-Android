package com.github.fhenm.himataway.model;

import twitter4j.DirectMessage;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.TwitterResponse;
import twitter4j.User;

public class Row implements TwitterResponse {

    private final static int TYPE_STATUS = 0;
    private final static int TYPE_FAVORITE = 1;
    private final static int TYPE_DM = 2;

    private Status status;
    private DirectMessage message;
    private User source;
    private User target;
    private int type;

    public Row() {
        super();
    }

    public static Row newStatus(Status status) {
        Row row = new Row();
        row.setStatus(status);
        row.setType(TYPE_STATUS);
        return row;
    }

    public static Row newFavorite(User source, User target, Status status) {
        Row row = new Row();
        row.setStatus(status);
        row.setTarget(target);
        row.setSource(source);
        row.setType(TYPE_FAVORITE);
        return row;
    }

    public static Row newDirectMessage(DirectMessage message) {
        Row row = new Row();
        row.setMessage(message);
        row.setType(TYPE_DM);
        return row;
    }

    public boolean isStatus() {
        return type == TYPE_STATUS;
    }

    public boolean isFavorite() {
        return type == TYPE_FAVORITE;
    }

    public boolean isDirectMessage() {
        return type == TYPE_DM;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DirectMessage getMessage() {
        return message;
    }

    public void setMessage(DirectMessage message) {
        this.message = message;
    }

    public User getSource() {
        return source;
    }

    public void setSource(User source) {
        this.source = source;
    }

    public User getTarget() {
        return target;
    }

    public void setTarget(User target) {
        this.target = target;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public RateLimitStatus getRateLimitStatus() {
        return null;
    }

    @Override
    public int getAccessLevel() {
        return READ;
    }
}
