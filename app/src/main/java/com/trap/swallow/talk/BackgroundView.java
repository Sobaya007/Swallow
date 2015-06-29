//package com.example.talk;
//
//import java.util.ArrayList;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.view.MotionEvent;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//
//public class BackgroundView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener{
//
//	private static final int BALL_RADIUS = 20;
//
//	private int ww, wh;
//	Paint p = new Paint();
//	int bgColor;
//	SurfaceHolder holder;
//
//	private Ball[] balls = new Ball[200];
//	private ObjectForSweep[] list = new ObjectForSweep[balls.length*2];
//	private ArrayList<ObjectForSweep>[] bucket = new ArrayList[10];
//	private ArrayList<ObjectForSweep>[] bucket2 = new ArrayList[10];
//	private ArrayList<CollisionInfo> collisionList = new ArrayList<CollisionInfo>();
//	MotionEvent e;
//	float tx, ty;
//	float gx, gy;
//
//	private boolean startFlag = false;
//
//	public BackgroundView(final Context context) {
//		super(context);
//		getHolder().addCallback(this);
//		bgColor = Color.rgb(153, 217, 234);
//
//		for (int i = 0; i < list.length; i++)
//			list[i] = new ObjectForSweep();
//		for (int i = 0; i < bucket.length; i++)
//			bucket[i] = new ArrayList<BackgroundView.ObjectForSweep>();
//		for (int i = 0; i < bucket2.length; i++)
//			bucket2[i] = new ArrayList<BackgroundView.ObjectForSweep>();
//	}
//
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		// TODO 自動生成されたメソッド・スタブ
//		e = event;
//		performClick();
//		return super.onTouchEvent(event);
//	}
//
//	@Override
//	public boolean performClick() {
//		// TODO 自動生成されたメソッド・スタブ
//		return super.performClick();
//	}
//
//	@Override
//	public void surfaceChanged(SurfaceHolder holder, int format, int width,
//			int height) {
//		// TODO 自動生成されたメソッド・スタブ
//		ww = width;
//		wh = height;
//		p.setColor(Color.WHITE);
//
//		if (startFlag == false) {
//			for (int i = 0; i < balls.length; i++)
//				balls[i] = new Ball();
//			startFlag = true;
//			Timer t = new Timer();
//			t.schedule(new Task(), 0, 16);
//		}
//	}
//
//	@Override
//	public void surfaceCreated(SurfaceHolder holder) {
//		// TODO 自動生成されたメソッド・スタブ
//		this.holder = holder;
//	}
//
//	@Override
//	public void surfaceDestroyed(SurfaceHolder holder) {
//		// TODO 自動生成されたメソッド・スタブ
//		holder = null;
//	}
//
//	private final void touchPhase() {
//		if (e != null) {
//			final int FINGER_RADIUS = 100;
//
//			switch (e.getAction()) {
//			case MotionEvent.ACTION_MOVE:
//				if (tx == -1) {
//					tx = e.getX();
//					ty = e.getY();
//				} else {
//					float vx = e.getX() - tx;
//					float vy = e.getY() - ty;
//					for (Ball b : balls) {
//						float dx = b.x - e.getX();
//						float dy = b.y - e.getY();
//						if (dx * dx + dy * dy < FINGER_RADIUS * FINGER_RADIUS) {
//							final float k = 0.5f;
//							b.vx += k * vx;
//							b.vy += k * vy;
//						}
//					}
//					tx = e.getX();
//					ty = e.getY();
//				}
//				break;
//			case MotionEvent.ACTION_UP:
//				e = null;
//				break;
//			}
//		} else {
//			tx = -1;
//		}
//	}
//
//	private final void sortPhase() {
//		//リストに値を代入
//		for (int i = 0; i < balls.length; i++) {
//			list[i*2].set(balls[i].x-BALL_RADIUS, balls[i], true);
//			list[i*2+1].set(balls[i].x+BALL_RADIUS, balls[i], false);
//		}
//		//ソート
//
//		//バケツを掃除
//		for (int i = 0; i < bucket.length; i++)
//			bucket[i].clear();
//		for (int i = 0; i < bucket2.length; i++)
//			bucket2[i].clear();
//
//		//１の位でソート
//		for (ObjectForSweep obj : list) {
//			bucket[obj.value % 10].add(obj);
//		}
//
//		//10の位でソート
//		for (ArrayList<ObjectForSweep> objects : bucket) {
//			for (ObjectForSweep obj : objects) {
//				bucket2[(obj.value / 10) % 10].add(obj);
//			}
//		}
//
//		//もっかい掃除
//		for (int i = 0; i < bucket.length; i++)
//			bucket[i].clear();
//
//		//100の位でソート
//		for (ArrayList<ObjectForSweep> objects : bucket2) {
//			for (ObjectForSweep obj : objects) {
//				bucket[(obj.value / 100) % 10].add(obj);
//			}
//		}
//
//		//もっかい掃除
//		for (int i = 0; i < bucket2.length; i++)
//			bucket2[i].clear();
//
//		//1000の位でソート
//		int count = 0;
//		for (ArrayList<ObjectForSweep> objects : bucket) {
//			for (ObjectForSweep obj : objects) {
//				list[count++] = obj;
//			}
//		}
//	}
//
//	private final void collisionPhase() {
//		collisionList.clear();
//
//		final double e = 0.5; //跳ね返り係数
//		final double k = 0.5; //ばね係数
//
//		double dx, dy, d2, d, c, vx, vy, nx, ny, dot, s;
//		Ball b1, b2;
//		//ボールVSボール
//		//1軸スイープ&プルーンにて衝突を検出
//		for (int i = 0; i < list.length; i++) {
//			if (!list[i].isMinValue) continue;
//			for (int j = i+1; j < list.length; j++) {
//				if (list[i].b == list[j].b) break;
//				if (!list[j].isMinValue) continue;
//				b1 = list[i].b; b2 = list[j].b;
//				dx = b1.x - b2.x;
//				dy = b1.y - b2.y;
//				d2 = dx * dx + dy * dy;
//				if (d2 < BALL_RADIUS * BALL_RADIUS * 4) {
//					d = Math.sqrt(d2); //距離
//					if (d == 0) {
//						nx = 1; ny = 0;
//					} else {
//						nx =dx / d; ny = dy / d; //法線
//					}
//					s = BALL_RADIUS*2-d; //めりこみ(正)
//					collisionList.add(new CollisionInfo(b1, b2, nx, ny, s));
//				}
//			}
//		}
//		//ボールVS壁
//		for  (Ball b : balls) {
//			//左の壁
//			if (b.x - BALL_RADIUS < 0) {
//				collisionList.add(new CollisionInfo(b, null, 1, 0, BALL_RADIUS - b.x));
//			}
//			//右の壁
//			if (b.x + BALL_RADIUS > ww) {
//				collisionList.add(new CollisionInfo(b, null, -1, 0, b.x + BALL_RADIUS - ww));
//			}
//			//上の壁
//			if (b.y - BALL_RADIUS < 0) {
//				collisionList.add(new CollisionInfo(b, null, 0, 1, BALL_RADIUS - b.y));
//			}
//			//下の壁
//			if (b.y + BALL_RADIUS > wh - 200) {
//				collisionList.add(new CollisionInfo(b, null, 0, -1, b.y + BALL_RADIUS - wh + 200));
//			}
//		}
//
//		final int N = 10;
//		//N回繰り返して拘束条件を解く
//		for (int i = 0; i < N; i++) {
//			for (CollisionInfo cInfo : collisionList) {
//				if (cInfo.b2 != null) { //ボールVSボール
//					vx = cInfo.b2.vx - cInfo.b1.vx; //相対速度
//					vy = cInfo.b2.vy - cInfo.b1.vy;
//					dot = vx * cInfo.nx + vy * cInfo.ny; //法線方向の相対速度
//					c = 0.5 * (1-e)*dot;//法線方向の撃力
//					cInfo.b1.vx += c * cInfo.nx;
//					cInfo.b1.vy += c * cInfo.ny;
//					cInfo.b2.vx -= c * cInfo.nx;
//					cInfo.b2.vy -= c * cInfo.ny;
//				} else { //ボールVS壁
//					vx = -cInfo.b1.vx; //相対速度
//					vy = -cInfo.b1.vy;
//					dot = vx * cInfo.nx + vy * cInfo.ny; //法線方向の相対速度
//					c = 0.5 * (1-e)*dot;//法線方向の撃力
//					cInfo.b1.vx += c * cInfo.nx;
//					cInfo.b1.vy += c * cInfo.ny;
//				}
//			}
//		}
//		//反発力を与える
//		for (CollisionInfo cInfo : collisionList) {
//			c = 0.5 * k * cInfo.s;//法線方向の撃力
//			cInfo.b1.vx += c * cInfo.nx;
//			cInfo.b1.vy += c * cInfo.ny;
//			if (cInfo.b2 != null) {
//				cInfo.b2.vx -= c * cInfo.nx;
//				cInfo.b2.vy -= c * cInfo.ny;
//			}
//		}
//	}
//
//	private final void stepPhase() {
//		for (int i = 0; i < balls.length; i++) {
//			balls[i].step();
//		}
//	}
//
//	private final void drawPhase() {
//		Canvas canvas = holder.lockCanvas();
//		if (canvas != null) {
//			canvas.drawColor(bgColor);
//			for (int i = 0; i < balls.length; i++) {
//				p.setColor(Color.WHITE);
//				canvas.drawCircle(balls[i].x, balls[i].y, BALL_RADIUS*1.5f, p);
//			}
//
//			getHolder().unlockCanvasAndPost(canvas);
//		}
//	}
//
//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		// TODO 自動生成されたメソッド・スタブ
//
//	}
//
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//		// TODO Auto-generated method stub
//		if(event.sensor.getType() == Sensor.TYPE_GRAVITY) {
//			float gx = event.values[0];
//			float gy = event.values[1];
//			float gz = event.values[2];
//			float g = (float)Math.sqrt(gx * gx + gy * gy + gz * gz);
//			if (g != 0) {
//				this.gx = -gx / g;
//				this.gy = gy / g;
//			}
//		}
//	}
//
//	private final class Task extends TimerTask {
//		@Override
//		public void run() {
//			if (holder != null) {
//				if (startFlag) {
//
//					touchPhase();
//
//					sortPhase();
//
//					collisionPhase();
//
//					stepPhase();
//
//					drawPhase();
//				}
//			}
//		}
//	}
//
//	private final class Ball {
//		private float x, y;
//		private float vx, vy;
//
//		public Ball() {
//			x = (int)(Math.random() * (ww-BALL_RADIUS*2)) + BALL_RADIUS;
//			y = (int)(Math.random() * (wh-BALL_RADIUS*2)) + BALL_RADIUS;
//			vx = (float)(Math.random() * 10 - 5);
//		}
//
//		public void step() {
//			vx += gx;
//			vy += gy;
//			x += vx;
//			y += vy;
//		}
//	}
//
//	//1軸スイープ＆プルーンを行うためのもの
//	private final class ObjectForSweep {
//		int value;
//		Ball b;
//		boolean isMinValue;
//
//		void set(float value, Ball b, boolean isMin) {
//			this.value = Math.max((int)value + 100, 0); //負になるとまずいので適当に増やす。maxは保険。
//			this.b = b;
//			this.isMinValue = isMin;
//		}
//	}
//
//	//衝突情報の保存用クラス
//	private final class CollisionInfo {
//		Ball b1, b2; //衝突したボールの組
//		double nx, ny; //法線
//		double s; //めりこみ
//
//		public CollisionInfo(Ball b1, Ball b2, double nx, double ny, double s) {
//			this.b1 = b1;
//			this.b2 = b2;
//			this.nx = nx;
//			this.ny = ny;
//			this.s = s;
//		}
//	}
//}
//
//
package com.trap.swallow.talk;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BackgroundView extends GLSurfaceView implements Renderer, SensorEventListener {

	private static final int BALL_RADIUS = 20;
	private static final int BALL_RADIUS_FOR_DRAW = 50;
	private static final int BALL_RADIUS_RED = 25;

	//見てる人の数に応じて数増やす
	private Ball[] balls = new Ball[200];
	private ObjectForSweep[] list = new ObjectForSweep[balls.length*2];
	private ArrayList<ObjectForSweep>[] bucket = new ArrayList[10];
	private ArrayList<ObjectForSweep>[] bucket2 = new ArrayList[10];
	private ArrayList<CollisionInfo> collisionList = new ArrayList<>();
	MotionEvent e;
	float tx, ty;
	float gx, gy;

	private boolean startFlag = false;
	private int ww, wh;

	/**
	 * 頂点データです。
	 */
	private static final float VERTEXS[] = {
		-1.0f,  1.0f, 0.0f,	// 左上
		-1.0f, -1.0f, 0.0f,	// 左下
		1.0f,  1.0f, 0.0f,	// 右上
		1.0f, -1.0f, 0.0f	// 右下
	};

	/**
	 * テクスチャ (UV マッピング) データです。
	 */
	private static final float TEXCOORDS[] = {
		0.0f, 0.0f,	// 左上
		0.0f, 1.0f,	// 左下
		1.0f, 0.0f,	// 右上
		1.0f, 1.0f	// 右下
	};

	/**
	 * 頂点バッファを保持します。
	 */
	private final FloatBuffer mVertexBuffer   = GLES20Utils.createBuffer(VERTEXS);

	/**
	 * テクスチャ (UV マッピング) バッファを保持します。
	 */
	private final FloatBuffer mTexcoordBuffer = GLES20Utils.createBuffer(TEXCOORDS);

	private int mProgram;
	private int mPosition;
	private int mTexcoord;
	private int mTextureId;

	private Bitmap bmp;
	private Canvas canvas;
	private Bitmap gaussian;
	private Bitmap gaussian_red;

	public boolean enable;

	public BackgroundView(final Context context) {
		super(context);
		enable = MyUtils.sp.getBoolean(MyUtils.BACKGROUND_ENABLE_KEY, false);
		this.setEGLContextClientVersion(2);	// OpenGL ES 2.0 を使用するように構成します。
		setRenderer(this);

		gaussian = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), com.trap.swallow.swallow.R.drawable.gaussian),
				BALL_RADIUS_FOR_DRAW*2, BALL_RADIUS_FOR_DRAW*2, false);
		gaussian_red = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), com.trap.swallow.swallow.R.drawable.gaussian2),
				BALL_RADIUS_RED*2, BALL_RADIUS_RED*2, false);

		for (int i = 0; i < list.length; i++)
			list[i] = new ObjectForSweep();
		for (int i = 0; i < bucket.length; i++)
			bucket[i] = new ArrayList<ObjectForSweep>();
		for (int i = 0; i < bucket2.length; i++)
			bucket2[i] = new ArrayList<ObjectForSweep>();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO 自動生成されたメソッド・スタブ
		e = event;
		performClick();
		return super.onTouchEvent(event);
	}

	@Override
	public boolean performClick() {
		// TODO 自動生成されたメソッド・スタブ
		return super.performClick();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if (startFlag) {
			if (enable) {

				touchPhase();

				sortPhase();

				collisionPhase();

				stepPhase();

				drawPhase();
			} else {
				gl.glClearColor(0.8627450980392157f, 0.9411764705882353f, 0.9803921568627451f, 1.0f);
				gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			}
		}

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO 自動生成されたメソッド・スタブ
		ww = width;
		wh = height;

		if (startFlag == false) {

			// ビューポートを設定します。
			GLES20.glViewport(0, 0, width, height);
			GLES20Utils.checkGlError("glViewport");

			for (int i = 0; i < balls.length; i++)
				balls[i] = new Ball();

			this.bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.canvas = new Canvas(bmp);
			this.mTextureId = GLES20Utils.loadTexture(bmp);
			startFlag = true;
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// OpenGL ES 2.0 を使用するので、パラメータで渡された GL10 インターフェースを無視して、代わりに GLES20 クラスの静的メソッドを使用します。

		// プログラムを生成して使用可能にします。
		mProgram = GLES20Utils.createProgram(readTextFile("test_vs.txt"), readTextFile("test_ps.txt"));
		if (mProgram == 0) {
			throw new IllegalStateException();
		}
		GLES20.glUseProgram(mProgram);
		GLES20Utils.checkGlError("glUseProgram");

		// シェーダで使用する変数のハンドルを取得し使用可能にします。

		mPosition = getAttribLocation("position");
		mTexcoord = getAttribLocation("texcoord");

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			float gx = event.values[0];
			float gy = event.values[1];
			float gz = event.values[2];
			float g = (float)Math.sqrt(gx * gx + gy * gy + gz * gz);
			if (g != 0) {
				this.gx = -gx / g;
				this.gy = gy / g;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private final String readTextFile(String filename) {
		AssetManager am = getResources().getAssets();
		InputStream is;
		StringBuilder result = new StringBuilder();
		try {
			is = am.open(filename);
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = r.readLine()) != null) {
				result.append(line + '\n');
			}
			is.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return result.toString();
	}

	private final int getAttribLocation(String paramName) {
		int result = GLES20.glGetAttribLocation(mProgram, paramName);
		GLES20Utils.checkGlError("glGetAttribLocation " + paramName);
		if (result == -1) {
			throw new IllegalStateException("Could not get attrib location for " + paramName);
		}
		GLES20.glEnableVertexAttribArray(result);
		return result;
	}

	private final int getUniformLocation(String paramName) {
		int result = GLES20.glGetUniformLocation(mProgram, paramName);
		GLES20Utils.checkGlError("glGetUniformLocation " + paramName);
		if (result == -1) {
			throw new IllegalStateException("Could not get uniform location for " + paramName);
		}
		return result;
	}

	private final void touchPhase() {
		if (e != null) {
			final int FINGER_RADIUS = 100;

			switch (e.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if (tx == -1) {
					tx = e.getX();
					ty = e.getY();
				} else {
					float vx = e.getX() - tx;
					float vy = e.getY() - ty;
					for (Ball b : balls) {
						float dx = b.x - e.getX();
						float dy = b.y - e.getY();
						if (dx * dx + dy * dy < FINGER_RADIUS * FINGER_RADIUS) {
							final float k = 0.5f;
							b.vx += k * vx;
							b.vy += k * vy;
						}
					}
					tx = e.getX();
					ty = e.getY();
				}
				break;
			case MotionEvent.ACTION_UP:
				e = null;
				break;
			}
		} else {
			tx = -1;
		}
	}

	private final void sortPhase() {
		//リストに値を代入
		for (int i = 0; i < balls.length; i++) {
			list[i*2].set(balls[i].x-BALL_RADIUS, balls[i], true);
			list[i*2+1].set(balls[i].x+BALL_RADIUS, balls[i], false);
		}
		//ソート

		//バケツを掃除
		for (int i = 0; i < bucket.length; i++)
			bucket[i].clear();
		for (int i = 0; i < bucket2.length; i++)
			bucket2[i].clear();

		//１の位でソート
		for (ObjectForSweep obj : list) {
			bucket[obj.value % 10].add(obj);
		}

		//10の位でソート
		for (ArrayList<ObjectForSweep> objects : bucket) {
			for (ObjectForSweep obj : objects) {
				bucket2[(obj.value / 10) % 10].add(obj);
			}
		}

		//もっかい掃除
		for (int i = 0; i < bucket.length; i++)
			bucket[i].clear();

		//100の位でソート
		for (ArrayList<ObjectForSweep> objects : bucket2) {
			for (ObjectForSweep obj : objects) {
				bucket[(obj.value / 100) % 10].add(obj);
			}
		}

		//もっかい掃除
		for (int i = 0; i < bucket2.length; i++)
			bucket2[i].clear();

		//1000の位でソート
		int count = 0;
		for (ArrayList<ObjectForSweep> objects : bucket) {
			for (ObjectForSweep obj : objects) {
				list[count++] = obj;
			}
		}
	}

	private final void collisionPhase() {
		collisionList.clear();

		final double e = 0.5; //跳ね返り係数
		final double k = 0.5; //ばね係数

		double dx, dy, d2, d, c, vx, vy, nx, ny, dot, s;
		Ball b1, b2;
		//ボールVSボール
		//1軸スイープ&プルーンにて衝突を検出
		for (int i = 0; i < list.length; i++) {
			if (!list[i].isMinValue) continue;
			for (int j = i+1; j < list.length; j++) {
				if (list[i].b == list[j].b) break;
				if (!list[j].isMinValue) continue;
				b1 = list[i].b; b2 = list[j].b;
				dx = b1.x - b2.x;
				dy = b1.y - b2.y;
				d2 = dx * dx + dy * dy;
				if (d2 < BALL_RADIUS * BALL_RADIUS * 4) {
					d = Math.sqrt(d2); //距離
					if (d == 0) {
						nx = 1; ny = 0;
					} else {
						nx =dx / d; ny = dy / d; //法線
					}
					s = BALL_RADIUS*2-d; //めりこみ(正)
					collisionList.add(new CollisionInfo(b1, b2, nx, ny, s));
				}
			}
		}
		//ボールVS壁
		for  (Ball b : balls) {
			//左の壁
			if (b.x - BALL_RADIUS < 0) {
				collisionList.add(new CollisionInfo(b, null, 1, 0, BALL_RADIUS - b.x));
			}
			//右の壁
			if (b.x + BALL_RADIUS > ww) {
				collisionList.add(new CollisionInfo(b, null, -1, 0, b.x + BALL_RADIUS - ww));
			}
			//上の壁
			if (b.y - BALL_RADIUS < 0) {
				collisionList.add(new CollisionInfo(b, null, 0, 1, BALL_RADIUS - b.y));
			}
			//下の壁
			if (b.y + BALL_RADIUS > wh - 200) {
				collisionList.add(new CollisionInfo(b, null, 0, -1, b.y + BALL_RADIUS - wh + 200));
			}
		}

		final int N = 10;
		//N回繰り返して拘束条件を解く
		for (int i = 0; i < N; i++) {
			for (CollisionInfo cInfo : collisionList) {
				if (cInfo.b2 != null) { //ボールVSボール
					vx = cInfo.b2.vx - cInfo.b1.vx; //相対速度
					vy = cInfo.b2.vy - cInfo.b1.vy;
					dot = vx * cInfo.nx + vy * cInfo.ny; //法線方向の相対速度
					c = 0.5 * (1-e)*dot;//法線方向の撃力
					cInfo.b1.vx += c * cInfo.nx;
					cInfo.b1.vy += c * cInfo.ny;
					cInfo.b2.vx -= c * cInfo.nx;
					cInfo.b2.vy -= c * cInfo.ny;
				} else { //ボールVS壁
					vx = -cInfo.b1.vx; //相対速度
					vy = -cInfo.b1.vy;
					dot = vx * cInfo.nx + vy * cInfo.ny; //法線方向の相対速度
					c = 0.5 * (1-e)*dot;//法線方向の撃力
					cInfo.b1.vx += c * cInfo.nx;
					cInfo.b1.vy += c * cInfo.ny;
				}
			}
		}
		//反発力を与える
		for (CollisionInfo cInfo : collisionList) {
			c = 0.5 * k * cInfo.s;//法線方向の撃力
			cInfo.b1.vx += c * cInfo.nx;
			cInfo.b1.vy += c * cInfo.ny;
			if (cInfo.b2 != null) {
				cInfo.b2.vx -= c * cInfo.nx;
				cInfo.b2.vy -= c * cInfo.ny;
			}
		}
	}

	private final void stepPhase() {
		for (int i = 0; i < balls.length; i++) {
			balls[i].step();
		}

	}


	private final void drawPhase() {

		Paint p = new Paint();
		p.setColor(Color.WHITE);
		//まずキャンバスにガウシアンを描画
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		for (Ball b : balls) {
			canvas.drawBitmap(gaussian, b.x-BALL_RADIUS_FOR_DRAW, b.y-BALL_RADIUS_FOR_DRAW, null);
		}
		//		for (int i = 0; i < 20; i++)
		//		canvas.drawBitmap(gaussian_red, balls[i].x-BALL_RADIUS_RED, balls[i].y-BALL_RADIUS_RED, null);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);



		// 背景色を指定して背景を描画します。
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


		GLES20.glVertexAttribPointer(mPosition, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
		GLES20.glVertexAttribPointer(mTexcoord, 2, GLES20.GL_FLOAT, false, 0, mTexcoordBuffer);
		// テクスチャの指定
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	private final class Ball {
		private float x, y;
		private float vx, vy;
		private float initialX, initialY;
		private float departX, departY;

		public Ball() {
			x = (int)(Math.random() * (ww-BALL_RADIUS*2)) + BALL_RADIUS;
			y = (int)(Math.random() * (wh-BALL_RADIUS*2)) + BALL_RADIUS;
			vx = (float)(Math.random() * 10 - 5);
			initialX = x;
			initialY = y;
		}

		public void step() {
			vx += gx;
			vy += gy;
			x += vx;
			y += vy;
		}
	}

	//1軸スイープ＆プルーンを行うためのもの
	private final class ObjectForSweep {
		int value;
		Ball b;
		boolean isMinValue;

		void set(float value, Ball b, boolean isMin) {
			this.value = Math.max((int)value + 100, 0); //負になるとまずいので適当に増やす。maxは保険。
			this.b = b;
			this.isMinValue = isMin;
		}
	}

	//衝突情報の保存用クラス
	private final class CollisionInfo {
		Ball b1, b2; //衝突したボールの組
		double nx, ny; //法線
		double s; //めりこみ

		public CollisionInfo(Ball b1, Ball b2, double nx, double ny, double s) {
			this.b1 = b1;
			this.b2 = b2;
			this.nx = nx;
			this.ny = ny;
			this.s = s;
		}
	}

}
