package com.github.fhenm.himataway.event.model;

import com.github.fhenm.himataway.model.Row;

public class StreamingCreateStatusEvent {
    private final Row row;

    public StreamingCreateStatusEvent(Row row) {
        this.row = row;
    }

    public Row getRow() {
        return row;
    }
}
