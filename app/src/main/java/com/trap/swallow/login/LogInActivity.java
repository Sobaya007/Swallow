package com.trap.swallow.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.trap.swallow.gcm.RegistrationIntentService;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.server.SwallowSecurity;
import com.trap.swallow.swallow.R;
import com.trap.swallow.talk.MyUtils;
import com.trap.swallow.talk.TalkActivity;

public class LogInActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//PreferenceからSwllowSecurityのSerialコードをロード
		String serial = MyUtils.sp.getString(MyUtils.SWALLOW_SECURITY_SERIALIZE_CODE, null);
		//Serialコードがあったなら
		if (serial != null) {
			SwallowSecurity security = null;
			try {
				//SwallowSecurityを取得してTalkActivityへ
				security = SwallowSecurity.deserialize(serial);
			} catch (SwallowException e) {
				e.printStackTrace();
			}
			final SwallowSecurity sec = security;
			if (sec != null) {
				final Swallow swallow = sec.getSwallow();
				new ServerTask(this, "") {
					@Override
					public void doInSubThread() throws SwallowException {
						swallow.modifyUser(null, null, null, null, null, null, null);
					}

					@Override
					protected void onPostExecute(Boolean aBoolean) {
						if (aBoolean) {
							toNext(swallow, sec);
						} else {
							init();
						}
					}
				};
			} else {
				init();
			}
		} else {
			init();
		}

		super.onCreate(savedInstanceState);
	}

	private final void init() {
		setContentView(R.layout.activity_login);

		final EditText userNameInput = (EditText)findViewById(R.id.userNameInput);
		final EditText passwordInput = (EditText)findViewById(R.id.passwordInput);
		//ログインボタンの設定
		Button button = (Button)findViewById(R.id.login_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog dialog = new ProgressDialog(LogInActivity.this);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage("読み込み中...");
				dialog.show();
				AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							//Serialコードがなかったら、サーバーにユーザー名とパスワードを送ってログイン

							SwallowSecurity sec;
							sec = new SwallowSecurity();
							sec.login(userNameInput.getText().toString(), passwordInput.getText().toString());
							//SerialコードをPreferenceに保存
							MyUtils.sp.edit().putString(MyUtils.SWALLOW_SECURITY_SERIALIZE_CODE, sec.serialize()).apply();
							//TalkActivityへ
							toNext(sec.getSwallow(), sec);
							return true;
						} catch (SwallowException e) {
							e.printStackTrace();
						}
						return false;
					}

					@Override
					protected void onPostExecute(Boolean b) {
						if (!b) {
							Toast.makeText(LogInActivity.this, "ログインできませんでした", Toast.LENGTH_SHORT);
						}
						dialog.dismiss();
					}
				};
				task.execute();
			}
		});

		Button registerButton = (Button)findViewById(R.id.register_button);
		registerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("http://swallow.trap.tokyotech.org/?mode=register");
				Intent i = new Intent(Intent.ACTION_VIEW,uri);
				startActivity(i);
			}
		});
	}

	private final void toNext(Swallow swallow, SwallowSecurity sec) {
		//SCMにSwallowSecurityを送る
		SCM.swallow = swallow;
		SCM.sec = sec;
		//IntentでTalkActivityへ
		Intent intent = new Intent(getApplicationContext(), TalkActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
}
