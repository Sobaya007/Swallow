package com.trap.swallow.server;

import java.io.Serializable;
import java.security.SecureRandom;

import android.util.Base64;

/*
 * ログイン関係
 */
@SuppressWarnings({ "unused", "serial" })
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
	 * ログイン処理
	 * 参考: https://gist.github.com/kazsw/1a6242f83da0cf61a84e
	 */
	public SwallowSecurity login(String userName, String password) {
		// FIXME: 未実装
		return this;
	}
	/*
	 * セッション破棄
	 * 参考: https://gist.github.com/kazsw/1a6242f83da0cf61a84e
	 */
	public SwallowSecurity logout(String userName, String password) {
		// FIXME: 未実装
		return this;
	}
	/*
	 * APIアクションを実行するためのクラスSwallowを取得
	 */
	public Swallow getSwallow(){
		// FIXME: テスト用
		return new SwallowTest();
		// return new SwallowImpl(sessionID, this);
	}

	/*
	 * バイト列をBase64文字列化
	 */
	private String base64Enc(byte[] input) {
		return Base64.encodeToString(input, Base64.DEFAULT);
	}
	/*
	 * Base64文字列をバイト列化
	 */
	private byte[] base64Dec(String input) {
		return Base64.decode(input, Base64.DEFAULT);
	}
	/*
	 * RSA公開鍵で文字列を暗号化
	 */
	private byte[] rsaEnc(byte[] publicKey, byte[] plainText) {
		// FIXME: 未実装
		return null;
	}
	/*
	 * AESで暗号化
	 */
	public byte[] aesEnc(byte[] plainText) {
		// FIXME: 未実装
		return null;
	}
	/*
	 * AESで復号化
	 */
	public byte[] aesDec(byte[] cipherText) {
		// FIXME: 未実装
		return null;
	}
}