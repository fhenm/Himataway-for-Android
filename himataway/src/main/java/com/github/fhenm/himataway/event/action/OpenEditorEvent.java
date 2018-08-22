package com.github.fhenm.himataway.event.action;

import twitter4j.Status;

public class OpenEditorEvent {

    private final String mText;
    private final Status mInReplyToStatus;
    private final Integer mSelectionStart;
    private final Integer mSelectionStop;

    public OpenEditorEvent(final String text, final Status inReplyToStatus, final Integer selectionStart, final Integer selectionStop) {
        mText = text;
        mInReplyToStatus = inReplyToStatus;
        mSelectionStart = selectionStart;
        mSelectionStop = selectionStop;
    }

    public String getText() {
        return mText;
    }

    public Status getInReplyToStatus() {
        return mInReplyToStatus;
    }

    public Integer getSelectionStart() {
        return mSelectionStart;
    }

    public Integer getSelectionStop() {
        return mSelectionStop;
    }
}
