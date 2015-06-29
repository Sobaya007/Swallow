package com.trap.swallow.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import android.util.Base64;

/*
 * ログイン関係
 */
@SuppressWarnings("serial")
public class SwallowSecurity implements Serializable {
	// セッションID
	private String sessionID;
	// AESの鍵(128bit)
	private byte[] key;
	// AESのIV(128bit)
	private byte[] iv;

	/*
	 * コンストラクタ
	 * 鍵やIVを生成しておく
	 */
	public SwallowSecurity() {
		key = SecureRandom.getSeed(16);
		iv = SecureRandom.getSeed(16);
	}
	/*
	 * APIアクションを実行するためのクラスSwallowを取得
	 */
	public Swallow getSwallow(){
		return new SwallowClient(this);
	}

	/*
	 * セッション永続化：シリアライズ
	 */
	public String serialize() throws SwallowException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(this);
			oos.close();
			return base64Enc(os.toByteArray());
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
	/*
	 * セッション永続化：シリアライズ
	 */
	public static SwallowSecurity deserialize(String serialized) throws SwallowException {
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(base64Dec(serialized)));
			return (SwallowSecurity) ois.readObject();
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	//サーバのアドレス
	private static final String REMOTE = "http://swallow.trap.tokyotech.org/";
	/*
	 * HTTP POST 実行
	 */
	private String httpPost(String endpoint, String data) throws SwallowException {
		try {
			URLConnection conn = new URL(REMOTE + endpoint).openConnection();
			conn.setDoOutput(true);

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			out.write(data);
			out.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder resp = new StringBuilder();

			String line;
			while((line = in.readLine()) != null){
				resp.append(line);
				resp.append("\r\n");
			}

			in.close();
			return resp.toString().trim();
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
	/*
	 * サーバから公開鍵を取得
	 */
	private byte[] fetchKey() throws SwallowException {
		try {
			Pattern p = Pattern.compile("(?m)^-+[^-]+-+$");
			Matcher m = p.matcher(httpPost("auth", "auth=start"));

			return base64Dec(m.replaceAll("").trim());
		} catch (SwallowException e) {
			throw e;
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
	/*
	 * ログイン処理
	 * 参考: https://gist.github.com/kazsw/1a6242f83da0cf61a84e
	 */
	public SwallowSecurity login(String email, String password) throws SwallowException {
		try {
			String param = "email=" + email + "&pass=" + password + "&key=" + base64Enc(key) + "&iv=" + base64Enc(iv);
			String[] resp = httpPost("auth", "auth=login&data=" + base64Enc(rsaEnc(fetchKey(), param.getBytes()))).split(": ");

			if("OK".equals(resp[0])){
				sessionID = resp[1].trim();
			}else{
				throw new SwallowException("Server responded with an error: " + resp[1].trim(), resp[1].trim(), null);
			}
		} catch (SwallowException e) {
			throw e;
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
		return this;
	}
	/*
	 * セッション破棄
	 * 参考: https://gist.github.com/kazsw/1a6242f83da0cf61a84e
	 */
	public SwallowSecurity logout() throws SwallowException {
		try {
			String param = "session=" + sessionID + "&key=" + base64Enc(key) + "&iv=" + base64Enc(iv);
			String[] resp = httpPost("auth", "auth=logout&data=" + base64Enc(rsaEnc(fetchKey(), param.getBytes()))).split(": ");

			if("NG".equals(resp[0])){
				throw new SwallowException("Server responded with an error: " + resp[1].trim(), resp[1].trim(), null);
			}
		} catch (SwallowException e) {
			throw e;
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
		return this;
	}
	/*
	 * リクエスト実行
	 * 参考: https://gist.github.com/kazsw/1a6242f83da0cf61a84e
	 */
	private byte[] request(String json, byte[] file) throws SwallowException {
		StringBuilder req = new StringBuilder();
		req.append("session=");
		req.append(sessionID);
		req.append("&request=");
		req.append(base64Enc(aesEnc(json.getBytes())));
		if(file != null && file.length > 0){
			req.append("&file=");
			req.append(base64Enc(aesEnc(file)));
		}

		return aesDec(base64Dec(httpPost("act", req.toString())));
	}
	public <T> T simpleRequest(String json, byte[] file, Class<T> respClass) throws SwallowException {
		Gson gson = new Gson();
		JsonObject jsonObj = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(request(json, file))), JsonObject.class);

		if(!jsonObj.get("succeeded").getAsBoolean()){
			String message = jsonObj.get("message").getAsString();
			throw new SwallowException("Server responded with an error: " + message, message, null);
		}
		return gson.fromJson(jsonObj.get("results").toString(), respClass);
	}
	public byte[] fileRequest(String json) throws SwallowException {
		return request(json, null);
	}

	/*
	 * バイト列をBase64文字列化
	 */
	private static String base64Enc(byte[] input) {
		return Base64.encodeToString(input, Base64.DEFAULT).trim();
	}
	/*
	 * Base64文字列をバイト列化
	 */
	private static byte[] base64Dec(String input) {
		return Base64.decode(input, Base64.DEFAULT);
	}
	/*
	 * RSA公開鍵で文字列を暗号化
	 */
	private byte[] rsaEnc(byte[] publicKey, byte[] plainText) throws SwallowException {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey)));
			return cipher.doFinal(plainText);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
	/*
	 * AESで暗号化
	 */
	private byte[] aesEnc(byte[] plainText) throws SwallowException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(plainText);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
	/*
	 * AESで復号化
	 */
	private byte[] aesDec(byte[] cipherText) throws SwallowException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			return cipher.doFinal(cipherText);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}
}