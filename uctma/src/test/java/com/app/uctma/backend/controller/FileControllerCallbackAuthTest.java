package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.FileProcessingCallbackReqDTO;
import com.app.uctma.backend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class FileControllerCallbackAuthTest {

    private static final String CALLBACK_SECRET = "test-callback-secret";

    private FileService fileService;
    private FileController fileController;

    @BeforeEach
    void setUp() {
        fileService = mock(FileService.class);
        fileController = new FileController(fileService);
        ReflectionTestUtils.setField(fileController, "callbackSharedSecret", CALLBACK_SECRET);
    }

    @Test
    void rejectsCallbackWithoutSecret() {
        var response = fileController.processCallback(null, new FileProcessingCallbackReqDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(fileService);
    }

    @Test
    void rejectsCallbackWithIncorrectSecret() {
        var response = fileController.processCallback("wrong-secret", new FileProcessingCallbackReqDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(fileService);
    }

    @Test
    void acceptsCallbackWithCorrectSecret() {
        var callback = new FileProcessingCallbackReqDTO();

        var response = fileController.processCallback(CALLBACK_SECRET, callback);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fileService).handleFileProcessingCallback(callback);
    }
}
