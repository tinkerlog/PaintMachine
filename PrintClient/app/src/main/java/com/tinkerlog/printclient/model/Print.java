package com.tinkerlog.printclient.model;

import android.util.Log;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.Scanner;

/**
 * Created by alex on 24.08.15.
 */
public class Print implements Comparable<Print> {

    private static final String TAG = "Print";

    private static final String ENC_STRING = "%d %s %s %s %s";
    private static final String DELIMITER = " ";

    public int id;
    public long creationDate;
    public String name;
    public String pic01FileName;
    public String pic02FileName;
    public String pic03FileName;

    public Print() {
        creationDate = System.currentTimeMillis();
    }

    public void clear() {
        pic01FileName = null;
        pic02FileName = null;
        pic03FileName = null;
    }

    public boolean isEmpty() {
        return pic01FileName == null;
    }

    public static Print decode(String src) {
        Log.d(TAG, "decoding: " + src);
        Print p = new Print();
        Scanner scanner = new Scanner(src).useDelimiter(DELIMITER);
        p.creationDate = scanner.nextLong();
        p.name = URLDecoder.decode(scanner.next());
        p.pic01FileName = scanString(scanner);
        p.pic02FileName = scanString(scanner);
        p.pic03FileName = scanString(scanner);
        return p;
    }

    public String encode() {
        String s = String.format(ENC_STRING,
                creationDate,
                URLEncoder.encode(name),
                pic01FileName,
                pic02FileName,
                pic03FileName
                );
        Log.d(TAG, "encoded: " + s);
        return s;
    }

    private static String scanString(Scanner scanner) {
        String s = scanner.next();
        if (s == null || s.equals("null")) {
            return null;
        }
        return s;
    }

    @Override
    public int compareTo(Print another) {
        return (creationDate > another.creationDate) ? -1 :
                (creationDate == another.creationDate) ? 0 : 1;
    }

    public String toString() {
        return "Print " + creationDate + ", " + name;
    }

}
