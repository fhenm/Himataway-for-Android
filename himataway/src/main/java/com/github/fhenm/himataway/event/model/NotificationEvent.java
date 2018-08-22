package com.github.fhenm.himataway.event.model;

import com.github.fhenm.himataway.model.Row;

public class NotificationEvent {

    private final Row mRow;

    public NotificationEvent(final Row row) {
        mRow = row;
    }

    public Row getRow() {
        return mRow;
    }
}
