package com.trap.swallow.talk;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

public class LineView extends View {

	public LineView(Context context) {
		super(context);
		setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, 1));
		setBackgroundColor(Color.rgb(200, 200, 200));
	}

}
