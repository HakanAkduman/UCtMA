package com.app.uctma.backend.controller;


import com.app.uctma.backend.dto.ChunkListResDTO;
import com.app.uctma.backend.dto.ConvertFileReqDTO;
import com.app.uctma.backend.dto.FileProcessingCallbackReqDTO;
import com.app.uctma.backend.dto.FileUploadResDTO;
import com.app.uctma.backend.dto.UrlConvertReqDTO;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.model.ConversionType;
import com.app.uctma.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/uctma/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResDTO>> fileUpload (ConvertFileReqDTO convertFileReqDTO){
        FileUploadResDTO response = fileService.uploadFileToConvert(convertFileReqDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResDTO>> convertUrl(@RequestBody UrlConvertReqDTO request) {
        FileUploadResDTO response = fileService.uploadUrlToConvert(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> processCallback(@RequestBody FileProcessingCallbackReqDTO callbackDTO) {
        fileService.handleFileProcessingCallback(callbackDTO);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileUploadResDTO>>> getUserFiles() {
        List<FileUploadResDTO> files = fileService.getUserFiles();
        return ResponseEntity.ok(ApiResponse.success(files));
    }
    @GetMapping("/{fileId}/chunks")
    public ResponseEntity<ApiResponse<List<ChunkListResDTO>>> getUserFiles(@PathVariable Long fileId) {
        List<ChunkListResDTO> files = fileService.getListChunksOfFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @PostMapping("/{fileId}/convert")
    public ResponseEntity<ApiResponse<Void>> reconvertFile(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "CONVERT") ConversionType conversionType,
            @RequestParam(required = false) Integer maxTokens) {
        fileService.reconvertFile(fileId, conversionType, maxTokens);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @DeleteMapping("/chunks/{chunkId}")
    public ResponseEntity<ApiResponse<Void>> deleteChunk(@PathVariable Long chunkId) {
        fileService.deleteChunk(chunkId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        Resource resource = fileService.downloadOriginalFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}/download-md")
    public ResponseEntity<Resource> downloadMdFile(@PathVariable Long fileId) {
        Resource resource = fileService.downloadMdFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/chunks/{chunkId}/download")
    public ResponseEntity<Resource> downloadChunk(@PathVariable Long chunkId) {
        Resource resource = fileService.downloadChunkFile(chunkId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/chunks/{fileId}/download-all")
    public ResponseEntity<Resource> downloadAllChunks(@PathVariable Long fileId) {
        Resource zipResource = fileService.downloadChunkFilesFromFileId(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipResource.getFilename() + "\"")
                .body(zipResource);
    }


}
