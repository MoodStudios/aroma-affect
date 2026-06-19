package com.ovrtechnology.menu;

import com.google.gson.Gson;
import com.ovrtechnology.AromaAffect;
import dev.architectury.platform.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;

/**
 * Sends mod feedback to an OVR/OMARA backend.
 */
public final class FeedbackClient {

    // TODO: replace with the real OVR/OMARA feedback endpoint once available.
    private static final String FEEDBACK_ENDPOINT = "https://omara.ovrtechnology.com/api/feedback";

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private FeedbackClient() {}

    /** JSON payload sent to the backend. Null fields are omitted by Gson. */
    private record FeedbackPayload(
            String feedback,
            String name,
            boolean anonymous,
            String modpack,
            String modVersion,
            String mcVersion) {}

    /**
     * Submits feedback asynchronously.
     *
     * @return a future resolving to {@code true} on a 2xx response, {@code false} otherwise (network
     *     error, timeout, or non-2xx status). Never completes exceptionally.
     */
    public static CompletableFuture<Boolean> submit(String feedback, String name, boolean anonymous) {
        FeedbackPayload payload =
                new FeedbackPayload(
                        feedback,
                        anonymous ? null : blankToNull(name),
                        anonymous,
                        blankToNull(ModpackConfig.getInstance().getModpackName()),
                        modVersion(),
                        mcVersion());

        String json = GSON.toJson(payload);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(FEEDBACK_ENDPOINT))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(
                        response -> {
                            int status = response.statusCode();
                            boolean ok = status >= 200 && status < 300;
                            if (!ok) {
                                AromaAffect.LOGGER.warn(
                                        "Feedback submission returned status {}", status);
                            }
                            return ok;
                        })
                .exceptionally(
                        throwable -> {
                            AromaAffect.LOGGER.warn(
                                    "Failed to submit feedback: {}", throwable.getMessage());
                            return false;
                        });
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String modVersion() {
        try {
            return Platform.getOptionalMod(AromaAffect.MOD_ID)
                    .map(dev.architectury.platform.Mod::getVersion)
                    .orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private static String mcVersion() {
        try {
            return SharedConstants.getCurrentVersion().name();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
