package com.app.uctma.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDTO {
    private Integer chunkIndex;
    private String text;
    private List<Integer> pageNumbers;
}
