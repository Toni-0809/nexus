package com.nexus.usecase;

import com.nexus.model.Link;
import com.nexus.usecase.port.LinkRepository;

import java.util.List;

public class GetOutgoingLinksForBlockUseCase {
    private final LinkRepository linkRepository;

    public GetOutgoingLinksForBlockUseCase(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public List<Link> execute(long sourceBlockId) {
        return linkRepository.findOutgoingLinks(sourceBlockId);
    }
}