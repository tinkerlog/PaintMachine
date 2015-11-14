package com.tinkerlog.printclient.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.util.FontCache;

public class CustomTextView extends TextView {

    public CustomTextView(Context context) {
        super(context);
        initialize(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        String fontName = a.getString(R.styleable.CustomTextView_fontName);
        if (fontName != null) {
            Typeface tf = FontCache.getFont(context, fontName);
            setTypeface(tf);
        }
        a.recycle();
    }

}
