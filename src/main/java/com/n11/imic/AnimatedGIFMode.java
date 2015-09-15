package com.n11.imic;

import org.apache.commons.lang.StringUtils;

public enum AnimatedGIFMode {

    SCALEFIRSTFRAMEPNG("1"), PASSTHROUGH("2"), SCALEALLFRAMES("d");

    private String identifier;

    AnimatedGIFMode(String identifier) {
        this.identifier = identifier;
    }

    public static AnimatedGIFMode valueFrom(String agifmode) {
        for (AnimatedGIFMode mode : AnimatedGIFMode.values()) {
            if (StringUtils.equalsIgnoreCase(mode.getIdentifier(), agifmode)) {
                return mode;
            }
        }
        return SCALEALLFRAMES;
    }

    private String getIdentifier() {
        return identifier;
    }
}