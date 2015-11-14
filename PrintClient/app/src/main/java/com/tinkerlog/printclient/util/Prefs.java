package com.tinkerlog.printclient.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.tinkerlog.printclient.model.Print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class Prefs {

    private static final String TAG = "Prefs";
    private static final String PREFERENCES = "printclient";

    private static final String KEY_PRINT_PREFIX = "print_";
    private static final String KEY_ADDRESS = "address";

    private SharedPreferences prefs;

    public Prefs(Context context) {
        this.prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences() {
        return prefs;
    }

    public void storePrint(Print print) {
        Editor e = prefs.edit();
        e.putString(KEY_PRINT_PREFIX + print.creationDate, print.encode());
        e.commit();
    }

    public void deletePrint(Print print) {
        Editor e = prefs.edit();
        e.remove(KEY_PRINT_PREFIX + print.creationDate);
        e.commit();
    }

    public List<Print> loadPrints() {
        List<Print> prints = new ArrayList<Print>();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_PRINT_PREFIX)) {
                try {
                    prints.add(Print.decode(prefs.getString(key, null)));
                }
                catch (Exception e) {
                    Log.w(TAG, "parsing failed", e);
                }
            }
        }
        return prints;
    }

}
