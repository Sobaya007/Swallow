package com.trap.swallow.gcm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.google.android.gms.iid.InstanceID;
import com.trap.swallow.swallow.R;

import java.io.IOException;

/**
 * Created by sobayaou on 2015/05/30.
 */
public class MyInstanceIDListenerService extends InstanceIDListenerService {

    public MyInstanceIDListenerService() {
        super();
    }

    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
