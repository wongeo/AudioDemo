package com.feng.audiodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.feng.audiodemo.R;

import java.util.List;

public class FileAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Item> mFiles;

    public FileAdapter(Context context, List<Item> list) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFiles = list;
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final TextView text1, text2;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.simple_list_item_2, parent, false);
        } else {
            view = convertView;
        }
        text1 = view.findViewById(R.id.text1);
        text2 = view.findViewById(R.id.text2);

        final Item item = (Item) getItem(position);
        text1.setText(item.getName());
        text2.setText(item.getUri());
        return view;
    }
}
