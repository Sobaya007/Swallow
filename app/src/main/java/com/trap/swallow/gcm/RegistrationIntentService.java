package com.trap.swallow.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.swallow.R;

import java.io.IOException;

/**
 * Created by sobayaou on 2015/05/30.
 */
public class RegistrationIntentService extends IntentService {

    public static final String TAG = "SwallowTag";
    private static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    public RegistrationIntentService(String name) {
        super(name);
    }

    public RegistrationIntentService() {
        super("RegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Initially this call goes out to the network to retrieve the token, subsequent calls
// are local.
        InstanceID instanceID = InstanceID.getInstance(this);
        String token = null;
        try {
            token = instanceID.getToken("472382951498",
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "GCM Registration Token: " + token);

// TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(token);

// Subscribe to topic channels
        subscribeTopics(token);

// You should store a boolean that indicates whether the generated token has been
// sent to your server. If the boolean is false, send the token to your server,
// otherwise your server should have already received the token.

//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        sharedPreferences.edit().putBoolean(SENT_TOKEN_TO_SERVER, true).apply();
    }

    private final void sendRegistrationToServer(String token) {
        Log.i("TAG", token);
        try {
            SCM.scm.swallow.modifyUser(null, null, null, null, null, null, null, token, null, null, null);
        } catch (SwallowException e) {
            e.printStackTrace();
        }
    }

    private final void subscribeTopics(String token) {

    }
}
