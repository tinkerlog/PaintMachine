package com.tinkerlog.printclient.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.tinkerlog.printclient.AppContext;
import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Printer;

public class MainActivity extends Activity {

    private static final int MENU_LIST = 0;
    private static final int MENU_MANUAL = 1;
    private static final int MENU_PRINTER = 2;

    private static final String TAG = "MainActivity";
    private Handler handler;
    private Switch modeSwitch;

    private Printer printer;
    private ControlFragment controlFragment;
    private PrinterFragment printFragment;
    private PrintListFragment printListFragment;
    private ListView drawer;
    private DrawerLayout drawerLayout;
    private View flash;
    private String[] menu;

    private AppContext appContext;
    private Callback printerCallback = new Callback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        appContext = (AppContext)getApplicationContext();

        printer = appContext.getPrinter();

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle("");

        menu = getResources().getStringArray(R.array.main_menu);
        flash = findViewById(R.id.act_main_flash);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawer = (ListView)findViewById(R.id.left_drawer);
        drawer.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, menu));
        drawer.setOnItemClickListener(new DrawerItemClickListener());

        selectItem(0);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!printer.isConnected()) {
            printer.addStatusCallback(printerCallback);
            printer.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (isFinishing()) {
            printer.removeStatusCallback(printerCallback);
            printer.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menu_connect) {
                item.setVisible(printer.getState() == Printer.State.CONNECTED);
            }
            else if (item.getItemId() == R.id.menu_error) {
                item.setVisible(printer.getState() == Printer.State.ERRORED);
            }
            else if (item.getItemId() == R.id.menu_disconnect) {
                item.setVisible(printer.getState() != Printer.State.CONNECTED && printer.getState() != Printer.State.ERRORED);
            }
        }
        return true;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            Log.d(TAG, "onItemClick: " + position);
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        Fragment frg = null;
        switch (position) {
            case MENU_LIST:
                if (printListFragment == null) {
                    printListFragment = PrintListFragment.newFragment();
                }
                frg = printListFragment;
                break;
            case MENU_MANUAL:
                if (controlFragment == null) {
                    controlFragment = ControlFragment.newFragment();
                }
                frg = controlFragment;
                break;
            case MENU_PRINTER:
                if (printFragment == null) {
                    printFragment = PrinterFragment.newFragment();
                }
                frg = printFragment;
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.act_main_container, frg).commit();

        drawer.setItemChecked(position, true);
        setTitle(menu[position]);
        drawerLayout.closeDrawer(drawer);
    }

//    public void doLeft0(View v) {
//        comThread.send("g 0 0 0");
//    }
//
//    public void doLeft50(View v) {
//        comThread.send("g 50 0 0");
//    }

    private class Callback implements Printer.StatusCallback {
        @Override
        public void onStatusChanged(final Printer.State state, final String message) {
            Log.d(TAG, "----- status: " + state.toString());
            final String msg;
            switch (state) {
                case CONNECTED:
                    msg = getString(R.string.toast_connected);
                    break;
                case DISCONNECTED:
                    msg = getString(R.string.toast_disconnected);
                    break;
                case ERRORED:
                    msg = getString(R.string.toast_error);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "starting re-connect ...");
                            printer.connect();
                        }
                    }, 10000);
                    break;
                default:
                    msg = null;
            }
            if (msg != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                        flash.setAlpha(1);
                        flash.animate().alpha(0).setDuration(200).start();
                        invalidateOptionsMenu();
                    }
                });
            }

        }
    }

}
