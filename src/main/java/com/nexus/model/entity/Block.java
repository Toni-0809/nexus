package com.nexus.model.entity;

import java.util.Objects;

public class Block {
    private long id;
    private long pageId;
    private BlockType type;
    private String content;
    private int orderIndex;

    public Block(long id, long pageId, BlockType type, String content, int orderIndex) {
        this.id = id;
        this.pageId = pageId;
        this.type = Objects.requireNonNullElse(type, BlockType.PARAGRAPH);
        this.content = Objects.requireNonNullElse(content, "");
        this.orderIndex = orderIndex;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public BlockType getType() {
        return type;
    }

    public void setType(BlockType type) {
        this.type = Objects.requireNonNullElse(type, BlockType.PARAGRAPH);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = Objects.requireNonNullElse(content, "");
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public enum BlockType {
        PARAGRAPH,
        HEADING,
        TODO
    }
}