package com.trap.swallow.talk;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Created by sobayaou on 2015/06/02.
 */
public class MessageViewAdapter extends BaseAdapter {

    public ArrayList<MessageView> messageViews = new ArrayList<>();

    public MessageViewAdapter() {

    }

    @Override
    public int getCount() {
        return messageViews.size();
    }

    @Override
    public Object getItem(int position) {
        return messageViews.get(position);
    }

    @Override
    public long getItemId(int position) {
        return messageViews.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = messageViews.get(position);
        parent.addView(convertView);
        return convertView;
    }
}
