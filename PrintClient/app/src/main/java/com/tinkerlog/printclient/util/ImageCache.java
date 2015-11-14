package com.tinkerlog.printclient.util;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.tinkerlog.printclient.Const;

@SuppressWarnings("serial")
public class ImageCache {

    private static final String TAG = "ImageCache";
    private static final int KEEP_ALIVE_TIME = 1;
    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 3;


    private ThreadPoolExecutor executor;
    private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();

    private Map<String, Bitmap> imageCache = new LinkedHashMap<String, Bitmap>(Const.MAX_CACHED_IMAGES, .75F, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
            return size() > Const.MAX_CACHED_IMAGES;
        }
    };

    public ImageCache() {
        executor = new ThreadPoolExecutor(MIN_THREADS, MAX_THREADS, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workQueue);
    }

    public void getBitmap(final String filename, final ImageView img, final Activity activity) {
        getBitmap(filename, img, activity, null);
    }

    public void getBitmap(final String filename, final ImageView img, final Activity activity, final Runnable onDone) {
        Bitmap bmp = imageCache.get(filename);
        if (bmp != null) {
            img.setImageBitmap(bmp);
        }
        else {
            img.setImageBitmap(null);
            executor.execute(new Runnable() {
                public void run() {
                    final Bitmap bmp = BitmapFactory.decodeFile(filename);
                    imageCache.put(filename, bmp);
                    Log.d(TAG, "image loaded");
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            img.setImageBitmap(bmp);
                            img.setAlpha(0.0f);
                            img.animate().alpha(1.0f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (onDone != null) {
                                        onDone.run();
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    public Bitmap getBitmap(String filename) {
        Bitmap bmp = imageCache.get(filename);
        if (bmp == null) {
            bmp = BitmapFactory.decodeFile(filename);
            imageCache.put(filename, bmp);
        }
        return bmp;
    }


    public interface ImageSink {
        public void setImageBitmap(Bitmap bmp);
    }

    private class ImageViewSink implements ImageSink {
        private ImageView imageView;
        public ImageViewSink(ImageView imageView) {
            this.imageView = imageView;
        }
        public void setImageBitmap(Bitmap bmp) {
            imageView.setImageBitmap(bmp);
        }
    }

}
