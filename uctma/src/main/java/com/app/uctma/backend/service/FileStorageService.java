package com.app.uctma.backend.service;

import com.app.uctma.backend.model.FileChunk;
import com.app.uctma.backend.model.FileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    void init() ;
    String saveFile(MultipartFile file) ;
    String saveChunk(String chunkName, String content);
    String saveMdFile(FileMetadata fileMetadata,String mdData);
    void deleteFile(String filePath);
    Resource loadFileAsResource(String filePath);
    Resource loadChunksAsZipResource(Long fileId, List<FileChunk> chunks);
    String getContentFromFile(String filePath);

}
