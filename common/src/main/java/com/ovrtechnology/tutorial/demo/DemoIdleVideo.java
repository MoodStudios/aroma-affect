package com.ovrtechnology.tutorial.demo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Plays a looping, silent video behind the start screen of the station.
 * <p>
 * Minecraft has no video decoder, so the video is a Motion JPEG stream: one JPEG per frame,
 * back to back in a single file, {@code aromaaffect_idle_video.mjpeg} in the game folder.
 * Any video converts to it with
 * {@code ffmpeg -i video.mp4 -an -vf scale=1280:-2 -r 30 -c:v mjpeg -q:v 4 -f mjpeg aromaaffect_idle_video.mjpeg}
 * and the frame rate used there goes in {@code idleVideoFps} of the demo config.
 * <p>
 * A worker thread reads and decodes the frames in step with the clock; the render thread only
 * copies the newest one into a texture. Frames are skipped when decoding falls behind.
 * A file that cannot be played turns the video off ({@link #isBroken()}); it never stops the game.
 */
public final class DemoIdleVideo implements AutoCloseable {

    private static final String FILE_NAME = "aromaaffect_idle_video.mjpeg";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AromaAffect.MOD_ID, "dynamic/idle_video");

    /** A single frame larger than this is not a frame of a video made for this screen. */
    private static final int MAX_FRAME_BYTES = 8 * 1024 * 1024;
    private static final int MAX_FRAME_SIDE = 4096;
    /** This many frames in a row that cannot be shown and the video is given up. */
    private static final int MAX_BAD_FRAMES = 60;

    /** Decoded RGBA pixels, owned by whoever takes it out of {@link #pending}. */
    private record Frame(ByteBuffer pixels, int width, int height) {
        void free() {
            STBImage.stbi_image_free(pixels);
        }
    }

    private final Path file;
    private final long frameNanos;
    private final int maxSide;
    private final AtomicReference<Frame> pending = new AtomicReference<>();
    private final Thread worker;
    private volatile boolean running = true;
    private volatile boolean broken = false;

    private DynamicTexture texture;
    private int width;
    private int height;

    private DemoIdleVideo(Path file, int fps) {
        this.file = file;
        this.frameNanos = 1_000_000_000L / fps;
        this.maxSide = Math.min(MAX_FRAME_SIDE, RenderSystem.getDevice().getMaxTextureSize());
        this.worker = new Thread(this::decodeLoop, "AromaAffect idle video");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** The video of this station, or null when there is none in the game folder. Call from the render thread. */
    public static DemoIdleVideo openIfPresent() {
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return new DemoIdleVideo(file, DemoConfig.get().idleVideoFps());
    }

    /** True once the video has stopped for good: unreadable file, or a frame the screen cannot take. */
    public boolean isBroken() {
        return broken;
    }

    /** Width over height of the video, or 0 until the first frame is ready. */
    public float aspect() {
        return texture == null || height == 0 ? 0.0f : (float) width / height;
    }

    /**
     * Draws the current frame over the whole screen, cropping the sides or the top and bottom
     * to keep its proportions. Returns false until the first frame is ready.
     */
    public boolean render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        return render(graphics, 0, 0, screenWidth, screenHeight);
    }

    /**
     * Draws the current frame filling the given box, cropping the sides or the top and bottom
     * to keep its proportions. Returns false until the first frame is ready.
     */
    public boolean render(GuiGraphics graphics, int x, int y, int boxWidth, int boxHeight) {
        if (broken || boxWidth <= 0 || boxHeight <= 0) {
            return false;
        }
        Frame frame = pending.getAndSet(null);
        if (frame != null) {
            try {
                upload(frame);
            } catch (RuntimeException e) {
                AromaAffect.LOGGER.warn("Idle video turned off, a frame could not be drawn: {}", e.toString());
                broken = true;
                running = false;
                releaseTexture();
                return false;
            } finally {
                frame.free();
            }
        }
        if (texture == null) {
            return false;
        }

        float videoAspect = (float) width / height;
        float screenAspect = (float) boxWidth / boxHeight;
        float u0 = 0.0f, u1 = 1.0f, v0 = 0.0f, v1 = 1.0f;
        if (videoAspect > screenAspect) {
            float visible = screenAspect / videoAspect;
            u0 = (1.0f - visible) / 2.0f;
            u1 = 1.0f - u0;
        } else {
            float visible = videoAspect / screenAspect;
            v0 = (1.0f - visible) / 2.0f;
            v1 = 1.0f - v0;
        }
        graphics.blit(TEXTURE, x, y, x + boxWidth, y + boxHeight, u0, u1, v0, v1);
        return true;
    }

    private void upload(Frame frame) {
        if (texture == null || frame.width() != width || frame.height() != height) {
            releaseTexture();
            width = frame.width();
            height = frame.height();
            texture = new DynamicTexture(() -> "Aroma Affect idle video", width, height, false);
            texture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(frame.pixels()), texture.getPixels().getPointer(), (long) width * height * 4);
        texture.upload();
    }

    private void releaseTexture() {
        if (texture != null) {
            texture = null;
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
        }
    }

    /** Stops the worker and frees the texture. Call from the render thread. */
    @Override
    public void close() {
        running = false;
        worker.interrupt();
        Frame frame = pending.getAndSet(null);
        if (frame != null) {
            frame.free();
        }
        releaseTexture();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Worker thread
    // ─────────────────────────────────────────────────────────────────────────────

    private void decodeLoop() {
        try (JpegStream stream = new JpegStream(file)) {
            int framesThisPass = 0;
            int badFrames = 0;
            long showAt = System.nanoTime();
            while (running) {
                if (!stream.readFrame()) {
                    if (framesThisPass == 0) {
                        AromaAffect.LOGGER.warn("{} has no JPEG frames, the start screen stays without video", file);
                        broken = true;
                        return;
                    }
                    framesThisPass = 0;
                    stream.rewind();
                    continue;
                }
                framesThisPass++;

                long late = System.nanoTime() - showAt;
                if (late > 500_000_000L) {
                    // Far behind (slow disk, the game was frozen): start counting from here
                    showAt = System.nanoTime();
                } else if (late > frameNanos) {
                    // A frame behind: skip decoding this one to catch up
                    showAt += frameNanos;
                    continue;
                }

                Frame frame = stream.decode(maxSide);

                // Keep the pace on a bad frame too, or a file of bad frames spins this thread
                long wait = showAt - System.nanoTime();
                if (wait > 0) {
                    LockSupport.parkNanos(wait);
                }
                showAt += frameNanos;

                if (frame == null) {
                    if (++badFrames >= MAX_BAD_FRAMES) {
                        AromaAffect.LOGGER.warn("{} cannot be played (frames are not baseline JPEG up to {} px), "
                                + "the start screen stays without video", file, maxSide);
                        broken = true;
                        return;
                    }
                    continue;
                }
                badFrames = 0;

                Frame old = pending.getAndSet(frame);
                if (old != null) {
                    old.free();
                }
            }
        } catch (IOException | RuntimeException e) {
            // close() interrupts the thread, which closes the file under a read: not an error
            if (running) {
                AromaAffect.LOGGER.warn("Idle video stopped: {}", e.toString());
                broken = true;
            }
        } finally {
            if (!running) {
                // close() already ran: nobody will take what was left for the render thread
                Frame frame = pending.getAndSet(null);
                if (frame != null) {
                    frame.free();
                }
            }
        }
    }

    /** Reads a file of concatenated JPEG images one image at a time. */
    private static final class JpegStream implements AutoCloseable {

        private final Path file;
        private InputStream in;
        private final byte[] chunk = new byte[1 << 16];
        private int chunkPos = 0;
        private int chunkLen = 0;
        private boolean endOfFile = false;

        private byte[] frame = new byte[1 << 18];
        private int frameLen = 0;
        private boolean frameTooBig = false;
        private ByteBuffer direct;

        JpegStream(Path file) throws IOException {
            this.file = file;
            this.in = Files.newInputStream(file);
            // After the file is open: an unreadable file must not leave native memory behind
            this.direct = MemoryUtil.memAlloc(1 << 18);
        }

        void rewind() throws IOException {
            in.close();
            in = Files.newInputStream(file);
            chunkPos = 0;
            chunkLen = 0;
            endOfFile = false;
        }

        @Override
        public void close() throws IOException {
            MemoryUtil.memFree(direct);
            in.close();
        }

        /**
         * Reads the next image into the frame buffer. False at the end of the file.
         * Bytes that are not a well formed JPEG are skipped up to the next image.
         */
        boolean readFrame() throws IOException {
            while (true) {
                frameLen = 0;
                frameTooBig = false;

                // Start of image marker
                int previous = -1;
                while (true) {
                    int b = next();
                    if (b < 0) return false;
                    if (previous == 0xFF && b == 0xD8) break;
                    previous = b;
                }
                put(0xFF);
                put(0xD8);

                if (readSegments() && !frameTooBig) {
                    return true;
                }
                if (endOfFile) {
                    return false;
                }
            }
        }

        /** Reads the segments of one image up to its end marker. False when they are malformed or cut short. */
        private boolean readSegments() throws IOException {
            int marker = readMarker();
            while (true) {
                if (marker < 0) return false;
                if (marker == 0xD9) return true;
                if (marker == 0xD8) {
                    // A new image starts here: the one being read was cut short. Start over
                    // from this one instead of swallowing it into the broken one.
                    frameLen = 0;
                    frameTooBig = false;
                    put(0xFF);
                    put(0xD8);
                    marker = readMarker();
                    continue;
                }
                if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                    marker = readMarker();
                    continue;
                }
                int high = take();
                int low = take();
                if (low < 0) return false;
                int length = ((high << 8) | low) - 2;
                if (length < 0) return false;
                for (int i = 0; i < length; i++) {
                    if (take() < 0) return false;
                }
                // 0xDA starts the compressed image data, which has no length of its own
                marker = marker == 0xDA ? skipImageData() : readMarker();
            }
        }

        /**
         * Decodes the image in the frame buffer. Null when it is not a readable JPEG or is
         * larger than maxSide on either side.
         */
        Frame decode(int maxSide) {
            if (direct.capacity() < frameLen) {
                direct = MemoryUtil.memRealloc(direct, frameLen + (frameLen >> 1));
            }
            direct.clear();
            direct.put(frame, 0, frameLen);
            direct.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                // Size first, from the header: decoding allocates width x height x 4 bytes
                if (!STBImage.stbi_info_from_memory(direct, w, h, channels)
                        || w.get(0) < 1 || h.get(0) < 1 || w.get(0) > maxSide || h.get(0) > maxSide) {
                    return null;
                }
                ByteBuffer pixels = STBImage.stbi_load_from_memory(direct, w, h, channels, 4);
                if (pixels == null) {
                    return null;
                }
                return new Frame(pixels, w.get(0), h.get(0));
            }
        }

        private int readMarker() throws IOException {
            int b = take();
            if (b != 0xFF) return -1;
            do {
                b = take();
            } while (b == 0xFF);
            return b;
        }

        /** Runs through the compressed data and returns the marker that ends it. */
        private int skipImageData() throws IOException {
            int previous = -1;
            while (true) {
                int b = take();
                if (b < 0) return -1;
                // Inside the data 0xFF is always followed by 0x00 or a restart marker
                if (previous == 0xFF && b != 0x00 && b != 0xFF && (b < 0xD0 || b > 0xD7)) {
                    return b;
                }
                previous = b;
            }
        }

        private int take() throws IOException {
            int b = next();
            if (b >= 0) put(b);
            return b;
        }

        private int next() throws IOException {
            if (chunkPos >= chunkLen) {
                chunkLen = in.read(chunk);
                chunkPos = 0;
                if (chunkLen <= 0) {
                    chunkLen = 0;
                    endOfFile = true;
                    return -1;
                }
            }
            return chunk[chunkPos++] & 0xFF;
        }

        private void put(int b) {
            if (frameLen >= MAX_FRAME_BYTES) {
                // Keep reading to the end of the image, without keeping it
                frameTooBig = true;
                return;
            }
            if (frameLen == frame.length) {
                frame = java.util.Arrays.copyOf(frame, Math.min(MAX_FRAME_BYTES, frame.length * 2));
            }
            frame[frameLen++] = (byte) b;
        }
    }
}
