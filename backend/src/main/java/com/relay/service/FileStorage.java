package com.relay.service;

import com.relay.web.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded photos on Cloudinary when credentials are configured (production),
 * otherwise on local disk under the upload dir (local dev).
 */
@Service
public class FileStorage {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String FOLDER = "relay";

    private final Path dir;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final RestClient http = RestClient.create("https://api.cloudinary.com/v1_1");

    public FileStorage(@Value("${relay.upload-dir}") String uploadDir,
                       @Value("${relay.cloudinary.cloud-name}") String cloudName,
                       @Value("${relay.cloudinary.api-key}") String apiKey,
                       @Value("${relay.cloudinary.api-secret}") String apiSecret) throws IOException {
        this.dir = Path.of(uploadDir).toAbsolutePath();
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        if (!useCloudinary()) Files.createDirectories(dir);
    }

    private boolean useCloudinary() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    /** Saves an image and returns its URL (absolute for Cloudinary, /uploads/... locally), or null if no file was sent. */
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext) || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw ApiException.bad("Only image files (jpg, png, webp, gif) are allowed");
        }
        return useCloudinary() ? uploadToCloudinary(file, ext) : saveLocally(file, ext);
    }

    private String saveLocally(MultipartFile file, String ext) {
        String name = UUID.randomUUID() + "." + ext;
        try {
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the photo");
        }
        return "/uploads/" + name;
    }

    /** Signed upload: signature = SHA-1 of the sorted params (folder, timestamp) followed by the API secret. */
    @SuppressWarnings("unchecked")
    private String uploadToCloudinary(MultipartFile file, String ext) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String toSign = "folder=" + FOLDER + "&timestamp=" + timestamp + apiSecret;
            String signature = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-1").digest(toSign.getBytes(StandardCharsets.UTF_8)));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return UUID.randomUUID() + "." + ext; }
            });
            body.add("api_key", apiKey);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("folder", FOLDER);
            body.add("signature", signature);

            Map<String, Object> res = http.post().uri("/{cloud}/image/upload", cloudName)
                    .body(body).retrieve().body(Map.class);
            return String.valueOf(res.get("secure_url"));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Photo upload failed: " + e.getMessage());
        }
    }
}
