package com.nixspace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "nixspace")
public record NixSpaceProperties(
        Jwt jwt,
        Aws aws,
        WebSocket websocket,
        Upload upload
) {

    public record Jwt(
            String secret,
            long accessTokenExpiry,
            long refreshTokenExpiry
    ) {}

    public record Aws(
            String region,
            S3 s3
    ) {
        public record S3(String bucket, long presignedUrlExpiry) {}
    }

    public record WebSocket(List<String> allowedOrigins) {}

    public record Upload(long maxFileSize, List<String> allowedMimeTypes) {}
}
