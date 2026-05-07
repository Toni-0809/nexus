package com.nexus.usecase;

import com.nexus.model.entity.Block;
import com.nexus.usecase.port.BlockRepository;

import java.util.List;

public class GetBlocksForPageUseCase {
    private final BlockRepository blockRepository;

    public GetBlocksForPageUseCase(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public List<Block> execute(long pageId) {
        return blockRepository.findByPageId(pageId);
    }
}