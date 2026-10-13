package com.app.uctma.backend.dto;

import com.app.uctma.backend.model.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResDTO {
    private Long id;
    private String originalFileName;
    private FileStatus status;
    private Integer maxTokens;
    private LocalDateTime createdAt;
}
