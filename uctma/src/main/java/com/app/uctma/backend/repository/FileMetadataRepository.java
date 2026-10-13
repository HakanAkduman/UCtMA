package com.app.uctma.backend.repository;

import com.app.uctma.backend.model.FileMetadata;
import com.app.uctma.backend.model.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata,Long> {
    List<FileMetadata> findByUserId(Long userId);
    Optional<FileMetadata> findByIdAndUserId(Long id, Long userId);
    List<FileMetadata> findByStatus(FileStatus status);


}
