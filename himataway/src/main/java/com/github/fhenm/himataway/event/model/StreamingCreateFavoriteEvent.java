package com.github.fhenm.himataway.event.model;

import com.github.fhenm.himataway.model.Row;

public class StreamingCreateFavoriteEvent {
    private final Row row;

    public StreamingCreateFavoriteEvent(Row row) {
        this.row = row;
    }

    public Row getRow() {
        return row;
    }
}
