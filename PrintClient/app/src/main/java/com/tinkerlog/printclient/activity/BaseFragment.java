package com.tinkerlog.printclient.activity;

import android.app.Activity;
import android.app.Fragment;

import com.tinkerlog.printclient.AppContext;
import com.tinkerlog.printclient.model.Printer;

/**
 * Created by alex on 27.08.15.
 */
public class BaseFragment extends Fragment {

    protected AppContext appContext;
    protected Printer printer;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = (AppContext)activity.getApplicationContext();
        printer = appContext.getPrinter();
    }

}
