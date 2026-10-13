package com.app.uctma.backend.dto;

import lombok.Builder;

@Builder
public record ChunkListResDTO(
        Long id,
        String chunkName
) {
}
