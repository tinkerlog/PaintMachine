package com.tinkerlog.printclient.model;

public class Image {

    // 40 pixel height
    // => 8 bytes per column

    public int width;
    public int height;

    public int[] data;

    public Image(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
