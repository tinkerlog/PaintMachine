package com.tinkerlog.printclient.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tinkerlog.printclient.AppContext;
import com.tinkerlog.printclient.Const;
import com.tinkerlog.printclient.R;
import com.tinkerlog.printclient.model.Print;

import java.util.List;

/**
 * Created by alex on 24.08.15.
 */
public class PrintListFragment extends Fragment {

    private static final String TAG = "PrintListFrg";

    private ImageButton addBtn;
    private AppContext appContext;
    private TextView emptyTxt;
    private ListView printsList;
    private List<Print> prints;
    private PrintsAdapter adapter;

    public static PrintListFragment newFragment() {
        return new PrintListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        appContext = (AppContext)getActivity().getApplicationContext();
        prints = appContext.getPrints();
        View rootView = inflater.inflate(R.layout.frg_prints_list, container, false);
        emptyTxt = (TextView)rootView.findViewById(R.id.frg_prints_empty);
        printsList = (ListView)rootView.findViewById(R.id.frg_prints_list);
        printsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clickItem(view, position);
            }
        });
        adapter = new PrintsAdapter(getActivity(), prints);
        printsList.setAdapter(adapter);
        addBtn = (ImageButton)rootView.findViewById(R.id.frg_prints_add);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        prints = appContext.getPrints();
        updateUI();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void onShowDelete(int position) {
        Log.d(TAG, "delete: " + position);
        showDeleteDialog(position);
    }

    private void showDeleteDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("delete?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onDelete(position);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create().show();
    }

    private void onDelete(int position) {
        Print toDelete = prints.get(position);
        appContext.deletePrint(toDelete);
        adapter.notifyDataSetChanged();
    }

    private void add() {
        Print p = new Print();
        p.name = "Print " + (prints.size() + 1);
        appContext.storePrint(p);
        openPrint(p);
    }

    private void clickItem(View view, int position) {
        Log.d(TAG, "clickItem: " + position);
        Print p = prints.get(position);
        openPrint(p);
    }

    private void openPrint(Print print) {
        Intent intent = new Intent(getActivity(), EditPrintActivity.class);
        intent.putExtra(Const.KEY_PRINT_ID, print.creationDate);
        getActivity().startActivity(intent);
    }

    private void updateUI() {
        emptyTxt.setVisibility(prints != null && prints.size() > 0 ? View.GONE : View.INVISIBLE);
    }

    private class Holder {
        TextView name;
        ImageView image;
        ImageButton deleteBtn;
        DeleteListener deleteListener;
        int pos;
    }

    private class PrintsAdapter extends ArrayAdapter<Print> {

        private LayoutInflater inflater;

        public PrintsAdapter(Context context, List<Print> prints) {
            super(context, R.layout.list_item_print, prints);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return prints.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getView: " + position);
            Holder holder = null;
            if (convertView == null || convertView.getTag() == null) {
                View v = inflater.inflate(R.layout.list_item_print, parent, false);
                holder = new Holder();
                holder.name = (TextView)v.findViewById(R.id.act_print_image_name);
                holder.image = (ImageView)v.findViewById(R.id.list_image);
                holder.deleteListener = new DeleteListener();
                holder.deleteBtn = (ImageButton)v.findViewById(R.id.list_delete_btn);
                holder.deleteBtn.setFocusable(false);
                v.setTag(holder);
                convertView = v;
            }
            else {
                holder = (Holder)convertView.getTag();
            }
            holder.pos = position;
            holder.deleteListener.position = position;
            holder.deleteBtn.setOnClickListener(holder.deleteListener);

            Print p = getItem(position);
            holder.name.setText(p.name);
            if (p.pic01FileName != null) {
                appContext.getImageCache().getBitmap(p.pic01FileName, holder.image, getActivity());
            }
            else {
                holder.image.setImageResource(R.drawable.back);
            }
            return convertView;
        }
    }

    private class DeleteListener implements View.OnClickListener {
        public int position;
        @Override
        public void onClick(View v) {
            onShowDelete(position);
        }
    }

}
