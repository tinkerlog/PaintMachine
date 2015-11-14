package com.tinkerlog.printclient.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tinkerlog.printclient.Const;
import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Command;
import com.tinkerlog.printclient.model.Image;
import com.tinkerlog.printclient.model.ImageCommand;
import com.tinkerlog.printclient.model.StatusCommand;
import com.tinkerlog.printclient.util.ResponseHandler;

public class ControlFragment extends BaseFragment {

    private static final String TAG = "ControlFragment";

    private static final int LIMIT_Y = 40;
    private static final int LIMIT_X = 40;

    private View leftControl;
    private View rightControl;
    private View leftStick;
    private View rightStick;
    private View headControl;
    private View headStick;
    private TextView leftTxt;
    private TextView headTxt;
    private TextView rightTxt;
    private Button fireBtn;
    private Button statusBtn;
    private Button imageBtn;
    private Button goBtn;

    private int controlHeight;
    private int maxHeight;
    private int minHeight;
    private int rangeHeight;
    private int stickHeight;
    private int controlWidth;
    private int maxWidth;
    private int minWidth;
    private int rangeWidth;

    private float left;
    private float right;
    private float head;
    private boolean isFire;

    private boolean isGone;

    public static ControlFragment newFragment() {
        return new ControlFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frg_control, container, false);

        leftControl = v.findViewById(R.id.frg_control_left);
        leftStick = v.findViewById(R.id.frg_control_left_stick);
        rightControl = v.findViewById(R.id.frg_control_right);
        rightStick = v.findViewById(R.id.frg_control_right_stick);
        headControl = v.findViewById(R.id.frg_control_head);
        headStick = v.findViewById(R.id.frg_control_head_stick);
        fireBtn = (Button)v.findViewById(R.id.frg_control_fire_btn);
        statusBtn = (Button)v.findViewById(R.id.frg_control_state_btn);
        imageBtn = (Button)v.findViewById(R.id.frg_control_image_btn);
        goBtn = (Button)v.findViewById(R.id.frg_control_go_btn);
        leftTxt = (TextView)v.findViewById(R.id.frg_control_status_left);
        headTxt = (TextView)v.findViewById(R.id.frg_control_status_head);
        rightTxt = (TextView)v.findViewById(R.id.frg_control_status_right);

        leftControl.setOnTouchListener(new VerticalTouchListener(leftStick, new ValueUpdater() {
            @Override
            public void update(float value, boolean force) {
                left = map(value);
                onValueUpdate(force);
            }
        }));
        rightControl.setOnTouchListener(new VerticalTouchListener(rightStick, new ValueUpdater() {
            @Override
            public void update(float value, boolean force) {
                right = map(value);
                onValueUpdate(force);
            }
        }));
        headControl.setOnTouchListener(new HorizontalTouchListener(headStick, new ValueUpdater() {
            @Override
            public void update(float value, boolean force) {
                head = map(-value);
                onValueUpdate(force);
            }
        }));

        leftControl.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                controlHeight = bottom;
                minHeight = LIMIT_Y;
                maxHeight = controlHeight - LIMIT_Y;
                stickHeight = leftStick.getHeight();
                int range = (maxHeight - stickHeight) - minHeight;
                rangeHeight = range / 2;
            }
        });

        headControl.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                controlWidth = right - left;
                minWidth = LIMIT_X;
                maxWidth = controlWidth - LIMIT_X;
                stickHeight = leftStick.getHeight();
                int range = (maxWidth - stickHeight) - minWidth;
                rangeWidth = range / 2;
            }
        });

        fireBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isFire = true;
                    onValueUpdate(true);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isFire = false;
                    onValueUpdate(true);
                }
                return false;
            }
        });

        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printer.requestStatus(new ResponseHandler() {
                    @Override
                    public void handleSuccess(Command cmd) {
                        StatusCommand sc = (StatusCommand) cmd;
                        leftTxt.setText(Integer.toString(sc.leftPos));
                        rightTxt.setText(Integer.toString(sc.rightPos));
                        headTxt.setText(Integer.toString(sc.headPos));
                    }
                });
            }
        });

        imageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // final Image img = new Image(16, 20);
                // img.data = Const.INVADER1_DATA_16;
                // final Image img = new Image(20, 46);
                // img.data = Const.TEST_DATA_46;
                final Image img = new Image(8, 16);
                img.data = Const.TEST_DATA_16;
                printer.send(new ImageCommand(img, new ResponseHandler() {
                    @Override
                    public void handleSuccess(Command cmd) {
                        Log.d(TAG, "----- image success: " + cmd.getResponse());
                    }
                }));
            }
        });

        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String c = isGone ? "goto 0 0" : "goto 300 0";
                // String c = isGone ? "goto 0 0" : "goto 0 1500";
                printer.send(new Command(c, new ResponseHandler()));
                isGone = !isGone;
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    private void onValueUpdate(final boolean force) {
        // Log.d(TAG, String.format("left: %.2f right: %.2f head: %.2f", left, right, head));
        final int leftInt = (int) (left * 255);
        final int rightInt = (int) (right * 255);
        int headInt = (int) (head * 255);
        final String msg = String.format("move %d %d %d %d", leftInt, rightInt, headInt, (isFire ? 1 : 0));
        Log.d(TAG, "msg: " + msg);

        Command cmd = new Command(msg, new ResponseHandler() {
            @Override
            public void handleSuccess(Command command) {
                Log.d(TAG, "----- success: " + command.getResponse());
            }
        });

        if (force) {
            printer.send(cmd);
        }
        else {
            printer.sendIfEmpty(cmd);
        }
    }

    private float map(float x) {
        if (x > 0) {
            return ((float)Math.pow(10, x) - 1) / 9f;
        }
        else {
            return -((float)Math.pow(10, -x) - 1) / 9f;
        }
    }

    private interface ValueUpdater {
        void update(float value, boolean force);
    }

    private class VerticalTouchListener implements View.OnTouchListener {
        private int lastY;
        private int startY;
        private View stick;
        private ValueUpdater updater;
        public VerticalTouchListener(View stick, ValueUpdater updater) {
            this.stick = stick;
            this.updater = updater;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int y = (int)event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastY = y;
                    startY = (int)stick.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaY = y - lastY;
                    if (deltaY == 0) {
                        return false;
                    }
                    int yNew = startY + deltaY;
                    if (yNew < minHeight) {
                        deltaY = minHeight - startY;
                    }
                    else if (yNew > (maxHeight-stickHeight)) {
                        deltaY = (maxHeight-stickHeight) - startY;
                    }
                    stick.setTranslationY(deltaY);
                    float output = (int)stick.getY() - minHeight;
                    output = (output - rangeHeight) / (float)rangeHeight;
                    updater.update(-output, false);
                    return true;
                case MotionEvent.ACTION_UP:
                    stick.animate().translationY(0).setDuration(100).start();
                    updater.update(0f, true);
                    return true;
            }
            return false;
        }
    }

    private class HorizontalTouchListener implements View.OnTouchListener {
        private int lastX;
        private int startX;
        private View stick;
        private ValueUpdater updater;
        public HorizontalTouchListener(View stick, ValueUpdater updater) {
            this.stick = stick;
            this.updater = updater;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (int)event.getX();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    startX = (int)stick.getX();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = x - lastX;
                    if (deltaX == 0) {
                        return false;
                    }
                    int xNew = startX + deltaX;
                    if (xNew < minWidth) {
                        deltaX = minWidth - startX;
                    }
                    else if (xNew > (maxWidth-stickHeight)) {
                        deltaX = (maxWidth-stickHeight) - startX;
                    }
                    stick.setTranslationX(deltaX);
                    float output = (int)stick.getX() - minWidth;
                    output = (output - rangeWidth) / (float)rangeWidth;
                    updater.update(-output, false);
                    return true;
                case MotionEvent.ACTION_UP:
                    stick.animate().translationX(0).setDuration(100).start();
                    updater.update(0f, true);
                    return true;
            }
            return false;
        }
    }

}
