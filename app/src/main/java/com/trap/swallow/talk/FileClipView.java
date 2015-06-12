package com.trap.swallow.talk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.trap.swallow.swallow.R;

import java.util.ArrayList;

public final class FileClipView extends View {

	ArrayList<Bitmap> bitmaps = new ArrayList<>();
	Bitmap back, forward;
	RectF rect;

	FileClipView(Context context) {
		super(context);
		init();
	}

	public FileClipView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private final void init() {
		back = BitmapFactory.decodeResource(getResources(), R.drawable.clip_backward);
		forward = BitmapFactory.decodeResource(getResources(), R.drawable.clip_forward);
	}

	void addImage(Bitmap bmp) {
		bitmaps.add(Bitmap.createScaledBitmap(bmp, getWidth() / 3, getHeight() / 3, false));
		this.invalidate();
	}

	void removeImage(int index) {
		bitmaps.remove(index);
		this.invalidate();
	}

	public void clearImage() {
		bitmaps.clear();
		this.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = getHeight() * back.getWidth() / back.getHeight();
		Rect rect = new Rect((getWidth() - width) / 2, 0, (getWidth() + width) / 2, getHeight());
		canvas.drawBitmap(back, new Rect(0, 0, back.getWidth(), back.getHeight()), rect, null);
		float dAngle = 120.0f / (bitmaps.size()+1);
		canvas.save();
		canvas.translate(getWidth() * 0.5f, getHeight() * 0.5f);
		canvas.rotate(-60 + dAngle);
		for (Bitmap bmp : bitmaps) {
			canvas.drawBitmap(bmp, -bmp.getWidth() * 0.5f, 0, null);
			canvas.rotate(dAngle);
		}
		canvas.restore();
		canvas.drawBitmap(forward, new Rect(0, 0, forward.getWidth(), forward.getHeight()), rect, null);
	}


}