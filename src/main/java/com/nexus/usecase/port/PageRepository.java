package com.nexus.usecase.port;

import com.nexus.model.entity.Page;

import java.util.List;
import java.util.Optional;

public interface PageRepository {
    Page save(Page page);

    List<Page> findAll();

    Optional<Page> findById(long id);

    void deleteById(long id);
}