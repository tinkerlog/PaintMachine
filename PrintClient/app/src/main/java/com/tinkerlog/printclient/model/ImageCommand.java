package com.tinkerlog.printclient.model;

import com.tinkerlog.printclient.util.ResponseHandler;

public class ImageCommand extends Command {

    public ImageCommand(Image image, ResponseHandler handler) {
        super(null, handler);
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("imag ")
                .append(image.width)
                .append(" ")
                .append(image.height)
                .append(" ");

        for (int i = 0; i < image.data.length; i++) {
            sbuf.append(image.data[i]).append(" ");
        }
        request = sbuf.toString();
    }

}
