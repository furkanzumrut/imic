package com.n11.imic;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.*;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class ImageScaler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageScaler.class);

    private ImageScaler() {
    }

    public static BufferedImage readImage(InputStream inputImage, String mimeContentType, ScalerParam scalerParam) throws IOException, ImageConverterException {
        BufferedImage img = null;
        if (mimeContentType.endsWith("jpeg") || mimeContentType.endsWith("jpg")) {
            img = new JpegReader().readImage(inputImage);
        }

        if (img != null) {
            return img;
        }

        ImageInputStream imgis = ImageIO.createImageInputStream(new BufferedInputStream(inputImage));
        Iterator<ImageReader> iter = ImageIO.getImageReaders(imgis);

        Exception lastException = null;
        while (iter.hasNext()) {
            ImageReader reader = null;
            try {
                reader = iter.next();
                ImageReadParam param = reader.getDefaultReadParam();
                reader.setInput(imgis, true, true);
                reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                    @Override
                    public void warningOccurred(ImageReader imageReader, String s) {
                        LOGGER.warn("Image read warning: " + s);
                    }
                });
                Iterator<ImageTypeSpecifier> imageTypes = reader.getImageTypes(0);
                while (imageTypes.hasNext()) {
                    ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
                    int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
                    if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY || bufferedImageType == BufferedImage.TYPE_BYTE_INDEXED) {
                        param.setDestinationType(imageTypeSpecifier);
                        break;
                    }
                }
                img = reader.read(0, param);
                if (mimeContentType.matches("^image/gif") && scalerParam.getAnimatedGifMode() == AnimatedGIFMode.PASSTHROUGH) {
                    // Attempt to read GIF frame #2, in which case an exception should be thrown.
                    try {
                        reader.read(1, param);
                        throw new ImageConverterException("Scaling animated GIF images is not supported.");
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.debug("No problem for images with a single frame, keep going.", e);
                    }
                }
                if (null != img) {
                    imgis.close();
                    break;
                }
            } catch (Exception e) {
                lastException = e;
            } finally {
                if (null != reader) {
                    reader.dispose();
                }
            }
        }
        // If you don't have an image at the end of all readers
        if (img == null && lastException != null) {
            LOGGER.error("Exception: ", lastException);
            throw new RuntimeException(lastException);
        }

        if (lastException instanceof ImageConverterException && lastException.getMessage().contains("GIF") && scalerParam.getAnimatedGifMode() == AnimatedGIFMode.PASSTHROUGH) {
            throw (ImageConverterException) lastException;
        }

        return img;
    }

    public static BufferedImage scaleImage(InputStream inputImage, String mimeContentType, ScalerParam scalerParam) throws IOException, ImageConverterException {
        BufferedImage img = readImage(inputImage, mimeContentType, scalerParam);
        if (!scalerParam.isUpScale()) {
            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.max((double) h / (double) scalerParam.getTargetHeight(), (double) w / (double) scalerParam.getTargetWidth());
            int targetHeight = new Double(Math.floor(h / scale)).intValue();
            int targetWidth = new Double(Math.floor(w / scale)).intValue();
            if (targetWidth >= img.getWidth() || targetHeight >= img.getHeight()) {
                LOGGER.debug("Not up-scaling image.");
                return img;
            }
        }
        return scaleImage(img, scalerParam);
    }

    private static BufferedImage scaleImage(BufferedImage img, ScalerParam scalerParam) {

        BufferedImage imgScaled;
        if (scalerParam.getTargetWidth() == scalerParam.getTargetHeight() && scalerParam.getTargetWidth() == ScalerParam.UNDEFINED_SIZE) {
            /* Optimize-only */
            imgScaled = img;
        } else if (scalerParam.getTargetWidth() == scalerParam.getTargetHeight() || scalerParam.isHasPadding()) {
            /* Scale and fit to a boundary box, paint background with specified color or white */
            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.max((double) h / (double) scalerParam.getTargetHeight(), (double) w / (double) scalerParam.getTargetWidth());
            int targetHeight = new Double(Math.floor(h / scale)).intValue();
            int targetWidth = new Double(Math.floor(w / scale)).intValue();
            /* Ignore rounding errors up to 1px for all edges, 2px max. */
            int roundErrorDiff = Math.abs(targetWidth - scalerParam.getTargetWidth());
            if (roundErrorDiff > 0 && roundErrorDiff <= 2) {
                targetWidth = scalerParam.getTargetWidth();
            }
            roundErrorDiff = Math.abs(targetHeight - scalerParam.getTargetHeight());
            if (roundErrorDiff > 0 && roundErrorDiff <= 2) {
                targetHeight = scalerParam.getTargetHeight();
            }
            imgScaled = Scalr.resize(img, scalerParam.getScalingMethod(), Scalr.Mode.FIT_TO_HEIGHT, targetWidth, targetHeight);
            if (imgScaled.getWidth() != scalerParam.getTargetWidth() || imgScaled.getHeight() != scalerParam.getTargetHeight()) {
                BufferedImage paddedImage = new BufferedImage(scalerParam.getTargetWidth(), scalerParam.getTargetHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D gi = paddedImage.createGraphics();
                gi.setComposite(AlphaComposite.SrcOver);
                Color fillRectColor = scalerParam.getPaddingColor() != null && scalerParam.getPaddingColor().length() == 6 ? Color.decode("#" + scalerParam.getPaddingColor()) : Color.WHITE;
                if (scalerParam.getPaddingColor() != null && "t".equals(scalerParam.getPaddingColor())) {
                    fillRectColor = Color.WHITE;
                }
                gi.setColor(fillRectColor);
                gi.fillRect(0, 0, scalerParam.getTargetWidth(), scalerParam.getTargetHeight());
                gi.drawImage(imgScaled, (int) Math.floor((scalerParam.getTargetWidth() - imgScaled.getWidth()) / 2.0), (int) Math.floor((scalerParam.getTargetHeight() - imgScaled.getHeight()) / 2.0), null);
                gi.dispose();
                imgScaled = paddedImage;
            }
        } else if (scalerParam.getTargetWidth() <= 0 && scalerParam.getTargetHeight() > 0) {
            /* Scale to specified height, keep aspect ratio */
            imgScaled = Scalr.resize(img, scalerParam.getScalingMethod(), Scalr.Mode.FIT_TO_HEIGHT, scalerParam.getTargetHeight());
        } else {
            /* Scale to specified width, keep aspect ratio */
            imgScaled = Scalr.resize(img, scalerParam.getScalingMethod(), Scalr.Mode.FIT_TO_WIDTH, scalerParam.getTargetWidth());
        }
        if (scalerParam.getPadding() > 0) {
            imgScaled = scalerParam.getPaddingColor() != null && scalerParam.getPaddingColor().length() == 6 ? Scalr.pad(imgScaled, scalerParam.getPadding(), Color.decode("#" + scalerParam.getPaddingColor())) : Scalr.pad(imgScaled, scalerParam.getPadding());
        }

        return imgScaled;
    }

    public static void writeImageToStream(BufferedImage img, OutputStream stream, String mimeContentType, ScalerParam scalerParam) throws IOException {
        ImageWriter imgWriter = ImageIO.getImageWritersByMIMEType(mimeContentType.matches("^image/gif") ? "image/png" : mimeContentType).next();
        ImageWriteParam imgWriterParams = imgWriter.getDefaultWriteParam();
        if (mimeContentType.matches("^image/(jpeg|jpg)")) {
            imgWriterParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imgWriterParams.setCompressionQuality(scalerParam.getQuality());
            imgWriterParams.setProgressiveMode(scalerParam.getProgressiveMode() ? ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);
        }
        ImageOutputStream ios = ImageIO.createImageOutputStream(stream);
        imgWriter.setOutput(ios);
        try {
            imgWriter.write(null, new IIOImage(img, null, null), imgWriterParams);
        } finally {
            imgWriter.dispose();
            ios.close();
        }
    }
}
