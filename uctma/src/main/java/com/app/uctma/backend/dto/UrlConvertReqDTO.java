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
public class UrlConvertReqDTO {
    private String url;
    private ConversionType conversionType = ConversionType.CONVERT;
    private Integer maxTokens = 400;
}
