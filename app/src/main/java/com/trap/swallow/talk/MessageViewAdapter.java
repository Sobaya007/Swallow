package com.trap.swallow.talk;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.trap.swallow.swallow.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sobayaou on 2015/06/02.
 */
public class MessageViewAdapter extends BaseAdapter {

    private static MessageViewAdapter singleton;
    private ArrayList<MessageView> messageViews = new ArrayList<>();
    private ListView parent;

    public MessageViewAdapter() {
        this.parent = (ListView)TalkActivity.singleton.findViewById(R.id.talk_scroll_view);
        singleton = this;
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
//        parent.addView(convertView);
        return convertView;
    }

    public final void add(MessageView mv) {
        messageViews.add(mv);
        notifyDataSetChanged();
    }

    public final void add(MessageView mv, int index) {
        messageViews.add(index, mv);
        notifyDataSetChanged();
    }

    public final static int getChildCount() {
        return singleton.messageViews.size();
    }

    public final MessageView getChildAt(int index) {
        return messageViews.get(index);
    }

    public final void removeAt(int index) {
        messageViews.remove(index);
        notifyDataSetChanged();
    }

    public final void clear() {
        messageViews.clear();
        notifyDataSetChanged();
    }
}
