package com.tinkerlog.printclient.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.tinkerlog.printclient.model.Command;

public class ResponseHandler extends Handler {

    private static final String TAG = "ResponseHandler";

    public static final int OK = 1;
    public static final int ERROR = 2;

    public ResponseHandler() {
        super();
    }

    public ResponseHandler(Callback callback) {
        super(callback);
    }

    public ResponseHandler(Looper looper) {
        super(looper);
    }

    public ResponseHandler(Looper looper, Callback callback) {
        super(looper, callback);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case OK:
                handleSuccess((Command)msg.obj);
                break;
            case ERROR:
                handleFailed((Command)msg.obj);
                break;
        }
    }

    public void handleFailed(Command cmd) {
        Log.e(TAG, "failed: " + cmd.getRequest());
    }

    public void handleSuccess(Command cmd) {
        Log.d(TAG, "success: " + cmd.getResponse());
    }

}
