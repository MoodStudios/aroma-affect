package com.ovrtechnology.util;

import com.ovrtechnology.AromaAffect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

public final class ResourceUtil {
    public static int[] getTextureDimensions(Identifier texture) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open(); ImageInputStream inputStream = ImageIO.createImageInputStream(stream)) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        reader.setInput(inputStream);
                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        reader.dispose();
                        return new int[]{width, height};
                    }
                }

            }
        } catch (Exception e) {
            AromaAffect.LOGGER.error(e.getMessage());
        }
        return new int[]{16, 16};
    }

    public static int getTextureWidth(Identifier texture) {
        return getTextureDimensions(texture)[0];
    }

    public static int getTextureHeight(Identifier texture) {
        return getTextureDimensions(texture)[1];
    }
}
