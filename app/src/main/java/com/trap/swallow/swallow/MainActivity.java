package com.trap.swallow.swallow;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.trap.swallow.gcm.RegistrationIntentService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

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

        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        String logPath = "log.txt";
        StringBuilder sb = new StringBuilder();
        try {
            InputStream in = openFileInput(logPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.e("ERROR_LOG", line);
                sb.append(line);
                sb.append('\n');
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //ログ出力ファイルを作成
        try {
            OutputStream out = openFileOutput(logPath, MODE_PRIVATE);
            PrintStream ps = new PrintStream(out);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(ps)));
//            writer.println(sb.toString());
//            System.setErr(ps);
            ps.close();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(getApplicationContext(), com.trap.swallow.login.LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
