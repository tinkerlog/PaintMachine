package com.tinkerlog.printclient.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Printer;

/**
 * Created by alex on 20.08.15.
 */
public class PrinterFragment extends BaseFragment {

    private Button connectBtn;
    private View dialogView;
    private TextView statusTxt;
    private Callback callback = new Callback();

    public static PrinterFragment newFragment() {
        return new PrinterFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frg_printer, container, false);
        dialogView = v.findViewById(R.id.frg_printer_dialog);

        if (printer.isConnected()) {
            dialogView.setVisibility(View.GONE);
        }

        statusTxt = (TextView)v.findViewById(R.id.frg_printer_status);
        connectBtn = (Button)v.findViewById(R.id.frg_printer_connect_btn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnect();
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        printer.addStatusCallback(callback);
    }

    @Override
    public void onPause() {
        super.onPause();
        printer.removeStatusCallback(callback);
    }

    private void onConnect() {
        connectBtn.setVisibility(View.GONE);
        printer.connect();
    }

    private class Callback implements Printer.StatusCallback {
        @Override
        public void onStatusChanged(final Printer.State state, final String message) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusTxt.setText(message);
                    if (state == Printer.State.CONNECTED) {
                        dialogView.setVisibility(View.GONE);
                        connectBtn.setVisibility(View.VISIBLE);
                    }
                    else {
                        dialogView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

}
