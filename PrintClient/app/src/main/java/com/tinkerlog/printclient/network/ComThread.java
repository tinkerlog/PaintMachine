package com.tinkerlog.printclient.network;

import android.os.Message;
import android.util.Log;

import com.tinkerlog.printclient.model.Command;
import com.tinkerlog.printclient.util.ResponseHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ComThread extends Thread {

    public interface ComCallback {
        void closed();
        void failed(String errorMsg);
    }

    private static final String TAG = "ComThread";

    private static final String SERVER_IP = "192.168.4.1";
    private static final int SERVER_PORT = 81;
    private static final String MSG_PING = "ping";
    private static final int MAX_BUFFER = 1024;
    private static final int MAX_SOCKET_TIMEOUT = 1000;

    private BlockingQueue<Command> outQueue = new LinkedBlockingQueue<>(8);

    private PrintWriter out;
    private BufferedInputStream in;
    private boolean running = true;
    private byte[] inBuffer = new byte[MAX_BUFFER];
    private ComCallback callback;
    private long lastActive;

    public ComThread(ComCallback callback) {
        this.callback = callback;
    }

    public void shutDown() {
        running = false;
        interrupt();
    }

    @Override
    public synchronized void start() {
        super.start();
        new Thread(new TimeoutCheck()).start();
    }

    public void send(Command command) {
        try {
            outQueue.put(command);
        }
        catch (InterruptedException e) {
            Log.w(TAG, "failed", e);
        }
    }

    public void sendIfEmpty(Command command) {
        if (outQueue.isEmpty()) {
            Log.d(TAG, "----- isEmpty: " + outQueue.size());
            send(command);
        }
        else {
            Log.d(TAG, "----- not empty: skipping");
        }
    }

    public void run() {
        Socket socket = null;
        try {
            Thread.sleep(500);
            Log.d(TAG, "connecting ...");
            socket = new Socket();
            socket.setSoTimeout(MAX_SOCKET_TIMEOUT);
            socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 1000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedInputStream(socket.getInputStream());

            Log.d(TAG, "socket timeout: " + socket.getSoTimeout());
            while (running) {

                Command cmd = outQueue.poll(2000, TimeUnit.MILLISECONDS);
                if (!running) {
                    break;
                }

                if (cmd == null) {
                    Log.d(TAG, "ping ...");
                    cmd = new Command(MSG_PING, null);
                    // continue;
                }
                else {
                    Log.d(TAG, "sending: " + cmd.getRequest());
                }
                out.write(cmd.getRequest());
                out.println();
                out.flush();
                Thread.sleep(1);

                int bytesRead = readBuffer(in, inBuffer);
                if (bytesRead == -1) {
                    Log.d(TAG, "no response, socket closed!");
                    if (cmd.handler != null) {
                        cmd.handler.sendMessage(Message.obtain(cmd.handler, ResponseHandler.ERROR, cmd));
                    }
                    callback.failed("no response!");
                    break;
                }
                else if (bytesRead > 0) {
                    String result = new String(inBuffer, 0, bytesRead);
                    Log.d(TAG, "  result: " + result);
                    cmd.setResponse(result);
                    if (cmd.handler != null) {
                        cmd.handler.sendMessage(Message.obtain(cmd.handler, ResponseHandler.OK, cmd));
                    }
                }

                Thread.sleep(1);
            }
            Log.d(TAG, "going down");
        }
        catch (InterruptedException e) {
            Log.d(TAG, "interrupted, going down");
        }
        catch (Exception e) {
            Log.e(TAG, "failed", e);
            callback.failed(e.getMessage());
        }
        finally {
            try {
                socket.close();
            }
            catch (IOException e) {
                // ignore
            }
            callback.closed();
            running = false;
        }
    }

    private int readBuffer(BufferedInputStream in, byte[] buf) throws IOException, InterruptedException {
        int count = 0;
        int b;

        while (true) {
            b = in.read();
            if (b != -1) {
                lastActive = System.currentTimeMillis();
                buf[count++] = (byte)b;
            }
            if (b == 0x0A || b == -1) { // end on \n
                break;
            }
        }
        return count;
    }

    private class TimeoutCheck implements Runnable {
        private static final String TAG = "TimeoutCheck";
        public void run() {
            while (running) {
                long delta = System.currentTimeMillis() - lastActive;
                Log.d(TAG, "----- delta: " + delta);
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    // ignored
                }
            }
            Log.d(TAG, "going down");
        }
    }

}