package com.github.fhenm.himataway.event.model;

public class StreamingDestroyStatusEvent {

    private final Long mStatusId;

    public StreamingDestroyStatusEvent(final Long statusId) {
        mStatusId = statusId;
    }

    public Long getStatusId() {
        return mStatusId;
    }
}
