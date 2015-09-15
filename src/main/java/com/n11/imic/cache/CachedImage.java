package com.n11.imic.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CachedImage implements Serializable {

    private static final long serialVersionUID = -2122643369187856169L;

    private final byte[] imageData;

    private final Map<String, String> httpHeaders = new HashMap<String, String>();

    public CachedImage(byte[] imageData, Map<String, String> httpHeaders) {
        this.imageData = imageData;
        this.httpHeaders.putAll(httpHeaders);
    }

    public int getImageDataLength() {
        return getImageData().length;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }
}
