package com.n11.imic;

public class ImageConverterException extends Exception {

    private static final long serialVersionUID = 907325212428930596L;

    private final String message;

    public ImageConverterException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ImageConverterException{ message= '" + message + "'}";
    }

    public String getMessage() {
        return message;
    }
}
