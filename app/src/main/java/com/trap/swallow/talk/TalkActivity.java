package com.trap.swallow.talk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.trap.swallow.gcm.RegistrationIntentService;
import com.trap.swallow.info.FileInfo;
import com.trap.swallow.info.TagInfoManager;
import com.trap.swallow.info.UserInfoManager;
import com.trap.swallow.login.LogInActivity;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.server.SwallowSecurity;
import com.trap.swallow.swallow.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class TalkActivity extends AppCompatActivity implements SensorEventListener {

	public static TalkActivity singleton;

	private final static int CHOICE_CODE = 12345;
	private static final int USER_IMAGE_CHANGE_CODE = 25252;

	private static final int REFLESH_BUTTON_ID = 0;
	private static final int UP_BUTTON_ID = 1;
	private static final int DOWN_BUTTON_ID = 2;
	private static final int SETTING_BUTTON_ID = 3;
	private static final int LOGOUT_BUTTON_ID = 4;
	private static final int PASTE_BUTTON_ID = 5;
	private static final int SCHEDULE_BUTTON_ID = 6;

	public ListView scrollView;

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
	private AlertDialog.Builder tagDeleteDialogBuilder;
	private AlertDialog tagDeleteDialog;
	private OnMultiChoiceClickListener tagDeleteChoiceListener;
	private ArrayList<TagInfoManager.TagInfo> deleteTargetTag = new ArrayList<>();
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
	private View mainView;
	private BackgroundView bgView;
	private FileInfo userImageFile; //設定変更時の一時保存用
	private final ArrayList<Boolean> tmpList = new ArrayList<>();
	private TimerTask timerTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//Singletonを設定
		singleton = this;

		ProgressDialog progressDialog = MyUtils.createPorgressDialog();
		progressDialog.show();
		MessageViewAdapter.progressDialog = progressDialog;

		//GCMへの登録
		initGCM();

		//xmlの内容を適用
		setContentView(R.layout.activity_talk);

		//タイトルを消す
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		//いろいろ置くためのLayout
		FrameLayout mainLayout = new FrameLayout(this);

		initBackgroundView(mainLayout);
		initFlipper(mainLayout);
		this.mainView = mainLayout;
		initSensor();
		scrollView = (ListView)findViewById(R.id.talk_scroll_view);
		initTalkManager();
		initAnimations();
		initInputText();
		initDetailButton();
		initSubmitButton();
		initInputBoxOnTalkView();
		initTagSelectButton();
		initSendMessageButton();
		initFileUploadButton();
		initTagSearchBox();
		initUserSearchBox();
		initPrivateSearchBox();
		initDrawer();
		initTagSelectDialog();
		initEnqueteButton();
		initReceivedCheckBox();
		initInsertCodeButton();
		initSetting();
		initRefreshWidget();
		initPrefernceDeleteButton();

		MyUtils.scrollDown();

		//別スレッドを起動
		Timer t = new Timer();
		t.schedule(timerTask = new Task(), 0, 1000);
	}

	@Override
	protected void onRestart() {
		Timer t = new Timer();
		t.schedule(timerTask = new Task(), 0, 1000);
		new ServerTask(this, "") {
			@Override
			public void doInSubThread() throws SwallowException {
				setObserveForRunning();
			}
		};
		super.onRestart();
	}

	@Override
	protected void onStop() {
		if (timerTask != null)
			timerTask.cancel();
		try {
			SCM.swallow.modifyUser(null, null, null, null, null, TagInfoManager.getNotificaionTagID(), null);
		} catch (SwallowException e) {
			e.printStackTrace();
		}
		super.onStop();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO 自動生成されたメソッド・スタブ
		//ホームボタンなら
		switch (item.getItemId()) {
			case android.R.id.home:

				break;
			case REFLESH_BUTTON_ID:
				final ProgressDialog progressDialog = MyUtils.createPorgressDialog();
				progressDialog.show();
				new ServerTask(this, "更新失敗") {
					@Override
					public void doInSubThread() throws SwallowException {
						TalkManager.refreshOnUserInfoChanged_1();
					}

					@Override
					protected void onPostExecute(Boolean aBoolean) {
						TalkManager.refreshOnUserInfoChanged_2();
						progressDialog.dismiss();
					}
				};
				break;
			case UP_BUTTON_ID:
				MyUtils.scrollUp();
				break;
			case DOWN_BUTTON_ID:
				MyUtils.scrollDown();
				break;
			case SETTING_BUTTON_ID:
				shiftToSetting();
				break;
			case LOGOUT_BUTTON_ID:
				logout();
				break;
			case PASTE_BUTTON_ID:
				paste();
				break;
			case SCHEDULE_BUTTON_ID:
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
			if (mv.mInfo.getUserID() == UserInfoManager.getMyUserInfo().user.getUserID()) {
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

				refreshOnTagSeletChange();

				if (TalkManager.isReplyMode()) {
					//リプ中なら
					TalkManager.endReply();
				}
				if (TalkManager.isEditMode()) {
					//編集中なら
					TalkManager.endEdit();
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case CHOICE_CODE:
					try {
						//受け取ったデータのパスを取得
						Uri uri = data.getData();

						FileInfo file = new FileInfo(getContentResolver(), getResources(), uri);
						TalkManager.addFileToPost(file);
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

						FileInfo file = new FileInfo(getContentResolver(), getResources(), uri);
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
		MenuItem item5 = menu.add(0, LOGOUT_BUTTON_ID, 4, "ログアウト");
		MenuItem item6 = menu.add(0, PASTE_BUTTON_ID, 5, "貼り付け")
				.setIcon(R.drawable.abc_ic_menu_paste_mtrl_am_alpha);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(item2, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		MenuItemCompat.setShowAsAction(item3, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(item4, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(item5, MenuItemCompat.SHOW_AS_ACTION_NEVER);
		MenuItemCompat.setShowAsAction(item6, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
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
		flipper.addView(inputView = LayoutInflater.from(this).inflate(R.layout.view_input, null));
		flipper.addView(settingView = LayoutInflater.from(this).inflate(R.layout.view_setting, null));
		flipper.addView(settingTagView = LayoutInflater.from(this).inflate(R.layout.view_setting_tag, null));
	}

	private final void initSensor() {
		//bgViewがあったら、重力センサーを登録
		if (bgView != null) {
			SensorManager sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			List<Sensor> sensors = sManager.getSensorList(Sensor.TYPE_GRAVITY);
			if (sensors.size() > 0) {
				Sensor s = sensors.get(0);
				sManager.registerListener(bgView, s, SensorManager.SENSOR_DELAY_UI);
				sManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
			}
			sensors = sManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				Sensor s = sensors.get(0);
				sManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
			}
		}
	}

	private final void initTalkManager() {
		TalkManager.start(this);
		new ServerTask(this, "初期化に失敗しました") {
			@Override
			public void doInSubThread() throws SwallowException {
				//tvManagerを初期化
				try {
					TalkManager.init();
					setObserveForRunning();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}

			@Override
			protected void onPostExecute(Boolean aBoolean) {
				//TalkManager.initの終わったあとにされる処理
				TalkManager.removeAllMessageViews();
				TalkManager.pushMessageList(); //このスレッドでやらないとまずい処理
				//タグ一覧表示ビューの設定
				initTagSelectList();
				initUserSelectList();
				initPrivateSelectList();
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
				TalkManager.startPost();
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
				TalkManager.startPost();
				String text = input.getText().toString();
				if (submitMessage(text, v)) {
					TalkManager.endPost();
				}
			}
		});
	}

	private final void initTagSelectButton() {
		final Button tagButton = (Button)findViewById(R.id.tag_select_button);
		tagButton.setText(TagInfoManager.getSelectedTagText());
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
					TalkManager.endPost();
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
	}

	private final void initTagSelectList() {
		final ListView tagSelectList = (ListView)findViewById(R.id.drawer_tag_select_list);
		tagSelectList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
									long arg3) {
				String tagName = (String) tagSelectList.getItemAtPosition(position);
				TagInfoManager.TagInfo selectedTag = TagInfoManager.findTagByName(tagName);
				TagInfoManager.selectTag(selectedTag);
			}
		});
	}

	private final void initDrawerContent() {
		ExpandableListView tagList = (ExpandableListView)findViewById(R.id.drawer_expandable);
		List<Map<String, View>> parent = new ArrayList<>();
	}

	private final void initTagSearchBox() {
		final EditText searchTagBox = (EditText)findViewById(R.id.drawer_tag_search_box);
		searchTagBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String searchWord = searchTagBox.getText().toString();
				boolean emptyFlag = searchWord.length() == 0;

				ArrayList<String> tag = new ArrayList<>();

				//タグリストの中から先頭が一致するものを検索
				if (emptyFlag) {
					for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
						tag.add(TagInfoManager.findTagByIndex(i, true).tag.getTagName());
					}
				} else {
					for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
						String tagName = TagInfoManager.findTagByIndex(i, true).tag.getTagName();
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
				for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
					TagInfoManager.TagInfo t = TagInfoManager.findTagByIndex(i, true);
					if (t.isSelected())
						selectedTagNameList.add(t.tag.getTagName());
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

	private final void initUserSelectList() {
//		final ListView userSelectList = (ListView)findViewById(R.id.drawer_user_select_list);
//		userSelectList.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
//									long arg3) {
//				String userName = (String) userSelectList.getItemAtPosition(position);
//				final UserInfo userInfo = UserInfoManager.findUserByName(userName);
//				((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawers();
////				ProgressDialog progressDialog = MyUtils.createPorgressDialog();
//				final ArrayList<MessageView> messageViewArrayList = new ArrayList<MessageView>();
//				new ServerTask(TalkActivity.this, "読み込みに失敗しました") {
//					@Override
//					public void doInSubThread() throws SwallowException {
//						SCM.loadUserMessageToList(messageViewArrayList, userInfo.user.getUserID());
//					}
//
//					@Override
//					protected void onPostExecute(Boolean aBoolean) {
//						if (aBoolean) {
//							TalkManager.removeAllMessageViews();
//							for (MessageView mv : messageViewArrayList) {
//								TalkManager.addMessageViewToPrev(mv);
//								mv.initOnMainThread();
//							}
//						}
//					}
//				}
//			}
//		});
	}

	private final void initUserSearchBox() {
		final EditText searchTagBox = (EditText)findViewById(R.id.drawer_user_search_box);
		searchTagBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String searchWord = searchTagBox.getText().toString();
				boolean emptyFlag = searchWord.length() == 0;
				//				searchWord = "^" + searchWord;

				ArrayList<String> user = new ArrayList<>();

				//タグリストの中から先頭が一致するものを検索
				if (emptyFlag) {
					for (int i = 0; i < UserInfoManager.getUserNum(); i++) {
						user.add(UserInfoManager.findUserByIndex(i).user.getUserName());
					}
				} else {
					for (int i = 0; i < UserInfoManager.getUserNum(); i++) {
						String userName = UserInfoManager.findUserByIndex(i).user.getUserName();
						if (userName.startsWith(searchWord)) {
							user.add(userName);
						}
					}
				}
				String[] members = new String[user.size()];
				for (int i = 0; i < members.length; i++)
					members[i] = user.get(i);
				//userSelectListに検索結果を適用
				ArrayAdapter<String> adapter = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				ListView userSelectList = (ListView) findViewById(R.id.drawer_user_select_list);
				userSelectList.setAdapter(adapter);
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

	private final void initPrivateSelectList() {
		final ListView tagSelectList = (ListView)findViewById(R.id.drawer_private_select_list);
		tagSelectList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
									long arg3) {
				String tagName = (String) tagSelectList.getItemAtPosition(position);
				TagInfoManager.TagInfo selectedTag = TagInfoManager.findTagByName(tagName);
				TagInfoManager.selectTag(selectedTag);
			}
		});
	}

	private final void initPrivateSearchBox() {
		final EditText searchTagBox = (EditText)findViewById(R.id.drawer_private_search_box);
		searchTagBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String searchWord = searchTagBox.getText().toString();
				boolean emptyFlag = searchWord.length() == 0;

				ArrayList<String> tag = new ArrayList<>();

				//タグリストの中から先頭が一致するものを検索
				if (emptyFlag) {
					for (int i = 0; i < TagInfoManager.getGroupTagNum(); i++) {
						tag.add(TagInfoManager.findGroupTagByIndex(i).tag.getTagName());
					}
				} else {
					for (int i = 0; i < TagInfoManager.getGroupTagNum(); i++) {
						String tagName = TagInfoManager.findGroupTagByIndex(i).tag.getTagName();
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
				ListView tagSelectList = (ListView) findViewById(R.id.drawer_private_select_list);
				tagSelectList.setAdapter(adapter);
				//選択されている文字列を羅列
				ArrayList<String> selectedTagNameList = new ArrayList<>();
				for (int i = 0; i < TagInfoManager.getGroupTagNum(); i++) {
					TagInfoManager.TagInfo t = TagInfoManager.findGroupTagByIndex(i);
					if (t.isSelected())
						selectedTagNameList.add(t.tag.getTagName());
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
				//Drawerが開いたとき

				updateTagSelectList();
				updateUserSelectList();
				updatePrivateSelectList();

				saveTagSelect();

				super.onDrawerOpened(drawerView);
			}
			@Override
			public void onDrawerClosed(View drawerView) {
				//ドロワーが閉じた時の処理
				refreshOnTagSeletChange();

				super.onDrawerClosed(drawerView);
			}
		};
		updateTagSelectList();
		updatePrivateSelectList();
		updateUserSelectList();
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
		tagSelectDialogBuilder.setNegativeButton("タグ削除♡", null);


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
				TagInfoManager.TagInfo clickedTag = TagInfoManager.findTagByIndex(which, true);
				TagInfoManager.selectTag(clickedTag);
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
				Button deleteButton = tagSelectDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
				deleteButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						tagDeleteDialog.show();
					}
				});
			}
		};

		tagAddDialog = tagAddDialogBuilder.create();

		tagDeleteDialogBuilder = new AlertDialog.Builder(this);
		tagDeleteDialogBuilder.setTitle("タグの削除");
		tagDeleteDialogBuilder.setPositiveButton("消しますとも", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder confirmDialogBuilder = new AlertDialog.Builder(TalkActivity.this);
				confirmDialogBuilder.setTitle("本当に？");
				confirmDialogBuilder.setMessage("本当に削除してよろしいですか？");
				confirmDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new ServerTask(TalkActivity.this, "タグの削除に失敗しました") {
							@Override
							public void doInSubThread() throws SwallowException {
								for (TagInfoManager.TagInfo tagInfo : deleteTargetTag)
									SCM.swallow.createTag(null, null, null, tagInfo.tag.getTagID());
								TagInfoManager.reload();
							}

							@Override
							protected void onPostExecute(Boolean aBoolean) {
								if (aBoolean) {
									deleteTargetTag.clear();
								}
							}
						};
					}
				});
				confirmDialogBuilder.setNegativeButton(R.string.cancel_text, null);
				confirmDialogBuilder.show();
			}
		});
		tagDeleteDialogBuilder.setNegativeButton(R.string.cancel_text, null);

		tagDeleteDialog = tagDeleteDialogBuilder.create();

		tagDeleteChoiceListener = new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				TagInfoManager.TagInfo tagInfo = TagInfoManager.findTagByIndex(which, true);
				if (deleteTargetTag.contains(tagInfo)) {
					deleteTargetTag.remove(tagInfo);
				} else {
					deleteTargetTag.add(tagInfo);
				}
			}
		};
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
						TalkManager.clearEnquete();
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

	private final void initInsertCodeButton() {
		final Button codeInsertButton = (Button)findViewById(R.id.codeInsertButton);
		codeInsertButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText input = (EditText) findViewById(R.id.input_text);
				int selection = input.getSelectionStart();
				if (selection == -1) {
					input.setText(input.getText() + "``````");
				} else {
					String str = input.getText().toString();
					input.setText(str.substring(0, selection) + "``````" + str.substring(selection, str.length()));
				}
				input.setSelection(selection + 3);
			}
		});
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
						String userName = ((TextView) findViewById(R.id.setting_name)).getText().toString();
						String profile = ((TextView) findViewById(R.id.setting_profile)).getText().toString();
						String mailText1 = ((TextView) findViewById(R.id.setting_mail1)).getText().toString();
						String mailText2 = ((TextView) findViewById(R.id.setting_mail2)).getText().toString();
						String password = ((TextView) findViewById(R.id.setting_password)).getText().toString();
						String passsword_again = ((TextView) findViewById(R.id.setting_password_again)).getText().toString();
						if (userName != null && userName.length() == 0)
							userName = null;
						if (profile != null && profile.length() == 0)
							profile = null;
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
								if (((SwallowException) e.getCause()).getServerMessage().equals("SQLSTATE[08S01]: Communication link failure: 1153 Got a packet bigger than 'max_allowed_packet' bytes")) {
									//画像サイズでかすぎぃ！
								}
								e.printStackTrace();
							}
							userImageFile = null;
						}
						Swallow.UserDetail user = (Swallow.UserDetail) UserInfoManager.getMyUserInfo().user;
						if ((userName != null && userName.equals(user.getUserName()) == false)
								|| (profile != null && profile.equals(user.getProfile()) == false)
								|| (mailText1 != null && mailText1.equals(user.getEmail()) == false)
								|| (mailText2 != null && mailText2.equals(user.getEmail()) == false)
								|| (imageID != null && imageID.equals(user.getImage()) == false)
								|| password != null) {
							try {
								//サーバーに変更を送信
								SCM.swallow.modifyUser(userName, profile, imageID, password, mailText1 + "@" + mailText2, null, null);
								TalkManager.refreshOnUserInfoChanged_1();
							} catch (SwallowException e) {
								e.printStackTrace();
								return "サーバーとの通信に失敗しました";
							}
						}
						//幼女設定をPreferenceへ
						boolean y = ((CheckBox) findViewById(R.id.setting_yojo)).isChecked();
						MyUtils.sp.edit().putBoolean(MyUtils.YOJO_CHECK_KEY, y
						).apply();
						//背景設定をPreferenceへ
						boolean bg = ((CheckBox) findViewById(R.id.setting_background)).isChecked();
						MyUtils.sp.edit().putBoolean(MyUtils.BACKGROUND_ENABLE_KEY, bg).apply();
						bgView.enable = bg;
						//NotifyをPreferenceへ
						ListView notifyTagList = (ListView) findViewById(R.id.setting_tag_list);
						ArrayList<String> list = new ArrayList<>();
						for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
							if (notifyTagList.isItemChecked(i)) {
								list.add(TagInfoManager.findTagByIndex(i, true).tag.getTagName());
							}
						}
						TagInfoManager.setNotification(list.toArray(new String[0]));
						return null;
					}

					@Override
					protected void onPostExecute(String str) {
						if (str == null) {
							TalkManager.refreshOnUserInfoChanged_2();
						} else {
							MyUtils.showShortToast(TalkActivity.this, str);
						}
					}
				};
				task.execute((Void) null);
				shiftToTalk();
			}
		});
	}

	private final void initRefreshWidget() {
		SwipeRefreshLayout mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
		mSwipeRefreshWidget.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				AsyncTask<Void, Void, ArrayList<MessageView>> task = new AsyncTask<Void, Void, ArrayList<MessageView>>() {
					@Override
					protected ArrayList<MessageView> doInBackground(Void... params) {
						return TalkManager.loadPreviousMessage();
					}

					@Override
					protected void onPostExecute(ArrayList<MessageView> messageViews) {
						if (messageViews != null) {
							for (MessageView mv : messageViews)
								TalkManager.addMessageViewToPrev(mv);
							((SwipeRefreshLayout) TalkActivity.singleton.findViewById(R.id.swipe_refresh_widget)).setRefreshing(false);
							MessageViewAdapter.afterPrevAdd();
						}
					}
				};
				task.execute((Void) null);
			}
		});
	}

	private final void initPrefernceDeleteButton() {
		Button pDeleteButton = (Button)findViewById(R.id.preference_delete_button);
		pDeleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(TalkActivity.this);
				builder.setMessage("本当に削除してよろしいですか？");
				builder.setNegativeButton(R.string.cancel_text, null);
				builder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyUtils.sp.edit().clear().commit();
						logout();
					}
				});
				builder.show();
			}
		});
	}

	private final boolean submitMessage(final String text, final View v) {
		if (canSend(text)) {
			//投稿
			new ServerTask(this, "送信に失敗しました") {
				@Override
				public void doInSubThread() throws SwallowException {
					TalkManager.submit(text);
				}

				@Override
				protected void onPostExecute(Boolean aBoolean) {
					if (aBoolean) {
						TalkManager.endPost();
						hideKeyboard(v);
					}
				}
			};
			return true;
		}
		return false;
	}

	private final void showTagSelectDialog() {
		//タグリストをStringの配列にする
		final String[] items = new String[TagInfoManager.getVisibleTagNum() + TagInfoManager.getGroupTagNum()];
		for (int i = 0 ; i < TagInfoManager.getVisibleTagNum(); i++)
			items[i] = TagInfoManager.findTagByIndex(i, true).tag.getTagName();
		for (int i = 0 ; i < TagInfoManager.getGroupTagNum(); i++)
			items[i+TagInfoManager.getVisibleTagNum()] = TagInfoManager.findGroupTagByIndex(i).tag.getTagName();

		boolean[] selectionList = new boolean[items.length];
		for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++)
			selectionList[i] = TagInfoManager.findTagByIndex(i, true).isSelected();
		for (int i = 0; i < TagInfoManager.getGroupTagNum(); i++)
			selectionList[i+TagInfoManager.getVisibleTagNum()] = TagInfoManager.findGroupTagByIndex(i).isSelected();
		tagSelectDialogBuilder.setMultiChoiceItems(items, selectionList,
				tagChoiceListener) ;

		//ダイアログを作成
		tagSelectDialog = tagSelectDialogBuilder.create();
		tagSelectDialog.setOnShowListener(tagSelectDialogShowListener);
		tagSelectDialog.show();

		selectionList = new boolean[items.length];
		tagDeleteDialogBuilder.setMultiChoiceItems(items, selectionList,
				tagDeleteChoiceListener);
		tagDeleteDialog = tagDeleteDialogBuilder.create();
	}

	private final void showEnqueteDialog() {
		//アンケート選択肢を追加
		enqueteDialogBuilder.setItems(TalkManager.getEnqueteArray(), null);
		//ダイアログを作成
		enqueteDialog = enqueteDialogBuilder.create();
		enqueteDialog.setOnShowListener(enqueteDialogShowListener);
		enqueteDialog.show();
	}

	private final void showFileUploadDialog() {
		final AlertDialog.Builder b = new AlertDialog.Builder(TalkActivity.this);
		final String[] item;
		if (TalkManager.hasFile()) {
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
						String[] items = new String[TalkManager.getFileNum()];
						for (int i = 0; i < items.length; i++)
							items[i] = TalkManager.getFile(i).fileName;
						b.setItems(items, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, final int selected) {
								AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(TalkActivity.this);
								confirmBuilder.setTitle("除去してよろしいですか？");
								confirmBuilder.setPositiveButton("hai", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										TalkManager.removeFile(selected);
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
			tagSelectButton.setText(TagInfoManager.getSelectedTagText());
		}
		{
			EditText input = (EditText)findViewById(R.id.input_text);
			input.setText(((EditText)findViewById(R.id.input_text_dummy)).getText().toString());
		}
		//保存
		saveTagSelect();
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

		{
			EditText input = (EditText)findViewById(R.id.input_text_dummy);
			input.setText(((EditText)findViewById(R.id.input_text)).getText().toString());
		}
	}

	private final void shiftToSetting() {
		//ViewFlipperにAnimationを設定
		ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
		flipper.setInAnimation(input_in_animation);
		flipper.setOutAnimation(talk_out_animation);

		//設定画面を初期化

		Swallow.UserDetail myself = (Swallow.UserDetail) UserInfoManager.getMyUserInfo().user;

		//プロフィール画像を設定
		ImageView imageView = (ImageView)findViewById(R.id.setting_image);
		if (UserInfoManager.getMyUserInfo().user.getImage() != null) {
			imageView.setImageBitmap(UserInfoManager.getMyUserInfo().profileImage);
		} else {
			imageView.setImageResource(R.drawable.person);
		}
		//名前を設定
		EditText nameView = (EditText)findViewById(R.id.setting_name);
		nameView.setText(myself.getUserName());
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
		String[] member = new String[TagInfoManager.getVisibleTagNum()];
		for (int i = 0; i < member.length; i++)
			member[i] = TagInfoManager.findTagByIndex(i, true).tag.getTagName();
		notifyTagList.setAdapter(new ArrayAdapter<>(TalkActivity.this,
				android.R.layout.simple_list_item_multiple_choice, member));
		//通知リストのチェックを設定
		TagInfoManager.TagInfo[]  notificationTagList = TagInfoManager.getNotification();
		for (TagInfoManager.TagInfo tagInfo : notificationTagList) {
			int index = -1;
			for (int j = 0; j < TagInfoManager.getVisibleTagNum(); j++) {
				if (tagInfo.tag.getTagID() == TagInfoManager.findTagByIndex(j, true).tag.getTagID()) {
					index = j;
					break;
				}
			}
			notifyTagList.setItemChecked(index, index != -1);
		}
		flipper.setDisplayedChild(2);
	}

	private final void editMessage(int postId) {
		TalkManager.startEdit(postId);
	}

	private final void deleteMessage(final int postID) {
		TalkManager.deleteMessage(postID);
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

	private final void setObserveForRunning() {
		try {
			SCM.swallow.modifyUser(null, null, null, null, null, TagInfoManager.getObserveTagIDInRunning(), null);
		} catch (SwallowException e) {
			e.printStackTrace();
		}
	}

	private final void logout() {
		String serial = MyUtils.sp.getString(MyUtils.SWALLOW_SECURITY_SERIALIZE_CODE, null);
		try {
			SwallowSecurity security = SwallowSecurity.deserialize(serial);
			security.logout();
		} catch (SwallowException e) {
			e.printStackTrace();
		}
		MyUtils.sp.edit().putString(MyUtils.SWALLOW_SECURITY_SERIALIZE_CODE, null).apply();
		Intent intent = new Intent(getApplicationContext(), LogInActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}

	private final void paste() {
		View view = getCurrentFocus();
		if (view != null && view instanceof EditText) {
			ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			ClipData cd = cm.getPrimaryClip();
			if(cd != null){
				ClipData.Item item = cd.getItemAt(0);
				EditText pasteTarget = (EditText)view;
				int selection = pasteTarget.getSelectionStart();
				String current = pasteTarget.getText().toString();
				if (selection == -1) {
					pasteTarget.setText(current + item.getText());
				} else {
					input.setText(current.substring(0, selection) + item.getText() + current.substring(selection, current.length()));
				}
			}
		}
	}

	private final void updateTagSelectList() {
		//tagSelectListを更新
		{
			ListView tagSelectList = (ListView) findViewById(R.id.drawer_tag_select_list);
			final String[] members = new String[TagInfoManager.getVisibleTagNum()];
			{
				//adapterを生成・設定。
				for (int i = 0; i < members.length; i++)
					members[i] = TagInfoManager.findTagByIndex(i, true).tag.getTagName();
				ArrayAdapter<String> tagList = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				tagSelectList.setAdapter(tagList);
			}
			//tagSelectListに選択状況を適用
			for (int i = 0; i < members.length; i++) {
				tagSelectList.setItemChecked(i, TagInfoManager.findTagByIndex(i, true).isSelected());
			}
		}

	}

	private final void updatePrivateSelectList() {
		//privateSelectListを更新
		{
			ListView tagSelectList = (ListView) findViewById(R.id.drawer_private_select_list);
			final String[] members = new String[TagInfoManager.getGroupTagNum()];
			{
				//adapterを生成・設定。
				for (int i = 0; i < members.length; i++)
					members[i] = TagInfoManager.findGroupTagByIndex(i).tag.getTagName();
				ArrayAdapter<String> tagList = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_multiple_choice, members);
				tagSelectList.setAdapter(tagList);
			}
			//privateSelectListに選択状況を適用
			for (int i = 0; i < members.length; i++) {
				tagSelectList.setItemChecked(i, TagInfoManager.findGroupTagByIndex(i).isSelected());
			}
		}

	}

	private final void updateUserSelectList() {
		//userSelectListを更新
		{
			ListView userSelectList = (ListView) findViewById(R.id.drawer_user_select_list);
			final String[] members = new String[UserInfoManager.getUserNum()];
			{
				//adapterを生成・設定。
				for (int i = 0; i < members.length; i++)
					members[members.length - i - 1] = UserInfoManager.findUserByIndex(i).user.getUserName();
				ArrayAdapter<String> tagList = new ArrayAdapter<>(TalkActivity.this,
						android.R.layout.simple_list_item_1, members);
				userSelectList.setAdapter(tagList);
			}
		}
	}

	private final boolean canSend(String text) {
		return text.length() > 0 || TalkManager.hasFile() || TalkManager.hasEnquete();
	}

	private final void saveTagSelect() {
		//開いた時のタグを保存
		{
			tmpList.clear();
			for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
				TagInfoManager.TagInfo t = TagInfoManager.findTagByIndex(i, true);
				tmpList.add(t.isSelected());
			}
		}
	}

	private final void refreshOnTagSeletChange() {
		boolean flag = false;
		for (int i = 0; i < TagInfoManager.getVisibleTagNum(); i++) {
			if (tmpList.get(i) != TagInfoManager.findTagByIndex(i, true).isSelected()) {
				flag = true;
				break;
			}
		}
		if (flag) {
			//開いたときと違ったら
			final ProgressDialog progressDialog = MyUtils.createPorgressDialog();
			progressDialog.show();
			AsyncTask<Void, Void, ArrayList<MessageView>> task = new AsyncTask<Void, Void, ArrayList<MessageView>>() {
				@Override
				protected ArrayList<MessageView> doInBackground(Void... params) {
					//MessageViewのリストを更新
					try {
						setObserveForRunning();
						return TalkManager.refreshOnTagSelectChanged();
					} catch (SwallowException e) {
						e.printStackTrace();
					}
					return null;
				}

				@Override
				protected void onPostExecute(ArrayList<MessageView> messageViews) {
					if (messageViews != null) {
						//更新に成功していたら、MessageViewを突っ込む
						TalkManager.removeAllMessageViews();
						for (MessageView mv : messageViews)
							TalkManager.addMessageViewToPrev(mv);
						MyUtils.scrollDown();
					} else {
						MyUtils.showShortToast(TalkActivity.this, "更新に失敗しました");
					}
					progressDialog.dismiss();
				}
			};
			task.execute((Void) null);
		}
	}

	private final void onTagSelectButtonPressed() {
		Button tagButton = (Button)TalkActivity.this.findViewById(R.id.tag_select_button);
		tagButton.setText(TagInfoManager.getSelectedTagText());
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
		if (tag.length() != 0 && TagInfoManager.findTagByName(tag) == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final Integer[] selected = {0}; //ポインタとして使用
			builder.setSingleChoiceItems(new String[]{"グループとして作成", "ただのタグとして作成"}, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					selected[0] = which;
				}
			});
			builder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//tagListに新たなタグを追加
					new ServerTask(TalkActivity.this, "タグの追加に失敗しました") {
						@Override
						public void doInSubThread() throws SwallowException {
							Integer[] participants;
							if (selected[0] == 0) {
								participants = new Integer[]{UserInfoManager.getMyUserID()};
							} else {
								participants = new Integer[0];
							}
							TagInfoManager.TagInfo t = TagInfoManager.addTag(tag, participants, true);
							if (t != null)
								TagInfoManager.selectTag(t);
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
			});
			builder.show();
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
			TalkManager.addEnquete(enquete);

			//アンケート選択ダイアログを再表示
			enqueteDialog.dismiss();
			showEnqueteDialog();
		}
	}

	float max_accel;
	float[] gravity = new float[3];

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			gravity[0] = event.values[0];
			gravity[1] = event.values[1];
			gravity[2] = event.values[2];
		}
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float ax = event.values[0] - gravity[0];
			float ay = event.values[1] - gravity[1];
			float az = event.values[2] - gravity[2];
			float a = (float)Math.sqrt(ax*ax + ay*ay + az*az);
			if (a > 25) {
				max_accel = Math.max(max_accel, a);
			} else if (a < 5 && max_accel != -1) {
				ViewFlipper flipper = (ViewFlipper) findViewById(R.id.flipper);
				String text;
				if (flipper.getCurrentView() == inputView) {
					text = input.getText().toString();
					if (canSend(text)) {
						text += "\nこの投稿の加速度は" + max_accel + "m/(s^2)でした";
						if (submitMessage(text, input)) {
							TalkManager.endPost();
							//view_talkに戻る
							((ViewFlipper) findViewById(R.id.flipper)).showPrevious();
						}
					}
				} else if (flipper.getCurrentView() == mainView) {
					EditText input = (EditText) findViewById(R.id.input_text_dummy);
					text = input.getText().toString();
					if (canSend(text)) {
						TalkManager.startPost();
						text += "\nこの投稿の加速度は" + max_accel + "m/(s^2)でした";
						if (submitMessage(text, input)) {
							TalkManager.endPost();
						}
					}
				}
				max_accel = -1;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	private final class Task extends TimerTask {

		@Override
		public void run() {
			backCount--;
			TalkManager.run();
		}

	}


}
