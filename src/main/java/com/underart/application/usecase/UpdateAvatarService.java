package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.MediaStorageException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.User;
import com.underart.domain.port.out.MediaStoragePort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateAvatarService {

    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private final UserRepositoryPort users;
    private final MediaStoragePort storage;

    @Transactional
    public User update(UUID userId, String type, long size, InputStream content) {
        String extension = type == null ? null : EXTENSIONS.get(type);
        if (extension == null || size <= 0 || size > 5 * 1024 * 1024) {
            throw new BusinessRuleException("Use a JPEG, PNG or WEBP image of up to 5 MB");
        }
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + extension;
        storage.upload(key, content, size, type);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String unused = status == STATUS_COMMITTED ? user.avatarKey() : key;
                if (unused == null) return;
                try {
                    storage.delete(unused);
                } catch (MediaStorageException ex) {
                    log.warn("Could not remove unused avatar {}", unused);
                }
            }
        });
        return users.save(user.withAvatar(storage.publicUrl(key), key));
    }
}
