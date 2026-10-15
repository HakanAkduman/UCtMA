package com.app.uctma.backend.service.impl;


import com.app.uctma.backend.Utils.CheckAccess;
import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.FileChunk;
import com.app.uctma.backend.model.FileMetadata;
import com.app.uctma.backend.model.FileStatus;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.model.ConversionType;
import com.app.uctma.backend.repository.FileChunkRepository;
import com.app.uctma.backend.repository.FileMetadataRepository;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.FileService;
import com.app.uctma.backend.service.FileStorageService;
import com.app.uctma.backend.service.RabbitFileMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> SUPPORTED_FILE_EXTENSIONS = Set.of(
        ".csv", ".txt", ".md",
        ".wav", ".mp3", ".m4a", ".aac", ".ogg", ".flac",
        ".mp4", ".avi", ".mov", ".mkv", ".webm",
        ".pdf", ".docx", ".pptx", ".html", ".xlsx",
        ".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp", ".webp"
    );

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService;
    private final FileChunkRepository fileChunkRepository;
    private final RabbitFileMessageService rabbitFileMessageService;
    private final CheckAccess checkAccessObj;

    // this function is to upload file to the server and create metadata object for it. anr then it leaves a message to RabbitMQ.
    // then in the fastapi side a consumer will be subscribed for these.
    @Override
    @Transactional
    public FileUploadResDTO uploadFileToConvert(ConvertFileReqDTO convertFileReqDTO) {


        User user= checkAccessObj.checkUserValid();
        validateFileExtension(convertFileReqDTO.getFile().getOriginalFilename());


        String savedFilePath=fileStorageService.saveFile(convertFileReqDTO.getFile());


        FileMetadata fileMetadata = FileMetadata.builder()
                .originalFileName(convertFileReqDTO.getFile().getOriginalFilename())
                .filePath(savedFilePath)
                .fileType(convertFileReqDTO.getFile().getContentType())
                .status(FileStatus.PENDING)
                .maxTokens(convertFileReqDTO.getMaxTokens())
                .user(user)
                .build();

        FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);

        FileMessageDTO fileMessageDTO= FileMessageDTO.builder()
                .fileId(savedMetadata.getId())
                .filePath(savedMetadata.getFilePath())
                .originalFileName(savedMetadata.getOriginalFileName())
                .conversionType(convertFileReqDTO.getConversionType())
                .maxTokens(convertFileReqDTO.getMaxTokens())
                .build();

        // we leave message to rabbit mq
        rabbitFileMessageService.sendFileProcessingMessage(fileMessageDTO);


        return FileUploadResDTO.builder().id(savedMetadata.getId())
                .originalFileName(savedMetadata.getOriginalFileName())
                .status(savedMetadata.getStatus())
                .maxTokens(savedMetadata.getMaxTokens())
                .createdAt(savedMetadata.getCreatedAt())
                .build();
    }

    private void validateFileExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Dosya adı boş olamaz.");
        }

        int extensionStart = originalFileName.lastIndexOf('.');
        String extension = extensionStart >= 0
                ? originalFileName.substring(extensionStart).toLowerCase(Locale.ROOT)
                : "";

        if (!SUPPORTED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Desteklenmeyen dosya formatı: "
                            + (extension.isBlank() ? "uzantısız" : extension)
                            + ". Desteklenen formatlar: "
                            + SUPPORTED_FILE_EXTENSIONS
            );
        }
    }

    @Override
    public List<FileUploadResDTO> getUserFiles() {

        User user= checkAccessObj.checkUserValid();


        List<FileMetadata> list= fileMetadataRepository.findByUserId(user.getId());

        return list.stream().map((fileMetadata -> {
                    return FileUploadResDTO.builder().id(fileMetadata.getId())
                           .originalFileName(fileMetadata.getOriginalFileName())
                           .status(fileMetadata.getStatus())
                           .maxTokens(fileMetadata.getMaxTokens())
                           .createdAt(fileMetadata.getCreatedAt()).build();
                })).toList();
    }

    @Override
    public List<ChunkListResDTO> getListChunksOfFile(Long fileId) {
        FileMetadata fileMetadata= checkAccessObj.userHasAccessToFile(fileId);
        return fileChunkRepository.findByFileMetadataIdOrderByChunkIndexAsc(fileMetadata.getId()).stream().map(
                (fileChunk -> {
                    return new ChunkListResDTO(fileChunk.getId(),fileChunk.getChunkName());
                })
        ).toList();
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        FileMetadata fileMetadata = checkAccessObj.userHasAccessToFile(fileId);
        deleteDerivedFiles(fileMetadata);
        fileStorageService.deleteFile(fileMetadata.getFilePath());
        fileMetadataRepository.delete(fileMetadata);
    }

    @Override
    @Transactional
    public void deleteChunk(Long chunkId) {
        FileChunk fileChunk = checkAccessObj.userHasAccessToChunk(chunkId);
        fileStorageService.deleteFile(fileChunk.getFilePath());
        fileChunkRepository.delete(fileChunk);
    }

    @Override
    @Transactional
    public void reconvertFile(Long fileId, ConversionType conversionType, Integer maxTokens) {
        FileMetadata fileMetadata = checkAccessObj.userHasAccessToFile(fileId);
        validateFileExtension(fileMetadata.getOriginalFileName());
        deleteDerivedFiles(fileMetadata);

        fileMetadata.setStatus(FileStatus.PENDING);
        if (maxTokens != null) {
            fileMetadata.setMaxTokens(maxTokens);
        }
        fileMetadataRepository.save(fileMetadata);

        rabbitFileMessageService.sendFileProcessingMessage(FileMessageDTO.builder()
                .fileId(fileMetadata.getId())
                .filePath(fileMetadata.getFilePath())
                .originalFileName(fileMetadata.getOriginalFileName())
                .conversionType(conversionType == null ? ConversionType.CONVERT : conversionType)
                .maxTokens(fileMetadata.getMaxTokens())
                .build());
    }

    private void deleteDerivedFiles(FileMetadata fileMetadata) {
        List<FileChunk> chunks = fileChunkRepository
                .findByFileMetadataIdOrderByChunkIndexAsc(fileMetadata.getId());

        for (FileChunk chunk : chunks) {
            fileStorageService.deleteFile(chunk.getFilePath());
        }
        fileChunkRepository.deleteAll(chunks);

        fileStorageService.deleteFile(fileMetadata.getMdFilePath());
        fileMetadata.setMdFilePath("");
    }

    @Override
    @Transactional
    public void handleFileProcessingCallback(FileProcessingCallbackReqDTO callbackDTO) {
        System.out.println("Python dan callback alındı. File ID: " + callbackDTO.getFileId() +
                ", Status: " + callbackDTO.getStatus()+ "tüm callback: "+ callbackDTO.toString());

        FileMetadata fileMetadata = fileMetadataRepository.findById(callbackDTO.getFileId())
                .orElseThrow(() -> new RuntimeException("Callback için dosya bulunamadı! ID: " + callbackDTO.getFileId()));

        fileMetadata.setStatus(callbackDTO.getStatus());

        if (callbackDTO.getConvertedText() != null
                && !callbackDTO.getConvertedText().isBlank()) {
            String mdPath=fileStorageService.saveMdFile(fileMetadata, callbackDTO.getConvertedText());
            fileMetadata.setMdFilePath(mdPath);
        }

        fileMetadataRepository.save(fileMetadata);

        if (callbackDTO.getStatus() == FileStatus.FAILED) {
            System.out.println(
                    "Dosya dönüştürme sırasında hata oldu. File ID: "
                            + fileMetadata.getId()
                            + ", hata: "
                            + callbackDTO.getErrorMessage()
            );
            return;
        }

        if (callbackDTO.getChunks() != null && !callbackDTO.getChunks().isEmpty()) {
            List<FileChunk> chunkEntities = new ArrayList<>();

            for (ChunkDTO chunkDTO : callbackDTO.getChunks()) {

                FileChunk chunk = FileChunk.builder()
                        .chunkIndex(chunkDTO.getChunkIndex())
                        .chunkName(fileMetadata.getId()+"_"+fileMetadata.getOriginalFileName()+"_"+chunkDTO.getChunkIndex()+".txt")
                        .fileMetadata(fileMetadata)
                        .build();


                String savedFilePath = fileStorageService.saveChunk(
                        chunk.getChunkName(),
                        chunkDTO.getText()
                );

                chunk.setFilePath(savedFilePath);
                chunkEntities.add(chunk);
            }

            fileChunkRepository.saveAll(chunkEntities);
        }
    }

    @Override
    public Resource downloadOriginalFile(Long fileId) {
        FileMetadata fileMetadata= checkAccessObj.userHasAccessToFile(fileId);
        return fileStorageService.loadFileAsResource(fileMetadata.getFilePath());
    }

    @Override
    public Resource downloadMdFile(Long fileId) {
        FileMetadata fileMetadata= checkAccessObj.userHasAccessToFile(fileId);
        return fileMetadata.getMdFilePath().isBlank()? null : fileStorageService.loadFileAsResource(fileMetadata.getMdFilePath());
    }

    @Override
    public Resource downloadChunkFilesFromFileId(Long fileId) {
        FileMetadata fileMetadata= checkAccessObj.userHasAccessToFile(fileId);
        List<FileChunk> chunks= fileChunkRepository.findByFileMetadataIdOrderByChunkIndexAsc(fileMetadata.getId());
        if(chunks.isEmpty())
            downloadOriginalFile(fileId);
        return fileStorageService.loadChunksAsZipResource(fileMetadata.getId(),chunks);
    }


    @Override
    public Resource downloadChunkFile(Long chunkId) {
        FileChunk fileChunk = checkAccessObj.userHasAccessToChunk(chunkId);
        return fileStorageService.loadFileAsResource(fileChunk.getFilePath());
    }
}
