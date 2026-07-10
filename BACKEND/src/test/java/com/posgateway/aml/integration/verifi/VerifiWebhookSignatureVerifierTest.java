package com.posgateway.aml.integration.verifi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifiWebhookSignatureVerifierTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private VerifiWebhookSignatureVerifier legacyVerifier;
    private VerifiJwsAuthenticator jwsAuthenticator;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        legacyVerifier = new VerifiWebhookSignatureVerifier(mapper);
        jwsAuthenticator = new VerifiJwsAuthenticator(mapper);
    }

    @Test
    void verifyApiKey_acceptsMatchingHeader() {
        assertTrue(legacyVerifier.verifyApiKey(
                Map.of("X-Api-Key", "secret-key"),
                "secret-key"));
    }

    @Test
    void verifyApiKey_rejectsMismatch() {
        assertFalse(legacyVerifier.verifyApiKey(
                Map.of("X-Api-Key", "wrong"),
                "secret-key"));
    }

    @Test
    void verifyBearerToken_acceptsValidJws() throws Exception {
        String token = buildJws(SECRET, "token-1", Instant.now().getEpochSecond());
        assertTrue(jwsAuthenticator.verifyBearerToken("Bearer " + token, SECRET));
    }

    @Test
    void verifyBearerToken_rejectsReplayedJti() throws Exception {
        long now = Instant.now().getEpochSecond();
        String token = buildJws(SECRET, "replay-jti", now);
        assertTrue(jwsAuthenticator.verifyBearerToken("Bearer " + token, SECRET));
        assertFalse(jwsAuthenticator.verifyBearerToken("Bearer " + token, SECRET));
    }

    @Test
    void verifyBearerToken_rejectsWrongSecret() throws Exception {
        String token = buildJws(SECRET, "token-2", Instant.now().getEpochSecond());
        assertFalse(jwsAuthenticator.verifyBearerToken("Bearer " + token, "wrong-secret-wrong-secret-wrong-sec"));
    }

    private static String buildJws(String secret, String jti, long iat) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"jti\":\"%s\",\"iat\":%d,\"exp\":%d}", jti, iat, iat + 300);
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }
}
