package com.trap.swallow.server;

import android.content.Context;
import android.os.AsyncTask;

import com.trap.swallow.talk.MyUtils;

/**
 * Created by Sobaya on 2015/05/23.
 */
public abstract class ServerTask extends AsyncTask<Void, Double, Boolean> {

    String alertText;
    Context context;

    public abstract void doInSubThread() throws SwallowException;

    @Override
    protected final Boolean doInBackground(Void... params) {
        try {
            doInSubThread();
            return true;
        } catch (SwallowException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (!aBoolean)
            MyUtils.showShortToast(context, alertText);
    }

    public ServerTask(Context context, String alertText) {
        this.context = context;
        this.alertText = alertText;
        execute((Void)null);
    }

}
