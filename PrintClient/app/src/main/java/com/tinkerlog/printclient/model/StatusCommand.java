package com.tinkerlog.printclient.model;

import com.tinkerlog.printclient.util.ResponseHandler;

public class StatusCommand extends Command {

    public int leftPos;
    public int headPos;
    public int rightPos;

    public StatusCommand(ResponseHandler handler) {
        super("stat", handler);
    }

    public void setResponse(String resp) {
        this.response = resp.trim();
        String[] tokens = response.split(" ");
        leftPos = Integer.parseInt(tokens[0]);
        rightPos = Integer.parseInt(tokens[1]);
        headPos = Integer.parseInt(tokens[2]);
    }

}
