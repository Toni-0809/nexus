package com.nexus.model;

public class Link {
    private long id;
    private long sourceBlockId;
    private long targetBlockId;

    public Link(long id, long sourceBlockId, long targetBlockId) {
        this.id = id;
        this.sourceBlockId = sourceBlockId;
        this.targetBlockId = targetBlockId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSourceBlockId() {
        return sourceBlockId;
    }

    public long getTargetBlockId() {
        return targetBlockId;
    }
}
