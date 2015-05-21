package com.trap.swallow.talk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.trap.swallow.server.SCM;
import com.trap.swallow.swallow.R;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;


public class MyUtils {

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

	public static final byte[] getFileByteArray(int fileId, SharedPreferences sp) {
		String key = "F" + fileId;
		String value = sp.getString(key, null);
		if (value == null) {
			//Preferenceにファイルデータがないとき
			byte[] buf = SCM.scm.swallow.getFile(fileId);
			sp.edit().putString(key, new String(buf));
            sp.edit().apply();
			return buf;
		} else {
			//Preferenceにファイルデータがあるとき
			return value.getBytes();
		}
	}

	public static final byte[] getThumbnailByteArray(int fileId, int width, int height, SharedPreferences sp) {
		String key = "F" + fileId;
		String value = sp.getString(key, null);
		if (value == null) {
			//Preferenceにファイルデータがないとき
			byte[] buf = SCM.scm.swallow.getThumbnail(fileId, width, height);
			sp.edit().putString(key, new String(buf));
            sp.edit().apply();
			return buf;
		} else {
			//Preferenceにファイルデータがあるとき
			return value.getBytes();
		}
	}

	public static final String getMimeType(String path) {
		//MIMEタイプを取得
		MimeTypeMap mt = MimeTypeMap.getSingleton();
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
	    byte[] b = new byte[1];
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
}
