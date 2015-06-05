package com.trap.swallow.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.gcm.GcmListenerService;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.server.SwallowImpl;
import com.trap.swallow.server.SwallowSecurity;
import com.trap.swallow.swallow.MainActivity;
import com.trap.swallow.swallow.R;
import com.trap.swallow.talk.MyUtils;
import com.trap.swallow.talk.TalkActivity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static android.support.v4.app.NotificationCompat.*;

/**
 * Created by sobayaou on 2015/05/30.
 */
public class MyGcmListenerService extends GcmListenerService {

    public static final String TAG = "SwallowTag";
    public static final int REQUEST_CODE_MAIN_ACTIVITY = 0;

    public MyGcmListenerService() {
        super();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("PostID");
        sendNotification(message);
        if (TalkActivity.singleton != null) {
            new ServerTask(TalkActivity.singleton, "更新に失敗しました") {
                @Override
                public void doInSubThread() throws SwallowException {
                    TalkActivity.singleton.tvManager.loadNextMessage();
                }
            };
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
    }

    @Override
    public void onSendError(String msgId, String error) {
        super.onSendError(msgId, error);
    }

    private final void sendNotification(String message) {

        MyUtils.staticInit(getApplicationContext());

        SwallowImpl swallow = null;
        String serial = MyUtils.sp.getString(MyUtils.SWALLOW_SECURITY_SERIALIZE_CODE, null);
        //Serialコードがあったなら
        if (serial != null) {
            try {
                //SwallowSecurityを取得してTalkActivityへ
                SwallowSecurity sec = SwallowSecurity.deserialize(serial);
                swallow = new SwallowImpl(sec);
            } catch (SwallowException e) {
                e.printStackTrace();
            }
        }
        if (swallow == null) return;

        Swallow.Message mInfo = null;
        try {
            mInfo = swallow.findMessage(null, null, null, null, new Integer[]{Integer.parseInt(message)}, null, null, null, null, null, null, null, null)[0];
        } catch (SwallowException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        String notifyTagString = MyUtils.sp.getString(MyUtils.NOTIFY_TAG_KEY, null);
        int myUserID = MyUtils.sp.getInt(MyUtils.MY_USER_ID_KEY, -1);
        if (notifyTagString != null
                && myUserID != -1
                && mInfo.getUserID() != myUserID
                ) {
            String[] tag = notifyTagString.split(",");
            boolean flag = false;
            for (String t : tag) {
                if (t.length() > 0) {
                    int i = Integer.parseInt(t);
                    for (int ID : mInfo.getTagID()) {
                        if (i == ID) {
                            flag = true;
                            break;
                        }
                    }
                }
            }
            if (flag) {
                Builder builder = new Builder(
                        getApplicationContext());
                if (mInfo != null) {
                    String userName = null;
                    Bitmap userThumb = null;
                    try {
                        UserInfo uInfo = null;
                        Swallow.User[] uInfos = swallow.findUser(null, null, null, null, new Integer[]{mInfo.getUserID()}, null, null, null);
                        if (uInfos.length > 0)
                            uInfo = new UserInfo(uInfos[0]);
                        if (uInfo != null) {
                            userName = uInfo.user.getUserName();
                            userThumb = uInfo.profileImage;
                        } else {
                            userName = "unknown";
                            userThumb = null;
                        }
                    } catch (SwallowException e) {
                        e.printStackTrace();
                    }
                    builder.setLargeIcon(userThumb);
                    builder.setTicker(userName + ":" + mInfo.getMessage());
                    builder.setContentText(userName + ":" + mInfo.getMessage());
                } else {
                    builder.setTicker(getString(R.string.notification_message_not_found_text));
                }
                builder.setContentTitle(getString(R.string.app_name));
                builder.setSmallIcon(R.drawable.small_ic_launcher);
                builder.setWhen(System.currentTimeMillis());
                builder.setAutoCancel(true);
                builder.setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0));
                builder.setColor(0xff005C92); //(0x005C92
                builder.setVibrate(new long[]{1000, 100, 250, 100, 100, 100, 250, 100, 100, 700}); //にっこにっこにー
                NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
                manager.notify(0, builder.build());
            }
        }
    }
}
