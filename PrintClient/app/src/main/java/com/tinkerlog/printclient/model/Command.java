package com.tinkerlog.printclient.model;

import com.tinkerlog.printclient.util.ResponseHandler;

public class Command {

    protected String request;
    protected String response;
    public ResponseHandler handler;

    public Command(String request, ResponseHandler handler) {
        this.request = request;
        this.handler = handler;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public String getRequest() {
        return request;
    }

    public byte[] getPayload() {
        return request.getBytes();
    }
}
