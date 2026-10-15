package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.ChunkListResDTO;
import com.app.uctma.backend.dto.ConvertFileReqDTO;
import com.app.uctma.backend.dto.FileProcessingCallbackReqDTO;
import com.app.uctma.backend.dto.FileUploadResDTO;
import com.app.uctma.backend.dto.UrlConvertReqDTO;
import com.app.uctma.backend.model.ConversionType;
import com.app.uctma.backend.model.FileChunk;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileUploadResDTO uploadFileToConvert(ConvertFileReqDTO convertFileReqDTO);
    FileUploadResDTO uploadUrlToConvert(UrlConvertReqDTO request);

    List<FileUploadResDTO> getUserFiles();
    List<ChunkListResDTO> getListChunksOfFile(Long fileId);

    void deleteFile(Long fileId);
    void deleteChunk(Long chunkId);
    void reconvertFile(Long fileId, ConversionType conversionType, Integer maxTokens);

    void handleFileProcessingCallback(FileProcessingCallbackReqDTO fileProcessingCallbackReqDTO);

    Resource downloadOriginalFile(Long fileId);
    Resource downloadMdFile(Long fileId);

    Resource downloadChunkFilesFromFileId(Long fileId);
    Resource downloadChunkFile(Long chunkId);
}
