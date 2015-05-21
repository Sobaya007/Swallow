package com.trap.swallow.login;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.trap.swallow.swallow.R;
import com.trap.swallow.server.SwallowSecurity;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.SwallowTest;
import com.trap.swallow.talk.TalkActivity;

public class LogInActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_login);

		final EditText userNameInput = (EditText)findViewById(R.id.userNameInput);
		final EditText passwordInput = (EditText)findViewById(R.id.passwordInput);

		//ログインボタンの設定
		Button button = (Button)findViewById(R.id.login_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SwallowSecurity sec = new SwallowSecurity();
				sec.login(userNameInput.getText().toString(), passwordInput.getText().toString());

				try {
					/*
					 * セッション情報を出力
					 */
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos;
					oos = new ObjectOutputStream(baos);
					oos.writeObject(sec);
					byte[] session = baos.toByteArray();
					// ↑ コレを保存しておけばいい

					/*
					 * セッション情報を復元
					 */
					ByteArrayInputStream bais = new ByteArrayInputStream(session);
					ObjectInputStream ois = new ObjectInputStream(bais);
					sec = (SwallowSecurity) ois.readObject();

					SCM.scm = new SCM(new SwallowTest(), session);
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}

				Context c = getApplicationContext();

				Class<TalkActivity> cl = TalkActivity.class;

				Intent intent = new Intent(c, cl);
			    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		});
		super.onCreate(savedInstanceState);
	}
}
