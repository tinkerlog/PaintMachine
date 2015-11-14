package com.tinkerlog.printclient;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.tinkerlog.printclient.model.Print;
import com.tinkerlog.printclient.model.Printer;
import com.tinkerlog.printclient.util.ImageCache;
import com.tinkerlog.printclient.util.Prefs;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by alex on 24.08.15.
 */
public class AppContext extends Application {

    private static final String TAG = "AppContext";

    private Prefs prefs;
    private File pictureDir;
    private ImageCache imageCache;

    private List<Print> prints;
    private Printer printer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "----- onCreate -----");

        printer = new Printer(this);

        prefs = new Prefs(this);
        prints = prefs.loadPrints();
        Collections.sort(prints);
        imageCache = new ImageCache();

        pictureDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), Const.PUBLIC_IMG_DIR);
        if (!pictureDir.mkdirs()) {
            Log.e(TAG, "picture dir not created");
        }
        Log.d(TAG, "pictureDir: " + pictureDir);
    }

    public List<Print> getPrints() {
        return prints;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void storePrint(Print print) {
        if (!prints.contains(print)) {
            prints.add(print);
        }
        Collections.sort(prints);
        prefs.storePrint(print);
    }

    public Print getPrint(long id) {
        for (Print p : prints) {
            if (p.creationDate == id) {
                return p;
            }
        }
        return null;
    }

    public void deletePrint(Print print) {
        prints.remove(print);
        prefs.deletePrint(print);
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    public File getPictureDir() {
        return pictureDir;
    }

}
