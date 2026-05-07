package com.nexus.adapter.ui.viewmodel;

import com.nexus.model.entity.Block;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BlockDto {
    private final LongProperty id = new SimpleLongProperty();
    private final LongProperty pageId = new SimpleLongProperty();
    private final ObjectProperty<Block.BlockType> type = new SimpleObjectProperty<>(Block.BlockType.PARAGRAPH);
    private final StringProperty content = new SimpleStringProperty("");
    private final IntegerProperty orderIndex = new SimpleIntegerProperty();

    public BlockDto(long id, long pageId, Block.BlockType type, String content, int orderIndex) {
        this.id.set(id);
        this.pageId.set(pageId);
        this.type.set(type);
        this.content.set(content);
        this.orderIndex.set(orderIndex);
    }

    public static BlockDto from(Block block) {
        return new BlockDto(
                block.getId(),
                block.getPageId(),
                block.getType(),
                block.getContent(),
                block.getOrderIndex()
        );
    }

    public Block toEntity() {
        return new Block(
                getId(),
                getPageId(),
                getType(),
                getContent(),
                getOrderIndex()
        );
    }

    public long getId() {
        return id.get();
    }

    public void setId(long value) {
        id.set(value);
    }

    public LongProperty idProperty() {
        return id;
    }

    public long getPageId() {
        return pageId.get();
    }

    public void setPageId(long value) {
        pageId.set(value);
    }

    public LongProperty pageIdProperty() {
        return pageId;
    }

    public Block.BlockType getType() {
        return type.get();
    }

    public void setType(Block.BlockType value) {
        type.set(value);
    }

    public ObjectProperty<Block.BlockType> typeProperty() {
        return type;
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String value) {
        content.set(value);
    }

    public StringProperty contentProperty() {
        return content;
    }

    public int getOrderIndex() {
        return orderIndex.get();
    }

    public void setOrderIndex(int value) {
        orderIndex.set(value);
    }

    public IntegerProperty orderIndexProperty() {
        return orderIndex;
    }
}