package com.app.uctma.backend.dto;


import com.app.uctma.backend.model.ConversionType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ConvertFileReqDTO {

    @NotEmpty(message = "Dosya yüklemeden döünştürme işlemi yapamazsınız")
    private MultipartFile file ;

    private ConversionType conversionType = ConversionType.CONVERT ;

    private Integer maxTokens=400;
}
