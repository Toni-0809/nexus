package com.nexus.usecase.port;

import com.nexus.model.Link;

import java.util.List;

public interface LinkRepository {
    Link save(Link link);

    List<Link> findBacklinks(long targetBlockId);

    List<Link> findOutgoingLinks(long sourceBlockId);

    boolean exists(long sourceBlockId, long targetBlockId);
}