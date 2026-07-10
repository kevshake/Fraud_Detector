package com.posgateway.aml.integration.verifi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Validates Verifi API 3.0 {@code Authorization: Bearer <JWS>} tokens per the official spec:
 * HMAC-SHA256 over {@code base64url(header) + '.' + base64url(payload)} with a shared secret,
 * plus {@code jti}/{@code iat}/{@code exp} claim checks.
 */
@Component
public class VerifiJwsAuthenticator {

    private static final String EXPECTED_ALG = "HS256";
    private static final long JTI_REPLAY_WINDOW_SECONDS = 360;
    private static final long MAX_IAT_FUTURE_SECONDS = 60;
    private static final long MAX_IAT_PAST_SECONDS = 300;
    private static final long MAX_EXP_AFTER_IAT_SECONDS = 300;

    private final ObjectMapper objectMapper;
    private final Cache<String, Boolean> recentJti;

    public VerifiJwsAuthenticator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.recentJti = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(JTI_REPLAY_WINDOW_SECONDS))
                .maximumSize(10_000)
                .build();
    }

    /**
     * @return true when the Bearer token is a valid JWS signed with {@code sharedSecret}
     */
    public boolean verifyBearerToken(String authorizationHeader, String sharedSecret) {
        if (authorizationHeader == null || authorizationHeader.isBlank()
                || sharedSecret == null || sharedSecret.isBlank()) {
            return false;
        }
        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (token.isEmpty()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            return verifyJws(parts[0], parts[1], parts[2], sharedSecret);
        }
        // JWE tokens have 5 parts — requires RSA private key decryption (not implemented here)
        return false;
    }

    private boolean verifyJws(String encodedHeader, String encodedPayload, String encodedSignature,
                              String sharedSecret) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(encodedHeader), StandardCharsets.UTF_8);
            JsonNode header = objectMapper.readTree(headerJson);
            if (!EXPECTED_ALG.equals(header.path("alg").asText())) {
                return false;
            }

            String signingInput = encodedHeader + "." + encodedPayload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expectedBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            String expectedSig = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(expectedBytes);
            if (!constantTimeEquals(expectedSig, encodedSignature)) {
                return false;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payloadJson);

            String jti = claims.path("jti").asText(null);
            if (jti == null || jti.isBlank()) {
                return false;
            }
            if (recentJti.getIfPresent(jti) != null) {
                return false;
            }

            long now = Instant.now().getEpochSecond();
            if (!claims.has("iat") || !claims.has("exp")) {
                return false;
            }
            long iat = claims.get("iat").asLong();
            long exp = claims.get("exp").asLong();

            if (iat > now + MAX_IAT_FUTURE_SECONDS) {
                return false;
            }
            if (iat < now - MAX_IAT_PAST_SECONDS) {
                return false;
            }
            if (exp <= now) {
                return false;
            }
            if (exp > iat + MAX_EXP_AFTER_IAT_SECONDS) {
                return false;
            }

            recentJti.put(jti, Boolean.TRUE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
