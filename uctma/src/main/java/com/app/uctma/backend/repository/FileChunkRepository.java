package com.app.uctma.backend.repository;

import com.app.uctma.backend.model.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk,Long> {

    List<FileChunk> findByFileMetadataIdOrderByChunkIndexAsc(Long fileMetadataId);
    void deleteByFileMetadataId(Long fileMetadataId);
}
