package com.tinkerlog.printclient.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.tinkerlog.printclient.Const;
import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Print;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProcessActivity extends BaseActivity {

    private static final String TAG = "ProcessActivity";

    private enum Filter {
        RED,
        GREEN,
        BLUE,
        BW;
        public static Filter getFilter(int value) {
            switch (value) {
                case 0: return RED;
                case 1: return GREEN;
                case 2: return BLUE;
                case 3: return BW;
            }
            return null;
        }
    };

    private Print currentPrint;

    private Button doneBtn;
    private ImageView processImageView;
    private ImageView srcImageView;
    private SeekBar seekBar;
    private ToggleButton[] filterBtns = new ToggleButton[4];
    private ToggleButton invertBtn;
    private FilterOnCheckedListener filterListener = new FilterOnCheckedListener();
    private Filter filter;
    private int threshold;
    private boolean isInverted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_process);

        processImageView = (ImageView)findViewById(R.id.act_process_image);
        srcImageView = (ImageView)findViewById(R.id.act_process_src_image);
        seekBar = (SeekBar)findViewById(R.id.act_process_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onSeek(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        filterBtns[0] = (ToggleButton)findViewById(R.id.act_process_filter_red);
        filterBtns[1] = (ToggleButton)findViewById(R.id.act_process_filter_green);
        filterBtns[2] = (ToggleButton)findViewById(R.id.act_process_filter_blue);
        filterBtns[3] = (ToggleButton)findViewById(R.id.act_process_filter_bw);
        for (ToggleButton t : filterBtns) {
            t.setOnCheckedChangeListener(filterListener);
        }

        invertBtn = (ToggleButton)findViewById(R.id.act_process_invert);
        invertBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isInverted = isChecked;
                onProcess(threshold);
            }
        });

        doneBtn = (Button)findViewById(R.id.act_process_done_btn);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
            }
        });

        long printId = getIntent().getLongExtra(Const.KEY_PRINT_ID, -1);
        currentPrint = appContext.getPrint(printId);

        appContext.getImageCache().getBitmap(currentPrint.pic01FileName, processImageView, this);
        appContext.getImageCache().getBitmap(currentPrint.pic01FileName, srcImageView, this);

        filter = Filter.BW;
        filterBtns[3].setChecked(true);
        threshold = 128;
    }

    @Override
    protected void onResume() {
        super.onResume();
        onProcess(threshold);
    }

    private void onDone() {
        save();
        finish();
    }

    private void onSeek(int progress) {
        threshold = progress;
        onProcess(threshold);
    }

    private void save() {
        Bitmap printBitmap = ((BitmapDrawable)processImageView.getDrawable()).getBitmap();
        try {
            String printFilename = appContext.getPictureDir().getAbsolutePath() + "/gallery_" + System.currentTimeMillis() + ".png";
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(printFilename));
            printBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.d(TAG, "filename: " + printFilename);
            currentPrint.pic02FileName = printFilename;
            appContext.storePrint(currentPrint);
        }
        catch (IOException e) {
            Log.d(TAG, "failed", e);
        }
    }

    private void onProcess(int threshold) {
        Bitmap srcBitmap = appContext.getImageCache().getBitmap(currentPrint.pic01FileName);
        float scale = (float) Const.MAX_HEIGHT_PIXEL / srcBitmap.getHeight();
        int width = (int)(srcBitmap.getWidth() * scale);
        int height = (int)(srcBitmap.getHeight() * scale);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, width, height, true);
        if (scaledBitmap == srcBitmap) {
            scaledBitmap = srcBitmap.copy(srcBitmap.getConfig(), true);
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = scaledBitmap.getPixel(x, y);
                int newPixel = 0;
                switch (filter) {
                    case RED:
                        newPixel = (Color.red(pixel) > threshold) ? 0xFFFFFFFF : 0xFF000000;
                        break;
                    case GREEN:
                        newPixel = (Color.green(pixel) > threshold) ? 0xFFFFFFFF : 0xFF000000;
                        break;
                    case BLUE:
                        newPixel = (Color.blue(pixel) > threshold) ? 0xFFFFFFFF : 0xFF000000;
                        break;
                    case BW:
                        int bw = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                        newPixel = (bw > threshold) ? 0xFFFFFFFF : 0xFF000000;
                        break;
                }
                if (isInverted) {
                    newPixel = (newPixel == 0xFFFFFFFF) ? 0xFF000000 : 0xFFFFFFFF;
                }
                scaledBitmap.setPixel(x, y, newPixel);
            }
        }
        processImageView.setImageBitmap(scaledBitmap);
    }

    private class FilterOnCheckedListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                for (int i = 0; i < 4; i++) {
                    ToggleButton t = filterBtns[i];
                    if (buttonView != t) {
                        t.setChecked(false);
                        t.setEnabled(true);
                    }
                    else {
                        filter = Filter.getFilter(i);
                        onProcess(threshold);
                        t.setEnabled(false);
                    }
                }
            }
        }
    }

}
