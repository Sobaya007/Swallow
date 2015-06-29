package com.trap.swallow.talk;

import android.app.ProgressDialog;
import android.database.DataSetObserver;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.trap.swallow.server.SCM;
import com.trap.swallow.swallow.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sobayaou on 2015/06/02.
 */
public class MessageViewAdapter extends BaseAdapter {

    private static ArrayList<MessageView> messageViews = new ArrayList<>();
    private static ListView parent;
    public static ProgressDialog progressDialog;
    private static int preSize;

    public MessageViewAdapter() {
        this.parent = (ListView)TalkActivity.singleton.findViewById(R.id.talk_scroll_view);
        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MyUtils.scrollDown();
                            TalkActivity.singleton.scrollView.setSelection(getChildCount()-1);
                        }
                    }, 100);
                }
                preSize = getChildCount();
            }
        });
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
        return messageViews.size();
    }

    public static final MessageView getChildAt(int index) {
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

    public static final int indexOf(int postID) {
        for (int i = 0; i < messageViews.size(); i++) {
            if (messageViews.get(i).mInfo.getPostID() == postID) {
                return i;
            }
        }
        return -1;
    }

    public static void afterPrevAdd() {
        parent.setSelection(getChildCount() - preSize);
        preSize = getChildCount();
    }

    public static void refresh() {
        for (MessageView mv : messageViews) {
            mv.refreshOnServerThraed();
        }
    }
}
