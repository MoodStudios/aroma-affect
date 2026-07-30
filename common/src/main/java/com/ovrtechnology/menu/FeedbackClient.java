package com.ovrtechnology.menu;

import com.google.gson.Gson;
import com.ovrtechnology.AromaAffect;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Sends mod feedback to an OVR/OMARA backend.
 */
public final class FeedbackClient {

    // Endpoint and shared secret are injected at build time (see common/build.gradle);
    // override for local emulator testing with -PfeedbackEndpoint / -PfeedbackHmacSecret.
    private static final String FEEDBACK_ENDPOINT = FeedbackConfig.ENDPOINT;

    private static final String FEEDBACK_HMAC_SECRET = FeedbackConfig.HMAC_SECRET;

    private static final String TIMESTAMP_HEADER = "X-Aroma-Timestamp";
    private static final String SIGNATURE_HEADER = "X-Aroma-Signature";

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private FeedbackClient() {}

    /**
     * JSON payload sent to the backend. Null fields are omitted by Gson.
     */
    private record FeedbackPayload(String feedback, String name, boolean anonymous,
                                   String modpack, String modVersion, String mcVersion) {}

    /**
     * Submits feedback asynchronously.
     *
     * @return a future resolving to {@code true} on a 2xx response, {@code false} otherwise
     *         (network error, timeout, or non-2xx status). Never completes exceptionally.
     */
    public static CompletableFuture<Boolean> submit(String feedback, String name, boolean anonymous) {
        if (!isConfigured()) {
            AromaAffect.LOGGER.warn(
                    "Feedback submission is unavailable because no HMAC secret was provided at build time");
            return CompletableFuture.completedFuture(false);
        }

        FeedbackPayload payload = new FeedbackPayload(
                feedback,
                anonymous ? null : blankToNull(name),
                anonymous,
                blankToNull(ModpackConfig.getInstance().getModpackName()),
                modVersion(),
                mcVersion());

        String json = GSON.toJson(payload);
        String timestamp = Long.toString(System.currentTimeMillis());
        String signature = sign(timestamp, json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FEEDBACK_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header(TIMESTAMP_HEADER, timestamp)
                .header(SIGNATURE_HEADER, signature)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    int status = response.statusCode();
                    boolean ok = status >= 200 && status < 300;
                    if (!ok) {
                        AromaAffect.LOGGER.warn("Feedback submission returned status {}", status);
                    }
                    return ok;
                })
                .exceptionally(throwable -> {
                    AromaAffect.LOGGER.warn("Failed to submit feedback: {}", throwable.getMessage());
                    return false;
                });
    }

    static boolean isConfigured() {
        return FEEDBACK_HMAC_SECRET != null && !FEEDBACK_HMAC_SECRET.isBlank();
    }

    /**
     * Computes the request signature the backend verifies:
     * {@code hex(HMAC-SHA256(secret, timestamp + "." + body))}, over the exact JSON body bytes
     * that are sent.
     */
    private static String sign(String timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(FEEDBACK_HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign feedback request", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Baked in at build time from {@code mod_version}: 26.1 has no Architectury Platform API
     * to query the loaded mod at runtime.
     */
    private static String modVersion() {
        String version = FeedbackConfig.MOD_VERSION;
        return version == null || version.isBlank() ? "unknown" : version;
    }

    /**
     * Baked in at build time from {@code minecraft_version}: the jar is built against a single
     * Minecraft version, so this is exact without touching a version-sensitive runtime API.
     */
    private static String mcVersion() {
        String version = FeedbackConfig.MC_VERSION;
        return version == null || version.isBlank() ? "unknown" : version;
    }
}
