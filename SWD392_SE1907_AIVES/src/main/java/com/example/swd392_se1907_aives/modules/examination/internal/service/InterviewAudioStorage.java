package com.example.swd392_se1907_aives.modules.examination.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class InterviewAudioStorage {
    private final Path root;

    public record Audio(byte[] bytes, String mime, String extension) {}

    public InterviewAudioStorage(
            @Value("${app.interview.storage-dir:./storage/interviews}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public Audio validate(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bản ghi phải có nội dung và tối đa 20 MB.");
        try {
            byte[] bytes = file.getBytes();
            String mime, extension;
            if (bytes.length >= 12
                    && ascii(bytes, 0, 4).equals("RIFF")
                    && ascii(bytes, 8, 4).equals("WAVE")) {
                mime = "audio/wav";
                extension = "wav";
            } else if (bytes.length >= 4
                    && bytes[0] == (byte) 0x1a
                    && bytes[1] == (byte) 0x45
                    && bytes[2] == (byte) 0xdf
                    && bytes[3] == (byte) 0xa3) {
                mime = "audio/webm";
                extension = "webm";
            } else if (bytes.length >= 4 && ascii(bytes, 0, 4).equals("OggS")) {
                mime = "audio/ogg";
                extension = "ogg";
            } else if (bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp")) {
                mime = "audio/mp4";
                extension = "mp4";
            } else
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Định dạng bản ghi không hợp lệ. Hãy ghi lại bằng micro trong phòng thi.");
            return new Audio(bytes, mime, extension);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được bản ghi.");
        }
    }

    public String save(UUID attempt, UUID turn, byte[] bytes, String extension) {
        String key = attempt + "/" + turn + "-" + UUID.randomUUID() + "." + extension;
        try {
            Path path = path(key);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được bản ghi trên máy chủ.");
        }
    }

    public Path path(String key) {
        if (key == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có tệp âm thanh.");
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Đường dẫn âm thanh không hợp lệ.");
        return path;
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(path(key));
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy tệp âm thanh đã lưu.");
        }
    }

    public boolean exists(String key) {
        return key != null && Files.isRegularFile(path(key));
    }

    private String ascii(byte[] bytes, int offset, int count) {
        return new String(bytes, offset, count, java.nio.charset.StandardCharsets.US_ASCII);
    }
}
