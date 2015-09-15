package com.n11.imic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class JpegReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpegReader.class);

    public BufferedImage readImage(InputStream imageData) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(imageData);
        for (Iterator<ImageReader> iterator = ImageIO.getImageReaders(stream); iterator.hasNext();) {
            ImageReader imageReader = iterator.next();
            imageReader.setInput(stream);
            try {
                BufferedImage image = imageReader.read(0);
                stream.close();
                return image;
            } catch (IIOException e) {
                LOGGER.error("Reading stream failed, attempting to read with next JPEG reader.", e);
            }
        }
        return null;
    }

}
