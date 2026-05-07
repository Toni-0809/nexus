package com.nexus.usecase;

import com.nexus.model.Link;
import com.nexus.usecase.port.LinkRepository;

public class CreateLinkUseCase {
    private final LinkRepository linkRepository;

    public CreateLinkUseCase(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public Link execute(long sourceBlockId, long targetBlockId) {
        if (sourceBlockId <= 0 || targetBlockId <= 0) {
            throw new IllegalArgumentException("Blocks must be saved before creating a link.");
        }

        if (sourceBlockId == targetBlockId) {
            throw new IllegalArgumentException("A block cannot link to itself.");
        }

        if (linkRepository.exists(sourceBlockId, targetBlockId)) {
            return new Link(0, sourceBlockId, targetBlockId);
        }

        return linkRepository.save(new Link(0, sourceBlockId, targetBlockId));
    }
}