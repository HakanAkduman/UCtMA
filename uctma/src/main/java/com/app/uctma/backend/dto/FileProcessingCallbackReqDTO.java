package com.app.uctma.backend.dto;

import com.app.uctma.backend.model.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingCallbackReqDTO {
    private Long fileId;
    private FileStatus status;
    private String convertedText;
    private List<ChunkDTO> chunks;
    private String errorMessage;
}
