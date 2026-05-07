package com.nexus.model.entity;

import java.util.Objects;

public class Page {
    private long id;
    private String title;

    public Page(long id, String title) {
        this.id = id;
        setTitle(title);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String safeTitle = Objects.requireNonNullElse(title, "Untitled").trim();

        if (safeTitle.isBlank()) {
            safeTitle = "Untitled";
        }

        this.title = safeTitle;
    }

    @Override
    public String toString() {
        return title;
    }
}