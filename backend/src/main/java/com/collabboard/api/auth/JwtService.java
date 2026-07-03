package com.collabboard.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final long accessTokenSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-seconds}") long accessTokenSeconds
    ) {
        this.secret = secret;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public TokenIssue issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenSeconds);
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + json(user.getEmail()) + "\","
                + "\"uid\":" + user.getId() + ","
                + "\"role\":\"" + user.getRole().name() + "\","
                + "\"name\":\"" + json(user.getName()) + "\","
                + "\"iat\":" + now.getEpochSecond() + ","
                + "\"exp\":" + expiresAt.getEpochSecond()
                + "}";
        String unsigned = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "." + base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return new TokenIssue(unsigned + "." + sign(unsigned), accessTokenSeconds);
    }

    public Optional<String> subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long expiresAt = longValue(payload, "exp").orElse(0L);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return stringValue(payload, "sub");
        }
        catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception exception) {
            throw new IllegalStateException("JWT signing failed", exception);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Optional<String> stringValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        start += marker.length();
        int end = json.indexOf("\"", start);
        return end > start ? Optional.of(json.substring(start, end)) : Optional.empty();
    }

    private Optional<Long> longValue(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return end > start ? Optional.of(Long.parseLong(json.substring(start, end))) : Optional.empty();
    }

    public record TokenIssue(String token, long expiresInSeconds) {
    }
}

