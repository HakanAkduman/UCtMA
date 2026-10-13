package com.app.uctma.backend.service.impl;


import com.app.uctma.backend.model.FileChunk;
import com.app.uctma.backend.model.FileMetadata;
import com.app.uctma.backend.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        Path path = Path.of(uploadDir);

        if(!Files.exists(path))
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        path = Path.of(uploadDir, "chunks");

        if(!Files.exists(path))
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        path = Path.of(uploadDir, "mds");

        if(!Files.exists(path))
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public String saveFile(MultipartFile file) {

        if(file.isEmpty())
            throw new RuntimeException("Yüklenen dosya boş olamaz");
        String originalFileName=file.getOriginalFilename();
        String newFileName=  UUID.randomUUID().toString() + "_" + originalFileName;

        Path filePath=Paths.get(uploadDir).resolve(newFileName);
        try {
            Files.copy(file.getInputStream(),filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return filePath.toString();
    }

    @Override
    public String saveChunk(FileChunk chunk) {
        Path chunksDir = Paths.get(uploadDir, "chunks");

        String chunkFileName = chunk.getChunkName();

        Path chunkFilePath = chunksDir.resolve(chunkFileName);

        try {
            Files.writeString(
                    chunkFilePath,
                    chunk.getContent(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            throw new RuntimeException("Chunk dosyası yazılırken hata oluştu: " + chunkFileName, e);
        }

        return chunkFilePath.toString();
    }

    @Override
    public String saveMdFile(FileMetadata fileMetadata,String mdData) {

        Path mdsDir = Paths.get(uploadDir, "mds");
        String mdFileName = String.format("%d_%s.md",
                fileMetadata.getId(),
                fileMetadata.getOriginalFileName()
        );

        Path mdFilePath = mdsDir.resolve(mdFileName);
        try {
            Files.writeString(
                    mdFilePath,
                    mdData,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            throw new RuntimeException("Chunk dosyası yazılırken hata oluştu: " + mdFileName, e);
        }

        return mdFilePath.toString();
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Dosya silinirken hata oluştu: " + filePath, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Dosya bulunamadı veya okunamıyor: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Geçersiz dosya yolu: " + filePath, ex);
        }
    }

    @Override
    public Resource loadChunksAsZipResource(Long fileId, List<FileChunk> chunks) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (FileChunk chunk : chunks) {
                Path chunkPath = Paths.get(chunk.getFilePath()).toAbsolutePath().normalize();

                if (Files.exists(chunkPath)) {
                    // ZIP içi dosya adı: file_3_chunk_1.txt
                    String zipEntryName = String.format(chunk.getChunkName());
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);

                    // Dosya içeriğini ZIP akışına yazıyoruz
                    byte[] bytes = Files.readAllBytes(chunkPath);
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                }
            }

            zos.finish();

            // Oluşan ZIP dosyasını ByteArrayResource olarak dönüyoruz
            return new ByteArrayResource(baos.toByteArray()) {
                @Override
                public String getFilename() {
                    return String.format("file_%d_chunks.zip", fileId);
                }
            };

        } catch (IOException e) {
            throw new RuntimeException("Chunk dosyaları ZIP haline getirilirken hata oluştu!", e);
        }
    }

}
