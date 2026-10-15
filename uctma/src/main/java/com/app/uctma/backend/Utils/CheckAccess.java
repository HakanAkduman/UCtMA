package com.app.uctma.backend.Utils;

import com.app.uctma.backend.model.FileChunk;
import com.app.uctma.backend.model.FileMetadata;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.FileChunkRepository;
import com.app.uctma.backend.repository.FileMetadataRepository;
import com.app.uctma.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CheckAccess {


    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;
    private final UserRepository userRepository;

    public FileMetadata userHasAccessToFile(Long fileId){
        User user = checkUserValid();

        FileMetadata fileMetadata=fileMetadataRepository.findById(fileId).orElseThrow(()-> new RuntimeException("Dosya bulunamadı"));

        if (!fileMetadata.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Yetkisiz işlem!");
        }
        return fileMetadata;
    }
    public FileChunk userHasAccessToChunk(Long chunkId){
        User user = checkUserValid();

        FileChunk fileChunk=fileChunkRepository.findById(chunkId).orElseThrow(()-> new RuntimeException("Dosya bulunamadı"));

        if (!fileChunk.getFileMetadata().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Yetkisiz işlem!");
        }
        return fileChunk;
    }

    public User checkUserValid(){
        String currentUserMail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUserMail==null )
            throw new RuntimeException("Kullanıcı girişi yapılmadı!");
        return userRepository.findByEmail(currentUserMail).orElseThrow(()-> new RuntimeException("Kullanıcı bulunamadı: "+ currentUserMail));
    }

}
