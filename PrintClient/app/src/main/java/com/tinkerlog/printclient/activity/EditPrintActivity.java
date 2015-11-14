package com.tinkerlog.printclient.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tinkerlog.printclient.Const;
import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Command;
import com.tinkerlog.printclient.model.Image;
import com.tinkerlog.printclient.model.ImageCommand;
import com.tinkerlog.printclient.model.Print;
import com.tinkerlog.printclient.util.ResponseHandler;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;


/**
 * Created by alex on 24.08.15.
 */
public class EditPrintActivity extends BaseActivity {

    private static final int ANI_DURATION = 300;
    private static final int REQ_SELECT_PICTURE = 1001;
    private static final int REQ_PROCESS_PICTURE = 1002;

    private static final String TAG = "EditPrintActivity";

    private ImageButton cameraBtn;
    private ImageButton collectionBtn;
    private ImageButton clearBtn;
    private View emptyContainer;
    private View deleteContainer;
    private ImageView pic0Img;
    private ImageView pic1Img;
    private ImageView pic2Img;
    private ImageView pic3Img;
    private EditText nameTxt;
    private Button processBtn;
    private Button printBtn;
    private Handler handler;
    private DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
    private DeleteCheck deleteCheck = new DeleteCheck();
    private Print currentPrint;
    private int selectedThumb;
    private boolean inPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        handler = new Handler();

        long printId = getIntent().getLongExtra(Const.KEY_PRINT_ID, -1);
        currentPrint = appContext.getPrint(printId);
        if (currentPrint == null) {
            currentPrint = new Print();
        }

        emptyContainer = findViewById(R.id.act_print_empty);
        deleteContainer = findViewById(R.id.act_print_delete_container);
        pic0Img = (ImageView)findViewById(R.id.act_print_image_0);
        pic0Img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPrint.isEmpty() ||
                        selectedThumb == 1 ||
                        (selectedThumb == 2 && currentPrint.pic03FileName == null)) {
                    return;
                }

                if (deleteContainer.getVisibility() == View.INVISIBLE) {
                    deleteContainer.setVisibility(View.VISIBLE);
                    deleteContainer.setTranslationY(deleteContainer.getHeight());
                    deleteContainer.animate()
                            .translationY(0F)
                            .setDuration(ANI_DURATION)
                            .setInterpolator(interpolator)
                            .start();
                }
                handler.removeCallbacks(deleteCheck);
                handler.postDelayed(deleteCheck, 2000);
            }
        });

        pic1Img = (ImageView)findViewById(R.id.act_print_image_1);
        pic1Img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedThumb = 0;
                updateUI();
            }
        });

        pic2Img = (ImageView)findViewById(R.id.act_print_image_2);
        pic2Img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedThumb = 1;
                updateUI();
            }
        });

        pic3Img = (ImageView)findViewById(R.id.act_print_image_3);
        pic3Img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedThumb = 2;
                updateUI();
            }
        });

        processBtn = (Button)findViewById(R.id.act_print_process_btn);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onProcess();
            }
        });
        printBtn = (Button)findViewById(R.id.act_print_print_btn);
        printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrint();
            }
        });
        clearBtn = (ImageButton)findViewById(R.id.act_print_clear_btn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClear();
            }
        });
        nameTxt = (EditText)findViewById(R.id.act_print_image_name);
        nameTxt.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction: " + event + ", action: " + actionId);
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    currentPrint.name = nameTxt.getText().toString();
                    appContext.storePrint(currentPrint);
                }
                return false;
            }
        });
        cameraBtn = (ImageButton)findViewById(R.id.act_print_camera);
        collectionBtn = (ImageButton)findViewById(R.id.act_print_collection);
        collectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGallery();
            }
        });

        selectedThumb = 0;
        updateUI();
    }

    private void updateUI() {

        nameTxt.setText(currentPrint.name);

        if (currentPrint.pic01FileName != null) {
            appContext.getImageCache().getBitmap(currentPrint.pic01FileName, pic1Img, this);
        }
        else {
            pic1Img.setImageResource(R.drawable.back);
        }
        if (currentPrint.pic02FileName != null) {
            appContext.getImageCache().getBitmap(currentPrint.pic02FileName, pic2Img, this);
        }
        else {
            pic2Img.setImageResource(R.drawable.back);
        }
        if (currentPrint.pic03FileName != null) {
            appContext.getImageCache().getBitmap(currentPrint.pic03FileName, pic3Img, this);
        }
        else {
            pic3Img.setImageResource(R.drawable.back);
        }

        pic1Img.setBackgroundColor(0x00000000);
        pic2Img.setBackgroundColor(0x00000000);
        pic3Img.setBackgroundColor(0x00000000);

        ImageView thumb = null;
        String src = null;
        switch (selectedThumb) {
            case 0:
                thumb = pic1Img;
                src = currentPrint.pic01FileName;
                break;
            case 1:
                thumb = pic2Img;
                src = currentPrint.pic02FileName;
                break;
            case 2:
                thumb = pic3Img;
                src = currentPrint.pic03FileName;
                break;
        }

        thumb.setBackgroundResource(R.drawable.thumb_back);

        if (src != null) {
            appContext.getImageCache().getBitmap(src, pic0Img, this);
            emptyContainer.setVisibility(View.INVISIBLE);
        }
        else {
            pic0Img.setImageResource(R.drawable.back);
            emptyContainer.setVisibility((selectedThumb == 1) ? View.INVISIBLE : View.VISIBLE);
        }

    }

    private class DeleteCheck implements Runnable {
        public void run() {
            deleteContainer.animate()
                    .translationY(deleteContainer.getHeight())
                    .setDuration(ANI_DURATION)
                    .setInterpolator(interpolator)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            deleteContainer.setVisibility(View.INVISIBLE);
                }
            }).start();
        }
    }

    private void onClear() {
        if (selectedThumb == 0) {
            currentPrint.clear();
        }
        else {
            currentPrint.pic03FileName = null;
        }
        appContext.storePrint(currentPrint);
        updateUI();
    }

    private void onProcess() {
        if (!currentPrint.isEmpty()) {
            Intent intent = new Intent(this, ProcessActivity.class);
            intent.putExtra(Const.KEY_PRINT_ID, currentPrint.creationDate);
            startActivityForResult(intent, REQ_PROCESS_PICTURE);
        }
    }

    private void onPrint() {
        Log.d(TAG, "----- onPrint");
        if (inPrint) {
            return;
        }
        if (currentPrint.isEmpty() || currentPrint.pic01FileName == null) {
            Toast.makeText(this, "Nothing to print ...", Toast.LENGTH_SHORT).show();
        }
        else if (!printer.isConnected()) {
            Toast.makeText(this, "Not connected ...", Toast.LENGTH_SHORT).show();
        }
        else {
            inPrint = true;
            Bitmap printBitmap = ((BitmapDrawable)pic2Img.getDrawable()).getBitmap();
            int width = printBitmap.getWidth();
            int height = printBitmap.getHeight();
            Log.d(TAG, "width: " + width + ", height: " + height);
            Image image = new Image(width, height);
            int[] data = new int[width * height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    data[x*height+y] = (printBitmap.getPixel(x, y) == 0xFF000000) ? 1 : 0;
                }
            }
            image.data = data;
            printer.send(new ImageCommand(image, new ResponseHandler() {
                @Override
                public void handleFailed(Command cmd) {
                    inPrint = false;
                    Toast.makeText(EditPrintActivity.this, "FAILED!", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void handleSuccess(Command cmd) {
                    inPrint = false;
                    Toast.makeText(EditPrintActivity.this, "Print sent!", Toast.LENGTH_SHORT).show();
                }
            } ));
        }
    }

    private void onGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_SELECT_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode);
        if (requestCode == REQ_SELECT_PICTURE && data != null && data.getData() != null) {
            safeImageFromGallery(data.getData());
        }
        updateUI();
    }

    private void safeImageFromGallery(Uri uri) {
        try {
            Log.d(TAG, "safeImageFromGallery: " + uri);
            Bitmap newBitmap = null;
            InputStream imageStream = getContentResolver().openInputStream(uri);
            Bitmap image = BitmapFactory.decodeStream(imageStream);
            int width = image.getWidth();
            int height = image.getHeight();
            Log.d(TAG, "orig: " + width + " * " + height);
            if (width > height) {  // landscape
                if (width > 1200) {
                    float scale = 1200f / width;
                    height = (int)(height * scale);
                    width = 1200;
                }
            }
            else {
                if (height > 1200) {
                    float scale = 1200f / height;
                    width = (int)(width * scale);
                    height = 1200;
                }
            }
            Log.d(TAG, "scaled: " + width + " * " + height);
            newBitmap = Bitmap.createScaledBitmap(image, width, height, true);

            pic0Img.setImageBitmap(newBitmap);
            pic0Img.setBackgroundColor(0xFF000000);

            emptyContainer.setVisibility(View.GONE);

            String currentFilename = null;
            if (height <= 400 || width <= 400) {
                currentFilename = appContext.getPictureDir().getAbsolutePath() + "/gallery_" + System.currentTimeMillis() + ".png";
                BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilename));
                newBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
            else {
                currentFilename = appContext.getPictureDir().getAbsolutePath() + "/gallery_" + System.currentTimeMillis() + ".jpg";
                BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(currentFilename));
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            }
            Log.d(TAG, "filename: " + currentFilename);

            if (selectedThumb == 0) {
                currentPrint.pic01FileName = currentFilename;
            }
            else {
                currentPrint.pic03FileName = currentFilename;
            }

            appContext.storePrint(currentPrint);
        }
        catch (Exception e) {
            Log.w(TAG, "failed", e);
        }
    }

}
