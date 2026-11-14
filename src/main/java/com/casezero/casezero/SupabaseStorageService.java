package com.casezero.casezero;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;

    private final String projectUrl;
    private final String secretKey;
    private final String bucket;
    private final HttpClient http = HttpClient.newHttpClient();

    public SupabaseStorageService(
            @Value("${CASEZERO_SUPABASE_URL:}") String projectUrl,
            @Value("${CASEZERO_SUPABASE_SECRET_KEY:${CASEZERO_SUPABASE_SERVICE_ROLE_KEY:}}") String secretKey,
            @Value("${CASEZERO_SUPABASE_STORAGE_BUCKET:casezero-evidence}") String bucket) {
        this.projectUrl = projectUrl == null ? "" : projectUrl.replaceAll("/+$", "");
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.bucket = bucket == null ? "casezero-evidence" : bucket.trim();
    }

    public UploadResult upload(Long draftId, MultipartFile file) {
        ensureConfigured();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file before uploading.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Each evidence file must be 20 MB or smaller.");
        }

        String original = safeFilename(file.getOriginalFilename());
        String extension = extension(original);
        String contentType = allowedContentType(extension);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("The selected file could not be read.");
        }
        if (!hasExpectedSignature(extension, bytes)) {
            throw new IllegalArgumentException("The file content does not match an allowed image, PDF or video format.");
        }

        String storedName = UUID.randomUUID() + "_" + original;
        String objectPath = "investigations/" + draftId + "/" + storedName;
        HttpRequest request = HttpRequest.newBuilder(objectUri(objectPath))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Supabase Storage rejected the upload (HTTP " + response.statusCode() + "). Check the private bucket setup and server key.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not connect to Supabase Storage.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The upload was interrupted.", ex);
        }

        return new UploadResult(objectPath, original, contentType, bytes.length);
    }

    public byte[] download(String objectPath) {
        ensureConfigured();
        HttpRequest request = HttpRequest.newBuilder(objectUri("authenticated/" + objectPath))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Supabase Storage could not retrieve this evidence file (HTTP " + response.statusCode() + ").");
            }
            return response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not connect to Supabase Storage.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The download was interrupted.", ex);
        }
    }

    public void delete(String objectPath) {
        ensureConfigured();
        String json = "{\"prefixes\":[\"" + objectPath.replace("\\", "") + "\"]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(projectUrl + "/storage/v1/object/" + encode(bucket)))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Supabase Storage could not remove this evidence file (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not connect to Supabase Storage.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The delete request was interrupted.", ex);
        }
    }

    private URI objectUri(String path) {
        StringBuilder encoded = new StringBuilder();
        for (String segment : path.split("/")) {
            if (encoded.length() > 0) encoded.append('/');
            encoded.append(encode(segment));
        }
        return URI.create(projectUrl + "/storage/v1/object/" + encode(bucket) + "/" + encoded);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void ensureConfigured() {
        if (projectUrl.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("Evidence storage is not configured yet. Set CASEZERO_SUPABASE_URL and CASEZERO_SUPABASE_SECRET_KEY on this computer.");
        }
        if (!bucket.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("The configured evidence bucket name is invalid.");
        }
    }

    private String safeFilename(String value) {
        String name = value == null ? "evidence" : value.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.length() > 120) name = "evidence";
        return name;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String allowedContentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "mp4", "m4v" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            default -> throw new IllegalArgumentException("Allowed files: JPG, PNG, WEBP, PDF, MP4, WEBM and MOV.");
        };
    }

    private boolean hasExpectedSignature(String ext, byte[] b) {
        if (b.length < 4) return false;
        return switch (ext) {
            case "jpg", "jpeg" -> (b[0] & 255) == 0xFF && (b[1] & 255) == 0xD8 && (b[2] & 255) == 0xFF;
            case "png" -> b.length >= 8 && (b[0] & 255) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47;
            case "webp" -> b.length >= 12 && new String(b, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(b, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "pdf" -> new String(b, 0, 4, StandardCharsets.US_ASCII).equals("%PDF");
            case "mp4", "m4v", "mov" -> b.length >= 12
                    && new String(b, 4, 4, StandardCharsets.US_ASCII).equals("ftyp");
            case "webm" -> (b[0] & 255) == 0x1A && (b[1] & 255) == 0x45
                    && (b[2] & 255) == 0xDF && (b[3] & 255) == 0xA3;
            default -> false;
        };
    }

    public record UploadResult(String path, String filename, String contentType, long size) {}
}
