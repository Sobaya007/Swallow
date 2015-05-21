package com.trap.swallow.talk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.MediaColumns;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow.File;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.swallow.R;

public class TalkActivity extends AppCompatActivity {


	private final static int CHOICE_CODE = 12345;

	TalkManager tvManager;
	public ScrollView scrollView;

	private InputMethodManager imm;

	private int backCount = -1;

	private TranslateAnimation input_in_animation;
	private TranslateAnimation input_out_animation;
	private Animation talk_in_animation;
	private Animation talk_out_animation;

	private EditText input;

	private ArrayList<Integer> storeSelectedTagIdListForReceive = new ArrayList<Integer>();

	private AlertDialog.Builder tagSelectDialogBuilder;
	private AlertDialog tagSelectDialog;
	private OnMultiChoiceClickListener tagChoiceListener;
	private OnShowListener tagSelectDialogShowListener;

	private AlertDialog.Builder tagAddDialogBuilder;
	private EditText tagAddInput;

	private View inputView;

	private BackgroundView bgView;
	private SensorManager sManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.activity_talk);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //ホームボタンのタッチを有効にする
		getSupportActionBar().setHomeButtonEnabled(true);

		bgView = new BackgroundView(this);
		FrameLayout mainLayout = new FrameLayout(this);
		mainLayout.addView(bgView);
		mainLayout.addView(LayoutInflater.from(this).inflate(R.layout.view_talk, null));
		mainLayout.addView(new View(this) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				bgView.onTouchEvent(event);
				return super.onTouchEvent(event);
			}
		});


		ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
		flipper.addView(mainLayout);
		inputView = LayoutInflater.from(this).inflate(R.layout.view_input, null);
		flipper.addView(inputView);

		sManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sManager.getSensorList(Sensor.TYPE_GRAVITY);
		if(sensors.size() > 0) {
			Sensor s = sensors.get(0);
			sManager.registerListener(bgView, s, SensorManager.SENSOR_DELAY_UI);
		}

		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		//メインのレイアウトのセッティング
		scrollView = (ScrollView)findViewById(R.id.talk_scroll_view);
		LinearLayout layout = (LinearLayout)findViewById(R.id.left_draw);

		//tvManagerの初期化
		final int myUserId = 0; //擬似
		tvManager = new TalkManager(this, layout, myUserId);
		tvManager.init();

		//画面遷移のアニメーションのセッティング
		initAnimations();

		//入力ボックスの設定
		initInputText();

		initDetailButton();

		initSubmitButton();

		//talk_view側入力ボックスのセッティング
		initInputBoxOnTalkView();

		//タグ選択ボタンのセッティング
		initTagSelectButton();

		//送信ボタンのセッティング
		initSendMessageButton();

		//ファイルアップロードボタンの設定
		initFileUploadButton();

		//タグ一覧表示ビューの設定
		initTagSelectList();

		//タグ検索ボックスを設定
		initTagSearchBox();

        //ドロワーの設定
        initDrawer();

		//タグ選択ダイアログの設定
		initTagSelectDialog();

		//別スレッドを起動
		Timer t = new Timer();
		t.schedule(new Task(), 0, 20);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO 自動生成されたメソッド・スタブ
		//ホームボタンなら
		if (item.getItemId() == android.R.id.home) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					tvManager.loadNewMessage();
					scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MessageView mv = (MessageView)v;
		//テキストに対する長押し時
		//自分の投稿なら
		//		if (mv.mInfo.userId == tvManager.myUserId) {
		menu.setHeaderTitle("メッセージ操作");
		menu.add(mv.mInfo.getPostID(), 0, Menu.NONE, "編集");
		menu.add(mv.mInfo.getPostID(), 1, Menu.NONE, "削除");
		//		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//テキスト長押しのポップアップに対する処理
		switch (item.getItemId()) {
		case 0: //編集時
			//入力フォームにシフト
			shiftToInputForm();
			//編集
			editMessage(item.getGroupId());
			break;
		case 1: //削除
			//削除
			deleteMessage(item.getGroupId());
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//戻るボタンが押された時
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//input formが出ていたら戻す
			ViewFlipper vf = (ViewFlipper)findViewById(R.id.flipper);
			if (vf.getCurrentView() == inputView) {
				//キーボードを隠す
				hideKeyboard(vf);
				vf.setInAnimation(talk_in_animation);
				vf.setOutAnimation(input_out_animation);
				vf.showPrevious();
			}
			//そうでなければ一回目に警告、二回目に戻る
			else if (backCount <= 0) {
				backCount = 300;
				Toast t = Toast.makeText(this, "戻るにはもう一回ボタンを押してください", Toast.LENGTH_SHORT);
				t.show();
			} else {
				return super.onKeyDown(keyCode, event);
			}
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == CHOICE_CODE) {
			try {
				//受け取ったデータのパスを取得
				Uri uri = data.getData();
				String scheme = uri.getScheme();
				String path = null;
				if ("file".equals(scheme)) {
					path = uri.getPath();
				} else if("content".equals(scheme)) {
					ContentResolver contentResolver = getContentResolver();
					Cursor cursor = contentResolver.query(uri, new String[] { MediaColumns.DATA }, null, null, null);
					if (cursor != null) {
						cursor.moveToFirst();
						path = cursor.getString(0);
						cursor.close();
					}
				}
				//イメージビューを生成
				LinearLayout thumbnailLayout = (LinearLayout)findViewById(R.id.file_thumbail_layout);
				ImageView iv = new ImageView(this);
				LinearLayout.LayoutParams params = MyUtils.getLayoutparams(thumbnailLayout.getWidth() / 5, thumbnailLayout.getWidth() / 5);
				params.leftMargin = 3; params.rightMargin = 3;
				iv.setLayoutParams(params);
				iv.setScaleType(ScaleType.FIT_XY);
				Bitmap bmp = null;

				String mimeType = MyUtils.getMimeType(path);

				//画像を乗っける
				if (mimeType.startsWith("image")) {
					InputStream in = getContentResolver().openInputStream(uri);
					bmp = BitmapFactory.decodeStream(in);
					in.close();
				} else {
					bmp = MyUtils.getImageFromPath(getResources(), mimeType);
				}
				iv.setImageBitmap(bmp);
				thumbnailLayout.addView(iv);

				MyUtils.showShortToast(this, path);
				File file = new File(null, System.currentTimeMillis(), path, mimeType, tvManager.getTagIdSelectedForSend(), null);

				tvManager.postFileData.add(file);
			} catch (FileNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu) {
	//		// メニューの要素を追加して取得
	//		MenuItem upItem = menu.add("Up");
	//		// アイコンを設定
	//		upItem.setIcon(R.drawable.);
	//
	//		// SHOW_AS_ACTION_ALWAYS:常に表示
	//		upItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	//		return super.onCreateOptionsMenu(menu);
	//	}

	private final void initAnimations() {
		input_in_animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 1.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f);

		input_out_animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 1.0f);

		talk_in_animation = new AlphaAnimation(0.0f, 1.0f);

		talk_out_animation = new AlphaAnimation(1.0f, 0.0f);

		input_in_animation.setDuration(150);
		input_out_animation.setDuration(150);
		talk_in_animation.setDuration(150);
		talk_out_animation.setDuration(150);

		input_in_animation.setInterpolator(new AccelerateDecelerateInterpolator());
		input_out_animation.setInterpolator(new AccelerateDecelerateInterpolator());
	}

	private final void initInputBoxOnTalkView() {
		EditText input = (EditText)findViewById(R.id.input_text_dummy);
		input.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
	}

	private final void initDetailButton() {
		Button button = (Button)findViewById(R.id.detail_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				shiftToInputForm();
				tvManager.inputType = TalkManager.InputType.POST;
			}
		});
	}

	private final void initSubmitButton() {
		Button sendButton = (Button)findViewById(R.id.talk_submit_button);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText input = (EditText)findViewById(R.id.input_text_dummy);
				//送信ボタンをタップしたときの処理
				tvManager.inputType = TalkManager.InputType.POST;
				String text = input.getText().toString();
				boolean  postResponse = submitMessage(text);
				if (postResponse) {
					//投稿成功
					//EditTextの文字列を削除
					input.setText("");
					//投稿するファイルのデータを削除
					tvManager.postFileData.clear();
					//キーボードを隠す
					hideKeyboard(v);
				}
			}
		});
	}

	private final void initTagSelectButton() {
		final Button tagButton = (Button)findViewById(R.id.tag_select_button);
		StringBuilder sb= new StringBuilder();
		for (int i = 0; i < tvManager.tagItemSelectedListForSend.length; i++) {
			if (tvManager.tagItemSelectedListForSend[i]) {
				sb.append(tvManager.findTagByIndex(i).getTagName());
				sb.append(", ");
			}
		}
		tagButton.setText(sb.toString().substring(0, sb.length()-2));
		tagButton.setOnClickListener(new OnClickListener() {
			//タグボタンが押された時の処理
			@Override
			public void onClick(View view) {
				showTagSelectDialog();
			}
		});
	}

	private final void initSendMessageButton() {
		Button sendButton = (Button)findViewById(R.id.send_message_button);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//送信ボタンをタップしたときの処理
				String text = input.getText().toString();
				boolean  postResponse = submitMessage(text);
				if (postResponse) {
					//投稿成功
					//EditTextの文字列を削除
					input.setText("");
					//投稿するファイルのデータを削除
					tvManager.postFileData.clear();
					//キーボードを隠す
					hideKeyboard(v);
					//view_talkに戻る
					((ViewFlipper)findViewById(R.id.flipper)).showPrevious();
				}
			}
		});
	}

	private final void initFileUploadButton() {
		Button uploadButton = (Button)findViewById(R.id.file_upload_button);
		uploadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//ファイルアップロードボタン押下時の処理
				showFileUploadDialog();
			}
		});
	}

	private final void initTagSelectList() {
		final ListView tagSelectList = (ListView)findViewById(R.id.drawer_tag_select_list);
		tagSelectList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				String selectedTag = (String)tagSelectList.getItemAtPosition(position);
				Tag t = tvManager.findTagByName(selectedTag);
				int selectedTagId = t.getTagID();
				int index = tvManager.selectedTagIdListForReceive.indexOf(selectedTagId);
				if (index == -1)
					tvManager.selectedTagIdListForReceive.add(selectedTagId);
				else
					tvManager.selectedTagIdListForReceive.remove(index);
			}
		});
		updateTagSelectList();
	}

	private final void initTagSearchBox() {
		final EditText searchTagBox = (EditText)findViewById(R.id.drawer_search_box);
		searchTagBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String searchWord = searchTagBox.getText().toString();
				boolean emptyFlag = searchWord.length() == 0;
				//				searchWord = "^" + searchWord;

				ArrayList<String> tag = new ArrayList<String>();

				//タグリストの中から先頭が一致するものを検索
				if (emptyFlag) {
					for (int i = 0; i < tvManager.getTagNum(); i++) {
						tag.add(tvManager.findTagByIndex(i).getTagName());
					}
				} else {
					for (int i = 0; i < tvManager.getTagNum(); i++) {
						String tagName = tvManager.findTagByIndex(i).getTagName();
						if (tagName.startsWith(searchWord)) {
							tag.add(tagName);
						}
					}
				}
				String[] members = new String[tag.size()];
				for (int i = 0; i < members.length; i++)
					members[i] = tag.get(i);
				//tagSelectListに検索結果を適用
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				ListView tagSelectList = (ListView)findViewById(R.id.drawer_tag_select_list);
				tagSelectList.setAdapter(adapter);
				//選択されている文字列を羅列
				ArrayList<String> selectedTagNameList = new ArrayList<String>();
				for (int selectedId : tvManager.selectedTagIdListForReceive) {
					Tag t = tvManager.findTagById(selectedId);
					selectedTagNameList.add(t.getTagName());
				}
				//tagSelectListに選択状況を適用
				for (int i = 0; i < members.length; i++) {
					tagSelectList.setItemChecked(i, selectedTagNameList.contains(members[i]));
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	private final void initDrawer() {
		DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
				drawer, R.string.drawer_open,
				R.string.drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				// TODO 自動生成されたメソッド・スタブ
				updateTagSelectList();
				storeSelectedTagIdListForReceive.clear();
				for (int id : tvManager.selectedTagIdListForReceive)
					storeSelectedTagIdListForReceive.add(id);
				super.onDrawerOpened(drawerView);
			}
			@Override
			public void onDrawerClosed(View drawerView) {
				//ドロワーが閉じた時の処理
				//開いた時と選択状況が違ったら更新
				if (storeSelectedTagIdListForReceive.size() != tvManager.selectedTagIdListForReceive.size()) {
					tvManager.refreshOnTagSelectChanged();
				} else {
					for (int i = 0; i < storeSelectedTagIdListForReceive.size(); i++) {
						if (storeSelectedTagIdListForReceive.get(i) != tvManager.selectedTagIdListForReceive.get(i)) {
							tvManager.refreshOnTagSelectChanged();
							break;
						}
					}
				}
				super.onDrawerClosed(drawerView);
			}
		};
		drawer.setDrawerListener(toggle);
		//影
		drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
	}

	private final void initInputText() {
		input = (EditText)findViewById(R.id.input_text);
		//		input = new EditText(this) {
		//			public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		//				if (keyCode == KeyEvent.KEYCODE_BACK) {
		//					//入力ボックス入力中に戻るボタンが押された時
		//
		//				}
		//				return false;
		//			};
		//		};
		//		input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,  LinearLayout.LayoutParams.MATCH_PARENT));
		//		input.setGravity(Gravity.TOP);
		//		input.setHint(R.string.input_text_hint);
		//		input.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		//		input.setBackgroundColor(Color.argb(0, 0, 0, 0));
		//
		//		ScrollView sv = (ScrollView)findViewById(R.id.input_scroll_view);
		//		sv.addView(input);
	}

	private final void initTagSelectDialog() {
		//タグ選択ダイアログの設定
		tagSelectDialogBuilder = new AlertDialog.Builder(TalkActivity.this);
		//タイトル設定
		tagSelectDialogBuilder.setTitle(R.string.tag_select_button_text);
		//ボタン設定
		tagSelectDialogBuilder.setPositiveButton(R.string.ok_text, null);
		tagSelectDialogBuilder.setNeutralButton(R.string.tag_add_button_text, null);


		//タグ追加ダイアログの設定
		tagAddDialogBuilder = new AlertDialog.Builder(this);
		//タイトル設定
		tagAddDialogBuilder.setTitle(R.string.tag_add_button_text);
		//ビューを設定
		final View view = View.inflate(TalkActivity.this, R.layout.dialog_tag_add, null);
		tagAddDialogBuilder.setView(view);

		tagAddInput = (EditText)view.findViewById(R.id.tag_add_input);

		tagAddDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				//タグ追加確定ボタンが押されたときの処理
				onTagAddDecideButtonPressed();
			}
		});
		//キャンセルボタンを置く
		tagAddDialogBuilder.setNegativeButton(R.string.cancel_text, null);

		tagChoiceListener = new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {}
		};

		tagSelectDialogShowListener = new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button okButton = tagSelectDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				okButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						//タグ選択確定ボタンの処理
						onTagSelectButtonPressed();
					}
				});
				Button addButton = tagSelectDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
				addButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						//タグ追加ボタンの処理
						onTagAddButtonPressed();
					}
				});
			}
		};
	}

	private final boolean submitMessage(String text) {
		if (text.length() != 0) {
			//投稿
			try {
				switch (tvManager.inputType) {
				case POST:
					return tvManager.submit(text);
				case EDIT:
					return tvManager.editMessage(text);
				default:
					return false;
				}
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		return false;
	}

	private final void showTagSelectDialog() {
		//タグリストをStringの配列にする
		final String[] items = new String[tvManager.getTagNum()];
		for (int i = 0 ; i < items.length; i++)
			items[i] = tvManager.findTagByIndex(i).getTagName();
		tagSelectDialogBuilder.setMultiChoiceItems(items, tvManager.tagItemSelectedListForSend,
				tagChoiceListener) ;
		//ダイアログを作成
		tagSelectDialog = tagSelectDialogBuilder.create();
		tagSelectDialog.setOnShowListener(tagSelectDialogShowListener);
		tagSelectDialog.show();
	}

	private final void updateTagSelectList() {
		ListView tagSelectList = (ListView)findViewById(R.id.drawer_tag_select_list);
		String[] members = new String[tvManager.getTagNum()];
		for (int i = 0; i < members.length; i++)
			members[i] = tvManager.findTagByIndex(i).getTagName();
		ArrayAdapter<String> tagList = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, members);
		tagSelectList.setAdapter(tagList);
		//選択されている文字列を羅列
		ArrayList<String> selectedTagNameList = new ArrayList<String>();
		for (int id : tvManager.selectedTagIdListForReceive) {
			selectedTagNameList.add(tvManager.findTagById(id).getTagName());
		}
		//tagSelectListに選択状況を適用
		for (int i = 0; i < members.length; i++) {
			tagSelectList.setItemChecked(i, selectedTagNameList.contains(members[i]));
		}
	}

	//未実装
	private final void showFileUploadDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(TalkActivity.this);
		b.setItems(R.array.file_upload_dialog_items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent;
				switch (which) {
				case 0: //画像選択
					intent = new Intent(Intent.ACTION_PICK);
					intent.setType("vnd.android.cursor.dir/image");
					startActivityForResult(intent, CHOICE_CODE);
					break;
				case 1: //その他ファイル選択
					intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("file/*");
					startActivityForResult(intent, CHOICE_CODE);
					break;
				}
				dialog.dismiss();
			}
		});
		b.show();
	}

	private final void shiftToInputForm() {
		//input_viewへ遷移
		ViewFlipper vf = (ViewFlipper)findViewById(R.id.flipper);
		vf.setInAnimation(input_in_animation);
		vf.setOutAnimation(talk_out_animation);
		vf.showNext();
		//入力フォームにフォーカスを当てる
		input.requestFocus();
		//キーボード表示
		showKeyboard(input);
	}

	private final void editMessage(int postId) {
		LinearLayout layout = (LinearLayout)findViewById(R.id.left_draw);
		//編集するMessageViewを検索
		for (int i = 0; i < layout.getChildCount(); i++) {
			MessageView mv = (MessageView)layout.getChildAt(i);
			if (mv.mInfo.getPostID() == postId) {
				//送信側のタグ選択状況を変更
				for (int j = 0; j < tvManager.getTagNum(); j++) {
					boolean contains = false;
					int tagId = tvManager.findTagByIndex(j).getTagID();
					for (int tagId2 : mv.mInfo.getTagID()) {
						if (tagId == tagId2) {
							contains = true;
							break;
						}
					}
					tvManager.tagItemSelectedListForSend[j]
							= contains;
				}
				//入力フォームに入っている文字列を変更
				input.setText(mv.mInfo.getMessage());

				tvManager.editingPostId = mv.mInfo.getPostID();
				break;
			}
		}
		tvManager.inputType = TalkManager.InputType.EDIT;
	}

	private final void deleteMessage(int postId) {
		LinearLayout layout = (LinearLayout)findViewById(R.id.left_draw);
		//削除するMessageViewを検索
		for (int i = 0; i < layout.getChildCount(); i++) {
			MessageView mv = (MessageView)layout.getChildAt(i);
			if (mv.mInfo.getPostID() == postId) {
				if (SCM.scm.deleteMessage(postId) == false) {
					MyUtils.showShortToast(this, R.string.message_delete_failed_text);
				} else {
					layout.removeView(mv);
				}
				break;
			}
		}
	}

	private final void showKeyboard(final EditText edit) {
		//キーボードの表示
		Runnable r = new Runnable() {
			@Override
			public void run() {
				imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
			}
		};
		new Handler().post(r);
	}

	protected final void hideKeyboard(View v) {
		//キーボードを隠す
		imm.hideSoftInputFromWindow(v.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private final void onTagSelectButtonPressed() {
		//ボタンに選択されているタグを並べて書く
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tvManager.getTagNum(); i++) {
			if (tvManager.tagItemSelectedListForSend[i]) {
				sb.append(tvManager.findTagByIndex(i).getTagName());
				sb.append(", ");
			}
		}
		if (sb.length() > 2) {
			//最後の「,」を抜く
			sb = sb.delete(sb.length()-2, sb.length());
		}
		String text = sb.toString();
		Button tagButton = (Button)TalkActivity.this.findViewById(R.id.tag_select_button);
		tagButton.setText(text);
		tagSelectDialog.dismiss(); //alertDialog抹消
	}

	private final void onTagAddButtonPressed() {
		//タグ追加ダイアログを表示

		//キーボード表示
		showKeyboard(tagAddInput);

		//タグ追加ダイアログを表示
		tagAddDialogBuilder.show();
	}

	private final void onTagAddDecideButtonPressed() {
		String tag = tagAddInput.getText().toString().trim();
		//何か入力されていて、まだそのタグがなかった場合
		if (tag.length() != 0 && tvManager.findTagByName(tag) == null) {
			//tagListに新たなタグを追加
			int[] newTagId = new int[1];
			if (SCM.scm.sendAddTag(tag, newTagId) == false) {
				Toast.makeText(TalkActivity.this, R.string.tag_add_failed_text, Toast.LENGTH_SHORT).show();
			} else {
				tvManager.addTag(new Tag(newTagId[0], System.currentTimeMillis(), tag, 0));
				//tagItemSelectedListを大きくする
				boolean[] newTagItemSelectedList = new boolean[tvManager.getTagNum()];
				for (int i = 0; i < tvManager.tagItemSelectedListForSend.length; i++)
					newTagItemSelectedList[i] = tvManager.tagItemSelectedListForSend[i];
				//新たに加えられたタグはチェックしておく
				newTagItemSelectedList[newTagItemSelectedList.length-1] = true;
				tvManager.tagItemSelectedListForSend = newTagItemSelectedList;
				//タグ選択ダイアログを再表示
				tagSelectDialog.dismiss();
				showTagSelectDialog();
			}
		}
	}

	private final class Task extends TimerTask {

		@Override
		public void run() {
			backCount--;
			TalkActivity.this.tvManager.run();
		}

	}
}
