package com.app.uctma.backend.service.impl;

import com.app.uctma.backend.Utils.CheckAccess;
import com.app.uctma.backend.dto.FileMessageDTO;
import com.app.uctma.backend.dto.FileProcessingCallbackReqDTO;
import com.app.uctma.backend.dto.UrlConvertReqDTO;
import com.app.uctma.backend.model.ConversionType;
import com.app.uctma.backend.model.FileMetadata;
import com.app.uctma.backend.model.FileStatus;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.FileChunkRepository;
import com.app.uctma.backend.repository.FileMetadataRepository;
import com.app.uctma.backend.service.FileStorageService;
import com.app.uctma.backend.service.RabbitFileMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileProcessingCallbackTest {

    private FileMetadataRepository fileMetadataRepository;
    private FileChunkRepository fileChunkRepository;
    private CheckAccess checkAccess;
    private RabbitFileMessageService rabbitFileMessageService;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileMetadataRepository = mock(FileMetadataRepository.class);
        fileChunkRepository = mock(FileChunkRepository.class);
        checkAccess = mock(CheckAccess.class);
        rabbitFileMessageService = mock(RabbitFileMessageService.class);
        fileService = new FileServiceImpl(
                fileMetadataRepository,
                mock(FileStorageService.class),
                fileChunkRepository,
                rabbitFileMessageService,
                checkAccess
        );
    }

    @Test
    void failedCallbackStoresErrorMessage() {
        FileMetadata fileMetadata = metadataWithId(12L);
        when(fileMetadataRepository.findById(12L)).thenReturn(Optional.of(fileMetadata));

        fileService.handleFileProcessingCallback(FileProcessingCallbackReqDTO.builder()
                .fileId(12L)
                .status(FileStatus.FAILED)
                .errorMessage("Desteklenmeyen dosya formatı")
                .build());

        assertEquals(FileStatus.FAILED, fileMetadata.getStatus());
        assertEquals("Desteklenmeyen dosya formatı", fileMetadata.getErrorMessage());
        verify(fileMetadataRepository).save(fileMetadata);
    }

    @Test
    void processingCallbackClearsPreviousErrorMessage() {
        FileMetadata fileMetadata = metadataWithId(13L);
        fileMetadata.setErrorMessage("Önceki dönüşüm hatası");
        when(fileMetadataRepository.findById(13L)).thenReturn(Optional.of(fileMetadata));

        fileService.handleFileProcessingCallback(FileProcessingCallbackReqDTO.builder()
                .fileId(13L)
                .status(FileStatus.PROCESSING)
                .build());

        assertEquals(FileStatus.PROCESSING, fileMetadata.getStatus());
        assertNull(fileMetadata.getErrorMessage());
        verify(fileMetadataRepository).save(fileMetadata);
    }

    @Test
    void reconvertClearsPreviousErrorMessage() {
        FileMetadata fileMetadata = metadataWithId(15L);
        fileMetadata.setStatus(FileStatus.FAILED);
        fileMetadata.setErrorMessage("Önceki dönüşüm hatası");
        when(checkAccess.userHasAccessToFile(15L)).thenReturn(fileMetadata);
        when(fileChunkRepository.findByFileMetadataIdOrderByChunkIndexAsc(15L))
                .thenReturn(List.of());

        fileService.reconvertFile(15L, ConversionType.CONVERT, null);

        assertEquals(FileStatus.PENDING, fileMetadata.getStatus());
        assertNull(fileMetadata.getErrorMessage());
        verify(fileMetadataRepository).save(fileMetadata);
    }

    @Test
    void reconvertRejectsFilesThatAreStillPendingOrProcessing() {
        for (FileStatus status : List.of(FileStatus.PENDING, FileStatus.PROCESSING)) {
            FileMetadata fileMetadata = metadataWithId(17L);
            fileMetadata.setStatus(status);
            when(checkAccess.userHasAccessToFile(17L)).thenReturn(fileMetadata);

            assertThrows(
                    RuntimeException.class,
                    () -> fileService.reconvertFile(17L, ConversionType.CONVERT_AND_CHUNK, null)
            );
        }

        verifyNoInteractions(fileMetadataRepository, fileChunkRepository, rabbitFileMessageService);
    }

    @Test
    void reconvertPublishesMessageOnlyAfterTransactionCommit() {
        FileMetadata fileMetadata = metadataWithId(16L);
        fileMetadata.setStatus(FileStatus.COMPLETED);
        when(checkAccess.userHasAccessToFile(16L)).thenReturn(fileMetadata);
        when(fileChunkRepository.findByFileMetadataIdOrderByChunkIndexAsc(16L))
                .thenReturn(List.of());
        TransactionSynchronizationManager.initSynchronization();

        try {
            fileService.reconvertFile(16L, ConversionType.CONVERT, null);

            verifyNoInteractions(rabbitFileMessageService);

            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(rabbitFileMessageService).sendFileProcessingMessage(
                    org.mockito.ArgumentMatchers.any(FileMessageDTO.class)
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void urlUploadPublishesMessageOnlyAfterMetadataCommit() {
        when(checkAccess.checkUserValid()).thenReturn(User.builder().build());
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata metadata = invocation.getArgument(0);
            metadata.setId(88L);
            return metadata;
        });
        TransactionSynchronizationManager.initSynchronization();

        try {
            fileService.uploadUrlToConvert(UrlConvertReqDTO.builder()
                    .url("https://www.deu.edu.tr/")
                    .conversionType(ConversionType.CONVERT_AND_CHUNK)
                    .maxTokens(400)
                    .build());

            verifyNoInteractions(rabbitFileMessageService);

            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(rabbitFileMessageService).sendFileProcessingMessage(
                    org.mockito.ArgumentMatchers.argThat(message ->
                            message.getFileId().equals(88L)
                                    && "URL".equals(message.getFileType())
                    )
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void failedCallbackWithoutMessageDoesNotPersistBlankError() {
        FileMetadata fileMetadata = metadataWithId(14L);
        when(fileMetadataRepository.findById(14L)).thenReturn(Optional.of(fileMetadata));

        fileService.handleFileProcessingCallback(FileProcessingCallbackReqDTO.builder()
                .fileId(14L)
                .status(FileStatus.FAILED)
                .errorMessage("  ")
                .build());

        assertNull(fileMetadata.getErrorMessage());
        verify(fileMetadataRepository).save(fileMetadata);
    }

    private FileMetadata metadataWithId(Long id) {
        return FileMetadata.builder()
                .id(id)
                .originalFileName("document.pdf")
                .fileType("application/pdf")
                .status(FileStatus.PENDING)
                .mdFilePath("")
                .build();
    }
}
