package com.trap.swallow.talk;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.trap.swallow.info.TagInfoManager;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.swallow.MainActivity;
import com.trap.swallow.swallow.R;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;


public class MyUtils {

	public static final String SWALLOW_SECURITY_SERIALIZE_CODE = "SC";
	public static final String GCM_REGISTRATION_FLAG = "REGISTER_ID_SEND_FLAG2";
	public static final String YOJO_CHECK_KEY = "AmIYojo";
	public static final String MESSAGE_VIEW_KEY = "MV";
	public static final String HAS_READ_KEY = "READ";
	public static final String ENQUETE_ANSWER_KEY = "ANSWER";
	public static final String BACKGROUND_ENABLE_KEY = "BG";
	public static final String MESSAGE_KEY = "Message";

	public static Typeface yojoFont;
	public static SharedPreferences sp;
	static {
		if (MainActivity.singleton != null) {
			sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.singleton.getApplicationContext());
			yojoFont = Typeface.createFromAsset(MainActivity.singleton.getAssets(), "yojo.ttf");
		}
	}

	public static void staticInit(Context context) {
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		yojoFont = Typeface.createFromAsset(context.getAssets(), "yojo.ttf");
	}

	public static final void showShortToast(Context context, String text) {
		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
	}

	public static final void showShortToast(Context context, int resId) {
		Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
	}

	public static final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
	public static final int MP = LinearLayout.LayoutParams.MATCH_PARENT;

	public static final  LinearLayout.LayoutParams getLayoutparams(int width, int height) {
		return new LinearLayout.LayoutParams(width, height);
	}

	public static final  ListView.LayoutParams getLayoutparamsForListView(int width, int height) {
		return new ListView.LayoutParams(width, height);
	}

	public static final byte[] getFileByteArray(int fileId) {
		String key = "F" + fileId;
		String value = sp.getString(key, null);
		if (value == null) {
			//Preferenceにファイルデータがないとき
			try {
				byte[] buf = SCM.swallow.getFile(fileId);
				sp.edit().putString(key, new String(buf));
				sp.edit().apply();
				return buf;
			} catch (SwallowException e) {
				e.printStackTrace();
			}
		} else {
			//Preferenceにファイルデータがあるとき
			return value.getBytes();
		}
		return null;
	}

	public static final byte[] getThumbnailByteArray(int fileId, int width, int height) {
		String key = "Thumb:" + fileId;
		String value = sp.getString(key, null);
		if (value == null) {
			//Preferenceにファイルデータがないとき
			try {
				byte[] buf = SCM.swallow.getThumbnail(fileId, width, height);
				sp.edit().putString(key, new String(buf));
				sp.edit().apply();
				return buf;
			} catch (SwallowException e) {
				e.printStackTrace();
			}
		} else {
			//Preferenceにファイルデータがあるとき
			return value.getBytes();
		}
		return null;
	}

	public static final String getMimeType(String path) {
		//MIMEタイプを取得
		MimeTypeMap mt = MimeTypeMap.getSingleton();
		path = path.toLowerCase();
		String mimeType = mt.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
		return mimeType;
	}

	public static final Bitmap getImageFromPath(Resources res, String mimeType) {
		if (mimeType.startsWith("application/zip")) {
			return BitmapFactory.decodeResource(res, R.drawable.zip);
		} else if (mimeType.startsWith("application/pdf")) {
			return BitmapFactory.decodeResource(res, R.drawable.pdf);
		} else if (mimeType.startsWith("text/plain")) {
			return BitmapFactory.decodeResource(res, R.drawable.text);
		} else if (mimeType.startsWith("swallow/folder")) {
			return BitmapFactory.decodeResource(res, R.drawable.icon_folder);
		}
		return null;
	}

	/**
	 * ファイルを読み込み、その中身をバイト配列で取得する
	 *
	 * @param filePath 対象ファイルパス
	 * @return 読み込んだバイト配列
	 * @throws Exception ファイルが見つからない、アクセスできないときなど
	 */
	public static byte[] readFileToByte(String filePath) throws Exception {
		byte[] b = new byte[100];
		FileInputStream fis = new FileInputStream(filePath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (fis.read(b) > 0) {
			baos.write(b);
		}
		baos.close();
		fis.close();
		b = baos.toByteArray();

		return b;
	}

	public static void scrollDown() {
		TalkActivity.singleton.scrollView.smoothScrollToPosition(MessageViewAdapter.getChildCount()-1);
	}

	public static void scrollUp() {
		TalkActivity.singleton.scrollView.smoothScrollToPosition(0);
	}

	public static final boolean getReceivedFlag() {
		return ((CheckBox)TalkActivity.singleton.findViewById(R.id.checkReceivedBox)).isChecked();
	}

	public static final ProgressDialog createPorgressDialog() {
		ProgressDialog progressDialog = new ProgressDialog(TalkActivity.singleton);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("読み込み中...");
		return progressDialog;
	}

	public static float mod(float f1, float f2) {
		return f1 - f2 * (int)(f1 / f2);
	}
}
