package com.nexus.usecase.port;

import com.nexus.model.entity.Block;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    Block save(Block block);

    List<Block> findByPageId(long pageId);

    Optional<Block> findById(long blockId);

    void deleteById(long blockId);
}