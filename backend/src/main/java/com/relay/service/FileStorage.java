package com.relay.service;

import com.relay.web.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorage {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path dir;

    public FileStorage(@Value("${relay.upload-dir}") String uploadDir) throws IOException {
        this.dir = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
    }

    /** Saves an image and returns its public URL path, or null if no file was sent. */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext) || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw ApiException.bad("Only image files (jpg, png, webp, gif) are allowed");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the photo");
        }
        return "/uploads/" + name;
    }
}
