package com.app.uctma.backend.dto;

import com.app.uctma.backend.model.ConversionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMessageDTO {
    private Long fileId;
    private String filePath;
    private String originalFileName;
    private ConversionType conversionType;
    private Integer maxTokens;
}
