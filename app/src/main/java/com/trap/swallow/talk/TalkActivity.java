package com.trap.swallow.talk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.trap.swallow.gcm.RegistrationIntentService;
import com.trap.swallow.info.FileInfo;
import com.trap.swallow.info.TagInfo;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.swallow.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TalkActivity extends AppCompatActivity {

	public static TalkActivity singleton;

	private final static int CHOICE_CODE = 12345;
	private static final int USER_IMAGE_CHANGE_CODE = 25252;

	private static final int REFLESH_BUTTON_ID = 0;
	private static final int UP_BUTTON_ID = 1;
	private static final int DOWN_BUTTON_ID = 2;
	private static final int SETTING_BUTTON_ID = 3;

	public TalkManager tvManager;
	public ScrollView scrollView;

	private int backCount = -1;

	private TranslateAnimation input_in_animation;
	private TranslateAnimation input_out_animation;
	private Animation talk_in_animation;
	private Animation talk_out_animation;

	private EditText input;

	private AlertDialog.Builder tagSelectDialogBuilder;
	private AlertDialog tagSelectDialog;
	private OnMultiChoiceClickListener tagChoiceListener;
	private OnShowListener tagSelectDialogShowListener;

	private AlertDialog.Builder tagAddDialogBuilder;
	private AlertDialog tagAddDialog;
	private EditText tagAddInput;
	private AlertDialog.Builder enqueteDialogBuilder;
	private AlertDialog enqueteDialog;
	private AlertDialog.Builder enqueteAddDialogBuilder;
	private AlertDialog enqueteAddDialog;
	private EditText enqueteAddInput;
	private OnShowListener enqueteDialogShowListener;
	private View inputView;
	private View settingView;
	private View settingTagView;

	private BackgroundView bgView;
	private FileInfo userImageFile; //設定変更時の一時保存用

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//Singletonを設定
		singleton = this;

		//GCMへの登録
		initGCM();

		//xmlの内容を適用
		setContentView(R.layout.activity_talk);

		//タイトルを消す
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		//いろいろ置くためのLayout
		FrameLayout mainLayout = new FrameLayout(this);

		//bgViewを初期化
		initBackgroundView(mainLayout);

		//Flipperを初期化
		initFlipper(mainLayout);

		//重力センサーの初期化
		initGravitySensor();

		//ScrollViewを取得しておく
		scrollView = (ScrollView)findViewById(R.id.talk_scroll_view);

		//tvManagerの初期化
		initTalkManager();

		//画面遷移のアニメーションの初期化
		initAnimations();

		//入力ボックスの初期化
		initInputText();

		//kwskボタンの初期化
		initDetailButton();

		//talk_view側投稿ボタンの初期化
		initSubmitButton();

		//talk_view側入力ボックスの初期化
		initInputBoxOnTalkView();

		//タグ選択ボタンの初期化
		initTagSelectButton();

		//input_view側送信ボタンの初期化
		initSendMessageButton();

		//ファイルアップロードボタンの初期化
		initFileUploadButton();

		//タグ検索ボックスを初期化
		initTagSearchBox();

		//ドロワーの初期化
		initDrawer();

		//タグ選択ダイアログの初期化
		initTagSelectDialog();

		//アンケートボタンの初期化
		initEnqueteButton();

		//既読ボタンの初期化
		initReceivedCheckBox();

		//設定画面の初期化
		initSetting();

		//別スレッドを起動
		Timer t = new Timer();
		t.schedule(new Task(), 0, 1000); //50FPS
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO 自動生成されたメソッド・スタブ
		//ホームボタンなら
		switch (item.getItemId()) {
			case android.R.id.home:

				break;
			case REFLESH_BUTTON_ID:
				AsyncTask<Void, Void, ArrayList<MessageView>> task = new AsyncTask<Void, Void, ArrayList<MessageView>>() {
					@Override
					protected ArrayList<MessageView> doInBackground(Void... params) {
						return tvManager.loadPreviousMessage();
					}

					@Override
					protected void onPostExecute(ArrayList<MessageView> messageViews) {
						if (messageViews != null) {
							for (MessageView mv : messageViews)
								tvManager.addMessageViewToPrev(mv);
							MyUtils.showShortToast(TalkActivity.this, "読み込み完了");
						} else {
							MyUtils.showShortToast(TalkActivity.this, "更新に失敗しました");
						}
					}
				};
				task.execute((Void) null);
				break;
			case UP_BUTTON_ID:
				MyUtils.scrollUp();
//				scrollView.fullScroll(ScrollView.FOCUS_UP);
				break;
			case DOWN_BUTTON_ID:
				MyUtils.scrollDown();
//				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				break;
			case SETTING_BUTTON_ID:
				shiftToSetting();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if (v instanceof MessageView) {
			MessageView mv = (MessageView) v;
			//テキストに対する長押し時
			//自分の投稿なら
			if (mv.mInfo.getUserID() == tvManager.getMyUserInfo().user.getUserID()) {
				menu.setHeaderTitle("メッセージ操作");
				menu.add(mv.mInfo.getPostID(), 0, Menu.NONE, "編集");
				menu.add(mv.mInfo.getPostID(), 1, Menu.NONE, "削除");
			}
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		//テキスト長押しのポップアップに対する処理
		switch (item.getItemId()) {
			case 0: //編集時
				//入力フォームにシフト
				shiftToInputForm();
				//編集
				editMessage(item.getGroupId());
				//キーボード表示
				showKeyboard(input);
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
			ViewFlipper vf = (ViewFlipper)findViewById(R.id.flipper);
			if (vf.getCurrentView() == inputView) {
				//input formが出ていたら

				//キーボードを隠す
				hideKeyboard(vf);

				//トークにもどる
				shiftToTalk();

				if (tvManager.isReplyMode()) {
					//リプ中なら
					tvManager.endReply();
				}
				if (tvManager.isEditMode()) {
					//編集中なら
					tvManager.endEdit();
				}

			} else if (vf.getCurrentView() == settingView) {
				//設定画面なら
				//トークに戻る
				shiftToTalk();
			} else if (vf.getCurrentView() == settingTagView) {
				//タブ設定画面なら
				//設定画面に戻る
				shiftToSetting();
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
	protected void onStop() {
		//タグの選択状況をプリファレンスに保存
		MyUtils.sp.edit().putString(MyUtils.SELECTED_TAG_KEY, tvManager.getSelectedTagIDText()).apply();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case CHOICE_CODE:
					try {
						//受け取ったデータのパスを取得
						Uri uri = data.getData();

						FileInfo file = new FileInfo(getContentResolver(), getResources(), uri, tvManager.getSelectedTagIDList());
						tvManager.addFileToPost(file);
					}
					catch (Exception e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
					break;
				case USER_IMAGE_CHANGE_CODE:
					try {
						//受け取ったデータのパスを取得
						Uri uri = data.getData();

						FileInfo file = new FileInfo(getContentResolver(), getResources(), uri, tvManager.getSelectedTagIDList());
						ImageView iv = (ImageView)findViewById(R.id.setting_image);
						iv.setImageBitmap(file.bmp);
						userImageFile = file;
					}
					catch (Exception e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
					break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// メニューの要素を追加
		MenuItem item = menu.add(0, SETTING_BUTTON_ID, 3, "設定")
				.setIcon(R.drawable.gear2_360);
		MenuItem item2 = menu.add(0, REFLESH_BUTTON_ID, 0, "更新")
				.setIcon(R.drawable.update_360);
		MenuItem item3 = menu.add(0, UP_BUTTON_ID, 1, "上へ")
				.setIcon(R.drawable.scroll_up);
		MenuItem item4 = menu.add(0, DOWN_BUTTON_ID, 2, "下へ")
				.setIcon(R.drawable.scroll_down);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(item2, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(item3, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(item4, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
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
	//	}4
	private final void initGCM() {
		//GCMへの登録が済んでいなかったら
//		if (!MyUtils.sp.getBoolean(MyUtils.GCM_REGISTRATION_FLAG, false)) {
			//IntentでGCMへ登録
			Intent intent = new Intent(this, RegistrationIntentService.class);
			startService(intent);
			//PreferenceにGCMへの登録が済んだことを通知
			MyUtils.sp.edit().putBoolean(MyUtils.GCM_REGISTRATION_FLAG, true).apply();
//		}
	}

	private final void initBackgroundView(FrameLayout mainLayout) {
		bgView = new BackgroundView(this);
		mainLayout.addView(bgView);
		LayoutInflater.from(this).inflate(R.layout.view_talk, mainLayout);
		//下のViewにTouchEventを送るために上に透明Viewを配置
		mainLayout.addView(new View(this) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				bgView.onTouchEvent(event);
				return super.onTouchEvent(event);
			}
		});
	}

	private final void initFlipper(FrameLayout mainLayout) {
		ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
		flipper.addView(mainLayout);
		flipper.addView(inputView  = LayoutInflater.from(this).inflate(R.layout.view_input, null));
		flipper.addView(settingView = LayoutInflater.from(this).inflate(R.layout.view_setting, null));
		flipper.addView(settingTagView = LayoutInflater.from(this).inflate(R.layout.view_setting_tag, null));
	}

	private final void initGravitySensor() {
		//bgViewがあったら、重力センサーを登録
		if (bgView != null) {
			SensorManager sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			List<Sensor> sensors = sManager.getSensorList(Sensor.TYPE_GRAVITY);
			if (sensors.size() > 0) {
				Sensor s = sensors.get(0);
				sManager.registerListener(bgView, s, SensorManager.SENSOR_DELAY_UI);
			}
		}
	}

	private final void initTalkManager() {
		LinearLayout mainLayout = (LinearLayout)findViewById(R.id.left_draw);
		tvManager = new TalkManager(this, mainLayout);
		new ServerTask(this, "初期化に失敗しました") {
			@Override
			public void doInSubThread() throws SwallowException {
				//tvManagerを初期化
				try {
					tvManager.init();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected void onPostExecute(Boolean aBoolean) {
				//tvManager.initの終わったあとにされる処理
				tvManager.pushMessageList(); //このスレッドでやらないとまずい処理
				//タグ一覧表示ビューの設定
				initTagSelectList();
			}
		};

	}

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
		ImageButton button = (ImageButton)findViewById(R.id.detail_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tvManager.startPost();
				shiftToInputForm();
			}
		});
	}

	private final void initSubmitButton() {
		Button sendButton = (Button)findViewById(R.id.talk_submit_button);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//送信ボタンをタップしたときの処理
				EditText input = (EditText) findViewById(R.id.input_text_dummy);
				tvManager.startPost();
				String text = input.getText().toString();
				if (submitMessage(text, v)) {
					tvManager.endPost();
				}
			}
		});
	}

	private final void initTagSelectButton() {
		final Button tagButton = (Button)findViewById(R.id.tag_select_button);
		tagButton.setText(tvManager.getSelectedTagText());
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
				if (submitMessage(text, v)) {
					tvManager.endPost();
					//view_talkに戻る
					((ViewFlipper) findViewById(R.id.flipper)).showPrevious();
				}
			}
		});
	}

	private final void initFileUploadButton() {
		LinearLayout uploadLayout = (LinearLayout)findViewById(R.id.file_upload_button_layout);
		uploadLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//ファイルアップロードボタン押下時の処理
				showFileUploadDialog();
			}
		});
		uploadLayout.addView(tvManager.getFileClipView());
	}

	//ドロワーのタグリストを初期化
	private final void initTagSelectList() {
		final ListView tagSelectList = (ListView)findViewById(R.id.drawer_tag_select_list);
		tagSelectList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
									long arg3) {
				String tagName = (String) tagSelectList.getItemAtPosition(position);
				TagInfo selectedTag = tvManager.findVisibleTagByName(tagName);
				selectedTag.isSelected = !selectedTag.isSelected;
			}
		});
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
					for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
						tag.add(tvManager.findVisibleTagByIndex(i).tagName);
					}
				} else {
					for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
						String tagName = tvManager.findVisibleTagByIndex(i).tagName;
						if (tagName.startsWith(searchWord)) {
							tag.add(tagName);
						}
					}
				}
				String[] members = new String[tag.size()];
				for (int i = 0; i < members.length; i++)
					members[i] = tag.get(i);
				//tagSelectListに検索結果を適用
				ArrayAdapter<String> adapter = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				ListView tagSelectList = (ListView) findViewById(R.id.drawer_tag_select_list);
				tagSelectList.setAdapter(adapter);
				//選択されている文字列を羅列
				ArrayList<String> selectedTagNameList = new ArrayList<>();
				for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
					TagInfo t = tvManager.findVisibleTagByIndex(i);
					if (t.isSelected)
						selectedTagNameList.add(t.tagName);
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
		//Drawerを開いた時のタグ選択状況を一時保存する変数
		final ArrayList<Boolean> tmpList = new ArrayList<>();

		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
				drawer, R.string.drawer_open,
				R.string.drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				//Drawerが開いたとき

				updateTagSelectList();

				//開いた時のタグを保存
				{
					tmpList.clear();
					for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
						TagInfo t = tvManager.findVisibleTagByIndex(i);
						tmpList.add(t.isSelected);
					}
				}
				super.onDrawerOpened(drawerView);
			}
			@Override
			public void onDrawerClosed(View drawerView) {
				//ドロワーが閉じた時の処理
				//開いた時と選択状況が違ったら更新
				boolean flag = false;
				for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
					if (tmpList.get(i) != tvManager.findVisibleTagByIndex(i).isSelected) {
						flag = true;
						break;
					}
				}
				if (flag) {
					//開いたときと違ったら
					AsyncTask<Void, Void, ArrayList<MessageView>> task = new AsyncTask<Void, Void, ArrayList<MessageView>>() {
						@Override
						protected ArrayList<MessageView> doInBackground(Void... params) {
							//MessageViewのリストを更新
							try {
								return tvManager.refreshOnTagSelectChanged();
							} catch (SwallowException e) {
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							return null;
						}

						@Override
						protected void onPostExecute(ArrayList<MessageView> messageViews) {
							if (messageViews != null) {
								//更新に成功していたら、MessageViewを突っ込む
								LinearLayout layout = (LinearLayout)findViewById(R.id.left_draw);
								layout.removeAllViews();
								for (MessageView mv : messageViews)
									tvManager.addMessageViewToPrev(mv);
							} else {
								MyUtils.showShortToast(TalkActivity.this, "更新に失敗しました");
							}
						}
					};
					task.execute((Void)null);

					//タグの選択状況をプリファレンスに保存
					MyUtils.sp.edit().putString(MyUtils.SELECTED_TAG_KEY, tvManager.getSelectedTagIDText()).apply();
				}
				super.onDrawerClosed(drawerView);
			}
		};
		updateTagSelectList();
		toggle.setHomeAsUpIndicator(R.drawable.ic_drawer);
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
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				TagInfo clickedTag = tvManager.findVisibleTagByIndex(which);
				clickedTag.isSelected = !clickedTag.isSelected;
			}
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

		tagAddDialog = tagAddDialogBuilder.create();
	}

	private final void initEnqueteButton() {
		final ImageButton enqueteButton = (ImageButton)findViewById(R.id.enqueteButton);
		enqueteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showEnqueteDialog();
			}
		});
		//アンケートダイアログの設定
		enqueteDialogBuilder = new AlertDialog.Builder(TalkActivity.this);
		//タイトル設定
		enqueteDialogBuilder.setTitle(R.string.enquete_button_text);
		//ボタン設定
		enqueteDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		enqueteDialogBuilder.setNeutralButton(R.string.enquete_add_button_text, null);
		enqueteDialogBuilder.setNegativeButton("キャンセル", null);
		enqueteDialogBuilder.setCancelable(false);

		//タグ追加ダイアログの設定
		enqueteAddDialogBuilder = new AlertDialog.Builder(this);
		//タイトル設定
		enqueteAddDialogBuilder.setTitle(R.string.enquete_add_button_text);
		//ビューを設定
		final View view = View.inflate(TalkActivity.this, R.layout.dialog_enquete_add, null);
		enqueteAddDialogBuilder.setView(view);

		enqueteAddInput = (EditText)view.findViewById(R.id.enquete_add_input);

		enqueteAddDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,
								int which) {
				//アンケート追加確定ボタンが押されたときの処理
				onEnqueteAddDecideButtonPressed();
			}
		});
		//キャンセルボタンを置く
		enqueteAddDialogBuilder.setNegativeButton(R.string.cancel_text, null);

		enqueteDialogShowListener = new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button okButton = enqueteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				okButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						//アンケート選択確定ボタンの処理
						onEnqueteButtonPressed();
					}
				});
				Button addButton = enqueteDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
				addButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						//アンケート追加ボタンの処理
						onEnqueteAddButtonPressed();
					}
				});
				Button cancelButton = enqueteDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
				cancelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						enqueteDialog.dismiss();
						tvManager.clearEnquete();
					}
				});
			}
		};

		enqueteAddDialog = enqueteAddDialogBuilder.create();
	}

	private final void initReceivedCheckBox() {
		final CheckBox checkBox = (CheckBox)findViewById(R.id.checkReceivedBox);
		checkBox.setChecked(false);
	}

	private final void initSetting() {
		//プロフィール画像をクリックしたらギャラリーで選択できるようにする
		ImageView imageView = (ImageView)findViewById(R.id.setting_image);
		imageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("vnd.android.cursor.dir/image");
				startActivityForResult(intent, USER_IMAGE_CHANGE_CODE);
			}
		});

		//通知設定のボタンを押したら通知設定のViewに移行
		Button tagSettingButton = (Button)findViewById(R.id.setting_to_tag_button);
		tagSettingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
				flipper.setInAnimation(input_in_animation);
				flipper.setOutAnimation(talk_out_animation);
				flipper.setDisplayedChild(3);
			}
		});

		//タグ設定終了ボタンが押されたら普通の設定Viewに移行
		Button tagSettingFinishButton = (Button)findViewById(R.id.setting_tag_finish_button);
		tagSettingFinishButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
				flipper.setInAnimation(talk_in_animation);
				flipper.setOutAnimation(input_out_animation);
				flipper.setDisplayedChild(2);
			}
		});

		//設定終了ボタンが押されたら、設定完了
		Button settingFinishButton = (Button)findViewById(R.id.setting_finish_button);
		settingFinishButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//決定ボタンが押されたとき
				AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

					@Override
					protected String doInBackground(Void[] params) {
						String userName = ((TextView)findViewById(R.id.setting_name)).getText().toString();
						String profile = ((TextView)findViewById(R.id.setting_profile)).getText().toString();
						String twitterText = ((TextView)findViewById(R.id.setting_twitter)).getText().toString();
						String mailText1 = ((TextView)findViewById(R.id.setting_mail1)).getText().toString();
						String mailText2 = ((TextView)findViewById(R.id.setting_mail2)).getText().toString();
						String password = ((TextView)findViewById(R.id.setting_password)).getText().toString();
						String passsword_again = ((TextView)findViewById(R.id.setting_password_again)).getText().toString();
						if (userName != null && userName.length() == 0)
							userName = null;
						if (profile != null && profile.length() == 0)
							profile = null;
						if (twitterText != null && twitterText.length() == 0)
							twitterText = null;
						if (mailText1 != null && mailText1.length() == 0)
							mailText1 = null;
						if (mailText2 != null && mailText2.length() == 0)
							mailText2 = null;
						if (password != null && password.length() == 0)
							password = null;
						if (passsword_again != null && passsword_again.length() == 0)
							passsword_again = null;
						if (password != null && passsword_again != null) {
							if (password.equals(passsword_again) == false) {
								//パスワードと再入力が一致しなかったら
								return "２つのパスワードが一致しません";
							}
						}
						//プロフィール画像の決定
						Integer imageID = null;
						if (userImageFile != null) {
							try {
								imageID = userImageFile.send().getFileID();
							} catch (SwallowException e) {
								if (((SwallowException)e.getCause()).getServerMessage().equals("SQLSTATE[08S01]: Communication link failure: 1153 Got a packet bigger than 'max_allowed_packet' bytes")) {
									//画像サイズでかすぎぃ！
								}
								e.printStackTrace();
							}
							userImageFile = null;
						}
						Swallow.UserDetail user = (Swallow.UserDetail)tvManager.getMyUserInfo().user;
						if (userName.equals(user.getUserName()) == false
						|| profile.equals(user.getProfile()) == false
								|| twitterText.equals(user.getTwitter()) == false
								|| mailText1.equals(user.getEmail()) == false
								|| mailText2.equals(user.getEmail()) == false
								|| imageID.equals(user.getImage()) == false
								|| password != null) {
							try {
								//サーバーに変更を送信
								tvManager.setMyUserInfo(SCM.scm.swallow.modifyUser(
										userName, profile, imageID, password, mailText1 + "@" + mailText2, null, twitterText, null, null,
										null, null));
								tvManager.refreshOnUserInfoChanged_1();
							} catch (SwallowException e) {
								e.printStackTrace();
								return "サーバーとの通信に失敗しました";
							}
						}
						//幼女設定をPreferenceへ
						boolean y = ((CheckBox) findViewById(R.id.setting_yojo)).isChecked();
						MyUtils.sp.edit().putBoolean(MyUtils.YOJO_CHECK_KEY,y
						).apply();
						//背景設定をPreferenceへ
						boolean bg = ((CheckBox)findViewById(R.id.setting_background)).isChecked();
						MyUtils.sp.edit().putBoolean(MyUtils.BACKGROUND_ENABLE_KEY, bg).apply();
						bgView.enable = bg;
						//NotifyをPreferenceへ
						ListView notifyTagList = (ListView) findViewById(R.id.setting_tag_list);
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < tvManager.getVisibleTagNum(); i++) {
							if (notifyTagList.isItemChecked(i)) {
								sb.append(tvManager.findVisibleTagByIndex(i).tagID);
								sb.append(',');
							}
						}
						if (sb.length() > 0)
							sb.deleteCharAt(sb.length()-1);
						MyUtils.sp.edit().putString(MyUtils.NOTIFY_TAG_KEY, sb.toString()).apply();
						return null;
					}

					@Override
					protected void onPostExecute(String str) {
						if (str == null) {
							tvManager.refreshOnUserInfoChanged_2();
							shiftToTalk();
						} else {
							MyUtils.showShortToast(TalkActivity.this, str);
						}
					}
				};
				task.execute((Void)null);
			}
		});
	}

	private final boolean submitMessage(final String text, final View v) {
		if (text.length() > 0 || tvManager.hasFile() || tvManager.hasEnquete()) {
			//投稿
			AsyncTask<Void, Void, MessageView> task = new AsyncTask<Void, Void, MessageView>() {
				@Override
				protected MessageView doInBackground(Void... params) {
					try {
						return tvManager.submit(text);
					} catch (SwallowException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				protected void onPostExecute(MessageView messageView) {
					if (messageView != null) {
						if (tvManager.isEditMode()) {
							//編集中なら
							tvManager.changeMessageView(messageView);
							tvManager.endEdit();
						} else {
							//普通の投稿なら
							tvManager.addMessageViewToNext(messageView);
							//スクロール
//							scrollView.fullScroll(ScrollView.FOCUS_DOWN);
							MyUtils.scrollDown();

							tvManager.endPost();
						}
						//キーボードを隠す
						hideKeyboard(v);
					}
					else
						MyUtils.showShortToast(TalkActivity.this, "メッセージの送信に失敗しました");
				}
			};
			task.execute((Void) null);
			return true;
		}
		return false;
	}

	private final void showTagSelectDialog() {
		//タグリストをStringの配列にする
		final String[] items = new String[tvManager.getVisibleTagNum()];
		for (int i = 0 ; i < items.length; i++)
			items[i] = tvManager.findVisibleTagByIndex(i).tagName;
		boolean[] selectionList = new boolean[items.length];
		for (int i = 0; i < selectionList.length; i++)
			selectionList[i] = tvManager.findVisibleTagByIndex(i).isSelected;
		tagSelectDialogBuilder.setMultiChoiceItems(items, selectionList,
				tagChoiceListener) ;
		//ダイアログを作成
		tagSelectDialog = tagSelectDialogBuilder.create();
		tagSelectDialog.setOnShowListener(tagSelectDialogShowListener);
		tagSelectDialog.show();
	}

	private final void showEnqueteDialog() {
		//アンケート選択肢を追加
		enqueteDialogBuilder.setItems(tvManager.getEnqueteArray(), null);
		//ダイアログを作成
		enqueteDialog = enqueteDialogBuilder.create();
		enqueteDialog.setOnShowListener(enqueteDialogShowListener);
		enqueteDialog.show();
	}

	private final void showFileUploadDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(TalkActivity.this);
		final String[] item;
		if (tvManager.hasFile()) {
			item = new String[]{"ギャラリーから選択", "ファイルを選択", "ファイルを削除"};
		} else {
			item = new String[]{"ギャラリーから選択", "ファイルを選択"};
		}
		b.setItems(item, new DialogInterface.OnClickListener() {
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
					case 2: //ファイルを削除
						AlertDialog.Builder b = new AlertDialog.Builder(TalkActivity.this);
						String[] items = new String[tvManager.getFileNum()];
						for (int i = 0; i < items.length; i++)
							items[i] = tvManager.getFile(i).fileName;
						b.setItems(items, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, final int selected) {
								AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(TalkActivity.this);
								confirmBuilder.setTitle("除去してよろしいですか？");
								confirmBuilder.setPositiveButton("hai", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										tvManager.removeFile(selected);
									}
								});
								confirmBuilder.setNegativeButton("iie", null);
								confirmBuilder.show();
							}
						});
						b.show();
				}
				dialog.dismiss();
			}
		});
		b.show();
	}

	public final void shiftToInputForm() {
		//tagSelectButtonを設定
		{
			Button tagSelectButton = (Button)findViewById(R.id.tag_select_button);
			tagSelectButton.setText(tvManager.getSelectedTagText());
		}
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

	private final void shiftToTalk() {
		ViewFlipper vf = (ViewFlipper)findViewById(R.id.flipper);
		vf.setInAnimation(talk_in_animation);
		vf.setOutAnimation(input_out_animation);
		vf.setDisplayedChild(0);
	}

	private final void shiftToSetting() {
		//ViewFlipperにAnimationを設定
		ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
		flipper.setInAnimation(input_in_animation);
		flipper.setOutAnimation(talk_out_animation);

		//設定画面を初期化

		Swallow.UserDetail myself = (Swallow.UserDetail)tvManager.getMyUserInfo().user;

		//プロフィール画像を設定
		ImageView imageView = (ImageView)findViewById(R.id.setting_image);
		if (tvManager.getMyUserInfo().user.getImage() != null) {
			imageView.setImageBitmap(tvManager.getMyUserInfo().profileImage);
		} else {
			imageView.setImageResource(R.drawable.person);
		}
		//名前を設定
		EditText nameView = (EditText)findViewById(R.id.setting_name);
		nameView.setText(myself.getUserName());
		//Twitterを設定
		EditText twitterView = (EditText)findViewById(R.id.setting_twitter);
		twitterView.setText(myself.getTwitter());
		//メアドを設定
		{
			String[] str = myself.getEmail().split("@");
			EditText mailView1 = (EditText) findViewById(R.id.setting_mail1);
			mailView1.setText(str[0]);
			EditText mailView2 = (EditText) findViewById(R.id.setting_mail2);
			mailView2.setText(str[1]);
		}
		//プロフィールを設定
		EditText profileView = (EditText)findViewById(R.id.setting_profile);
		profileView.setText(myself.getProfile());
		//パスワード部分を初期化
		EditText passwordView = (EditText)findViewById(R.id.setting_password);
		passwordView.setText("");
		EditText passwordAgainView = (EditText)findViewById(R.id.setting_password_again);
		passwordAgainView.setText("");
		//幼女を設定
		CheckBox yojoCheck = (CheckBox)findViewById(R.id.setting_yojo);
		boolean y = MyUtils.sp.getBoolean(MyUtils.YOJO_CHECK_KEY, false);
		yojoCheck.setChecked(y);
		//背景を設定
		CheckBox bgCheck = (CheckBox)findViewById(R.id.setting_background);
		boolean bg = MyUtils.sp.getBoolean(MyUtils.BACKGROUND_ENABLE_KEY, false);
		bgCheck.setChecked(bg);
		//通知リストを設定
		ListView notifyTagList = (ListView)findViewById(R.id.setting_tag_list);
		String[] member = new String[tvManager.getVisibleTagNum()];
		for (int i = 0; i < member.length; i++)
			member[i] = tvManager.findVisibleTagByIndex(i).tagName;
		notifyTagList.setAdapter(new ArrayAdapter<>(TalkActivity.this,
				android.R.layout.simple_list_item_multiple_choice, member));
		//通知リストのチェックを設定
		for (String notifyTagIDStr : MyUtils.sp.getString(MyUtils.NOTIFY_TAG_KEY, null).split(",")) {
			int index = -1;
			for (int j = 0; j < tvManager.getVisibleTagNum(); j++) {
				if (notifyTagIDStr.equals(Integer.toString(tvManager.findVisibleTagByIndex(j).tagID))) {
					index = j;
					break;
				}
			}
			notifyTagList.setItemChecked(index, index != -1);
		}
		flipper.setDisplayedChild(2);
	}

	private final void editMessage(int postId) {
		tvManager.startEdit(postId);
	}

	private final void deleteMessage(final int postId) {
		final LinearLayout layout = (LinearLayout)findViewById(R.id.left_draw);
		//削除するMessageViewを検索
		for (int i = 0; i < layout.getChildCount(); i++) {
			View child = layout.getChildAt(i);
			if (child instanceof MessageView) {
				final View child2 = layout.getChildAt(i+1);
				final MessageView mv = (MessageView)child;
				if (mv.mInfo.getPostID() == postId) {
					new ServerTask(this, "削除失敗") {
						@Override
						public void doInSubThread() throws SwallowException {
							SCM.scm.deleteMessage(postId);
						}

						@Override
						protected void onPostExecute(Boolean aBoolean) {
							if (aBoolean) {
								layout.removeView(mv);
								layout.removeView(child2);
							}
						}
					};
					break;
				}
			}
		}
	}

	private final void showKeyboard(final EditText edit) {
		//キーボードの表示
		Runnable r = new Runnable() {
			@Override
			public void run() {
				((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).
						showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
			}
		};
		new Handler().post(r);
	}

	protected final void hideKeyboard(View v) {
		//キーボードを隠す
		((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(v.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private final void updateTagSelectList() {
		//tagSelectListを更新
		{
			ListView tagSelectList = (ListView) findViewById(R.id.drawer_tag_select_list);
			final String[] members = new String[tvManager.getVisibleTagNum()];
			{
				//adapterを生成・設定。
				for (int i = 0; i < members.length; i++)
					members[i] = tvManager.findVisibleTagByIndex(i).tagName;
				ArrayAdapter<String> tagList = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				tagSelectList.setAdapter(tagList);
			}
			//tagSelectListに選択状況を適用
			for (int i = 0; i < members.length; i++) {
				tagSelectList.setItemChecked(i, tvManager.findVisibleTagByIndex(i).isSelected);
			}
			//クリック時の動作を設定
			tagSelectList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//タグの選択状況を反転
					TagInfo clickedTag = tvManager.findVisibleTagByName(members[position]);
					clickedTag.isSelected = !clickedTag.isSelected;
				}
			});
		}

	}

	private final void onTagSelectButtonPressed() {
		Button tagButton = (Button)TalkActivity.this.findViewById(R.id.tag_select_button);
		tagButton.setText(tvManager.getSelectedTagText());
		tagSelectDialog.dismiss(); //alertDialog抹消
	}

	private final void onTagAddButtonPressed() {
		//タグ追加ダイアログを表示

		//キーボード表示
		showKeyboard(tagAddInput);

		//テキストボックスを空にする
		tagAddInput.setText("");

		//タグ追加ダイアログを表示
		tagAddDialog.show();
	}

	private final void onTagAddDecideButtonPressed() {
		final String tag = tagAddInput.getText().toString().trim();
		//何か入力されていて、まだそのタグがなかった場合
		if (tag.length() != 0 && tvManager.findVisibleTagByName(tag) == null) {
			//tagListに新たなタグを追加
			new ServerTask(this, "タグの追加に失敗しました") {
				@Override
				public void doInSubThread() throws SwallowException {
					int tagID = SCM.scm.sendAddTag(tag, false);
					TagInfo newTag = new TagInfo(tag, tagID);
					tvManager.addVisibleTag(newTag);
					newTag.isSelected = true;
				}

				@Override
				protected void onPostExecute(Boolean aBoolean) {
					if (aBoolean) {
						//タグ選択ダイアログを再表示
						tagSelectDialog.dismiss();
						showTagSelectDialog();
					}
					super.onPostExecute(aBoolean);
				}
			};
		}
	}

	private final void onEnqueteButtonPressed() {
		enqueteDialog.dismiss(); //alertDialog抹消
	}

	private final void onEnqueteAddButtonPressed() {
		//アンケート追加ダイアログを表示

		//キーボード表示
		showKeyboard(enqueteAddInput);

		//テキストボックスを空にする
		enqueteAddInput.setText("");

		//アンケート追加ダイアログを表示
		enqueteAddDialog.show();
	}

	private final void onEnqueteAddDecideButtonPressed() {
		String enquete = enqueteAddInput.getText().toString().trim();
		//何か入力されていたら
		if (enquete != null && enquete.length() != 0) {
			tvManager.addEnquete(enquete);

			//アンケート選択ダイアログを再表示
			enqueteDialog.dismiss();
			showEnqueteDialog();
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
