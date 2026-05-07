package com.nexus.usecase;

import com.nexus.model.entity.Page;
import com.nexus.usecase.port.PageRepository;

public class CreatePageUseCase {
    private final PageRepository pageRepository;

    public CreatePageUseCase(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public Page execute(String title) {
        return pageRepository.save(new Page(0, title));
    }
}