package com.n11.imic;

import org.imgscalr.Scalr;

public class ScalerParam {

    public static final int UNDEFINED_SIZE = -1;

    public static final int THIRTY_DAYS = 30 * 24 * 60 * 60;

    private float quality = 0.8f;
    private int padding = UNDEFINED_SIZE;
    private int targetWidth = UNDEFINED_SIZE;
    private int targetHeight = UNDEFINED_SIZE;
    private String paddingColor;
    private boolean hasPadding;
    private Scalr.Method scalingMethod = Scalr.Method.QUALITY;
    private boolean progressiveMode = true;
    private int httpExpires = THIRTY_DAYS;
    private boolean upScale = true;
    private AnimatedGIFMode animatedGifMode = AnimatedGIFMode.SCALEFIRSTFRAMEPNG;

    public ScalerParam withHttpExpires(int httpExpires) {
        setHttpExpires(httpExpires);
        return this;
    }

    public ScalerParam withUpScale(boolean upScale) {
        setUpScale(upScale);
        return this;
    }

    public ScalerParam withProgressiveMode(boolean progressiveMode) {
        setProgressiveMode(progressiveMode);
        return this;
    }

    public ScalerParam withMethod(Scalr.Method scalingMethod) {
        setScalingMethod(scalingMethod);
        return this;
    }

    public ScalerParam withPaddingColor(String paddingColor) {
        setPaddingColor(paddingColor);
        return this;
    }

    public ScalerParam withHeight(int height) {
        setTargetHeight(height);
        return this;
    }

    public ScalerParam withWidth(int targetWidth) {
        setTargetWidth(targetWidth);
        return this;
    }

    public ScalerParam withHasPadding(boolean hasPadding) {
        setHasPadding(hasPadding);
        return this;
    }

    public boolean isHasPadding() {
        return hasPadding;
    }

    public void setHasPadding(boolean hasPadding) {
        this.hasPadding = hasPadding;
    }

    public ScalerParam withPadding(int padding) {
        setPadding(padding);
        return this;
    }

    public ScalerParam withQuality(float quality) {
        setQuality(quality);
        return this;
    }

    public boolean getProgressiveMode() {
        return progressiveMode;
    }

    public void setProgressiveMode(boolean progressiveMode) {
        this.progressiveMode = progressiveMode;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public String getPaddingColor() {
        return paddingColor;
    }

    public void setPaddingColor(String paddingColor) {
        this.paddingColor = paddingColor != null ? paddingColor.replaceAll("[^0-9a-fA-F]", "") : null;
    }

    public Scalr.Method getScalingMethod() {
        return scalingMethod;
    }

    public void setScalingMethod(Scalr.Method scalingMethod) {
        this.scalingMethod = scalingMethod;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public int getHttpExpires() {
        return httpExpires;
    }

    public void setHttpExpires(int httpExpires) {
        this.httpExpires = httpExpires;
    }

    public void setUpScale(boolean upScale) {
        this.upScale = upScale;
    }

    public boolean isUpScale() {
        return upScale;
    }

    public AnimatedGIFMode getAnimatedGifMode() {
        return animatedGifMode;
    }

    public void setAnimatedGifMode(AnimatedGIFMode animatedGifMode) {
        this.animatedGifMode = animatedGifMode;
    }

    public ScalerParam copy() {
        return new ScalerParam().withMethod(getScalingMethod())
                .withPadding(getPadding())
                .withPaddingColor(getPaddingColor())
                .withQuality(getQuality())
                .withWidth(getTargetWidth())
                .withHeight(getTargetHeight());
    }

    @Override
    public String toString() {
        return "ScalerParam{" +
                "quality=" + quality +
                ", padding=" + padding +
                ", targetWidth=" + targetWidth +
                ", targetHeight=" + targetHeight +
                ", paddingColor='" + paddingColor + '\'' +
                ", scalingMethod=" + scalingMethod +
                ", progressiveMode=" + progressiveMode +
                ", httpExpires=" + httpExpires +
                ", upScale=" + upScale +
                ", animatedGifMode=" + animatedGifMode +
                '}';
    }

}
