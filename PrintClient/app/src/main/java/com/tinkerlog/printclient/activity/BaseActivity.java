package com.tinkerlog.printclient.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.tinkerlog.printclient.AppContext;
import com.tinkerlog.printclient.model.Printer;

/**
 * Created by alex on 24.08.15.
 */
public class BaseActivity extends FragmentActivity {

    protected AppContext appContext;
    protected Printer printer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = (AppContext)getApplicationContext();
        printer = appContext.getPrinter();
    }
}
