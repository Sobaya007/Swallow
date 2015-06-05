package com.trap.swallow.swallow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.trap.swallow.gcm.RegistrationIntentService;

public class MainActivity extends Activity {

    public static MainActivity singleton;

    GoogleApiClient client;

    public MainActivity() {
        singleton = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO 自動生成されたメソッド・スタブ
        super.onCreate(savedInstanceState);

        Log.d("Swallow", "aaa");

        setContentView(R.layout.activity_main);

        Intent intent = new Intent(getApplicationContext(), com.trap.swallow.login.LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
