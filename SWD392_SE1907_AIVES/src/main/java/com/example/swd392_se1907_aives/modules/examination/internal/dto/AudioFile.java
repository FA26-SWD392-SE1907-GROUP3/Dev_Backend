package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import java.nio.file.Path;

public record AudioFile(Path path, String objectKey) {
    public String contentType() {
        if (objectKey.endsWith(".wav")) {
            return "audio/wav";
        }
        if (objectKey.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (objectKey.endsWith(".mp4")) {
            return "audio/mp4";
        }
        return "audio/webm";
    }
}
