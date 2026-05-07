package com.nexus.usecase;

import com.nexus.model.Link;
import com.nexus.usecase.port.LinkRepository;

import java.util.List;

public class GetBacklinksForBlockUseCase {
    private final LinkRepository linkRepository;

    public GetBacklinksForBlockUseCase(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public List<Link> execute(long targetBlockId) {
        return linkRepository.findBacklinks(targetBlockId);
    }
}