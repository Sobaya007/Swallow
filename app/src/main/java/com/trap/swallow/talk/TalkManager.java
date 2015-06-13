package com.trap.swallow.talk;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.trap.swallow.info.FileInfo;
import com.trap.swallow.info.TagInfoManager;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.info.UserInfoManager;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.Swallow.File;
import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.swallow.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/*
 * トークに関する処理のみをするクラス
 */
public class TalkManager {
	
	private static FileClipView clipView;
	private static final List<MessageView> messageList = Collections.synchronizedList(new ArrayList<MessageView>());
	private static TalkActivity context;
	private static final MessageViewAdapter parent = new MessageViewAdapter();
	private static int editingPostId = -1; //編集中の投稿のID
	private static int replyPostId = -1; //リプライ用の一時保存変数
	private static ArrayList<FileInfo> postFileData = new ArrayList<>();
	private static ArrayList<String> enqueteList = new ArrayList<>(); //現在の投稿のアンケート
	private static ArrayList<String> codeList = new ArrayList<>(); //現在の投稿の挿入するソース

	public static void start(TalkActivity context) {
		TalkManager.context = context;
		TalkManager.clipView = (FileClipView)context.findViewById(R.id.file_clip_view);
		((ListView)context.findViewById(R.id.talk_scroll_view)).setAdapter(parent);
	}

	public final static void init() throws SwallowException, IOException, ClassNotFoundException {

		UserInfoManager.reload();

		TagInfoManager.reload();

		//messageListの読み込み
		SCM.initMessageList(messageList);

	}

	public final static void pushMessageList() {
		//レイアウトの更新
		for (int i = 0; i < messageList.size(); i++) {
			MessageView mv = messageList.get(i);
			Animation anim = createAnimationOnInit(messageList.size() - i);
			mv.anim = anim;
			if (i == 0)
				anim.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
						MyUtils.scrollDown();
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						//一番下のMessageViewが入り終わったらスクロール
						MyUtils.scrollDown();
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
			addMessageViewToPrev(mv);
		}
	}

	public final static ArrayList<MessageView> loadPreviousMessage() {

		if (!UserInfoManager.reload()) return null;

		if (!TagInfoManager.reload()) return null;

		ArrayList<MessageView> newMessageList = new ArrayList<>();
		//newMessageListの読み込み
		try {
			SCM.loadOlderMessageToList(newMessageList);
		} catch (SwallowException e) {
			e.printStackTrace();
			return null;
		}

		for (int i = 0; i < newMessageList.size(); i++) {
			MessageView mv = newMessageList.get(i);
			Animation anim = createAnimationOnReflesh(i);
			mv.anim = anim;
			messageList.add(mv);
		}
		return newMessageList;
	}

	public final static ArrayList<MessageView> loadNextMessage() {

		if (!UserInfoManager.reload()) return null;

		if (!TagInfoManager.reload()) return null;

		ArrayList<MessageView> newMessageList = new ArrayList<>();
		try {
			SCM.loadNewMessagesToList(newMessageList);
		} catch (SwallowException e) {
			e.printStackTrace();
			return null;
		}

		for (int i = 0; i < newMessageList.size(); i++) {
			MessageView mv = newMessageList.get(i);
			Animation anim = createAnimationOnReflesh(i);
			mv.anim = anim;
			messageList.add(i, mv);
		}
		return newMessageList;
	}

	public final static ArrayList<MessageView> loadPreviousMessageUntil(long until) {

		if (!UserInfoManager.reload()) return null;

		if (!TagInfoManager.reload()) return null;

		ArrayList<MessageView> newMessageList = new ArrayList<>();
		//newMessageListの読み込み
		try {
			SCM.loadOlderMessageToListUntil(newMessageList, until);
		} catch (SwallowException e) {
			e.printStackTrace();
			return null;
		}

		for (int i = 0; i < newMessageList.size(); i++) {
			MessageView mv = newMessageList.get(i);
			Animation anim = createAnimationOnReflesh(i);
			mv.anim = anim;
			messageList.add(mv);
		}
		return newMessageList;
	}

	public final static ArrayList<MessageView> refreshOnTagSelectChanged() throws SwallowException {

		//messageListの更新
		messageList.clear();
		SCM.initMessageList(messageList);

		//レイアウトの更新
		ArrayList<MessageView> newMessageList = new ArrayList<>();
		for (int i = 0; i < messageList.size(); i++) {
			MessageView mv = messageList.get(i);
			Animation anim = createAnimationOnInit(i);
			mv.anim = anim;
			newMessageList.add(mv);
		}
		return newMessageList;
	}

	public final static void refreshOnUserInfoChanged_1() {

		UserInfoManager.reload();

		TagInfoManager.reload();
	}

	public final static void refreshOnUserInfoChanged_2() {
		//messageListの更新
		for (MessageView mv : messageList) {
			mv.refleshOnUserInfoChanged();
		}
	}

	public final ArrayList<MessageView> refreshOnUserSelected() throws SwallowException{
		messageList.clear();
		SCM.initMessageList(messageList);

		//レイアウトの更新
		ArrayList<MessageView> newMessageList = new ArrayList<>();
		for (int i = 0; i < messageList.size(); i++) {
			MessageView mv = messageList.get(i);
			Animation anim = createAnimationOnInit(i);
			mv.anim = anim;
			newMessageList.add(mv);
		}
		return newMessageList;
	}

	public final static void submit(String text) throws SwallowException {
		//ファイル関連
		ArrayList<Integer> fileId = new ArrayList<>();
		ArrayList<FileInfo> postFileData = (ArrayList<FileInfo>)TalkManager.postFileData.clone();
		for (FileInfo info : postFileData) {
			Swallow.File file = info.send();
			int fileID = file.getFileID();
			fileId.add(fileID);
		}
		//コード挿入関連
		StringBuilder sb = new StringBuilder(text);
		for (String code : codeList) {
			sb.append(MessageView.MESSAGE_SEPARATOR);
			sb.append(code);
		}
		text = sb.toString();
		Message mInfo;
		if (isEditMode()) {
			mInfo = SCM.editMessage(text, fileId.toArray(new Integer[0]), replyPostId == -1 ? null : new Integer[]{replyPostId}, TagInfoManager.getSelectedTagIDForSend(), null, enqueteList.toArray(new String[0]), editingPostId);
		} else {
			mInfo = SCM.sendMessage(text, fileId.toArray(new Integer[0]), replyPostId == -1 ? null : new Integer[]{replyPostId}, TagInfoManager.getSelectedTagIDForSend(), null, enqueteList.toArray(new String[0]));
		}
		enqueteList.clear();
		TalkManager.postFileData.clear();
		codeList.clear();
	}

	private final static Animation createAnimationOnInit(int index) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 1.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f);
		animation.setDuration(500);
		animation.setStartOffset(1000 + index
				* 100);
		return animation;
	}

	private final static Animation createAnimationOnReflesh(int index) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, -1.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		animation.setStartOffset(300 + index * 100);
		return animation;
	}

	public final static void addMessageViewToPrev(MessageView mv) {
		LineView lv = new LineView(context);
		lv.setAnimation(mv.anim);
		parent.add(mv, 0);
		mv.initOnMainThread();
		//ViewFlipperのアニメーションとかぶるので、若干遅延させる
//		mv.setAnimation(mv.anim);
//		mv.invalidate();
	}

	public final static void addMessageViewToNext(MessageView mv) {
		LineView lv = new LineView(context);
		lv.setAnimation(mv.anim);
		parent.add(mv);
		//ViewFlipperのアニメーションとかぶるので、若干遅延させる
		mv.setAnimation(mv.anim);
		mv.invalidate();
		mv.initOnMainThread();
	}

	public final static void changeMessageView(MessageView after) {
		for (int i = 0; i < messageList.size(); i++) {
			if (messageList.get(i).mInfo.getPostID() == editingPostId) {
				messageList.remove(i);
				messageList.add(i, after);
				after.setAnimation(after.anim);
				after.invalidate();
				break;
			}
		}
		for (int i = 0; i < parent.getChildCount(); i++) {
			MessageView mv = parent.getChildAt(i);
			if (mv.mInfo.getPostID() == editingPostId) {
				parent.removeAt(i);
				parent.add(after, i);
			}
		}
	}

	public final static MessageView findMessageViewById(int postId) {
		for (MessageView mv : messageList) {
			if (mv.mInfo.getPostID() == postId) {
				return mv;
			}
		}
		return null;
	}

	public final static File findFileById(int id) {
		try {
			return SCM.swallow.findFile(null, null, null, null, new Integer[]{id}, null, null, null)[0];
		} catch (SwallowException e) {
			MyUtils.showShortToast(context, "ファイルの検索に失敗しました");
		}
		return null;
	}

	public static void run() {
		//画面サイズ取得
		WindowManager wm = (WindowManager)context.getSystemService(TalkActivity.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point size = new Point();
		disp.getSize(size);

		//既読をつける
		//範囲内かつ未読なら既読つける
		synchronized (messageList) {
			int[] pos = new int[2];
			int y;
			for (MessageView mv : messageList) {
				synchronized (mv) {
					mv.getLocationInWindow(pos);
					y = pos[1];
					//範囲チェック
					if (-mv.getHeight() <= y && y <= size.y) {
						String key = MyUtils.HAS_READ_KEY + mv.mInfo.getPostID();
						if (MyUtils.sp.getBoolean(key, false) == false) {
							//未読チェック
							try {
								SCM.swallow.createReceived(mv.mInfo.getPostID());
								MyUtils.sp.edit().putBoolean(key, true).apply();
							} catch (SwallowException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	public static final boolean isReplyMode() {
		return replyPostId != -1;
	}

	public static final void startReply(int replyPostId) {
		TalkManager.replyPostId = replyPostId;
		((TextView)context.findViewById(R.id.input_explain_text)).setText("リプなう");
	}

	public static final void endReply() {
		replyPostId = -1;
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public static final boolean isEditMode() {
		return editingPostId != -1;
	}

	public static final void startEdit(int editingPostId) {

		((TextView)context.findViewById(R.id.input_explain_text)).setText("編集なう");
		//編集するMessageViewを検索
		MessageView mv = findMessageViewById(editingPostId);
		//送信側のタグ選択状況を変更
		TagInfoManager.setSelection(mv.mInfo.getTagID());
		//タグ選択リストを変更
		((Button) context.findViewById(R.id.tag_select_button)).setText(TagInfoManager.getSelectedTagText());
		//入力フォームに入っている文字列を変更
		((EditText)context.findViewById(R.id.input_text)).setText(mv.mInfo.getMessage());

		TalkManager.editingPostId = editingPostId;
	}

	public static final void endEdit() {
		editingPostId = -1;
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public static final void addFileToPost(FileInfo fileInfo) {
		postFileData.add(fileInfo);
		clipView.addImage(fileInfo.bmp);
	}

	public static final void startPost() {
		editingPostId = -1;
		replyPostId = -1;

		((TextView)context.findViewById(R.id.input_explain_text)).setText("");
		((EditText)context.findViewById(R.id.input_text)).setText("");

	}

	public static final void endPost() {
		((EditText)context.findViewById(R.id.input_text_dummy)).setText("");
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public static final FileClipView getFileClipView() {
		return clipView;
	}

	public static final boolean hasFile() {
		return postFileData.size() > 0;
	}

	public static final boolean hasEnquete() {
		return enqueteList.size() > 0;
	}

	public static final void addEnquete(String enquete) {
		enqueteList.add(enquete);
		((ImageButton)context.findViewById(R.id.enqueteButton)).setBackgroundResource(R.drawable.enquete_selected_selector);
	}

	public static final void clearEnquete() {
		enqueteList.clear();
		((ImageButton)context.findViewById(R.id.enqueteButton)).setBackgroundResource(R.drawable.enquete_normal_selector);
	}

	public static final void clearFile() {
		postFileData.clear();
		clipView.clearImage();
	}

	public static final String[] getEnqueteArray() {
		return enqueteList.toArray(new String[0]);
	}

	public static final int getFileNum() {
		return postFileData.size();
	}

	public static final FileInfo getFile(int index) {
		return postFileData.get(index);
	}

	public static final void removeFile(int index) {
		postFileData.remove(index);
		clipView.removeImage(index);
	}

	public final static void removeAllMessageViews() {
		parent.clear();
	}

	public final static void deleteMessage(final int postID) {
		//削除するMessageViewを検索
		for (int i = 0; i < parent.getChildCount(); i++) {
			MessageView mv = parent.getChildAt(i);
			final int index = i;
			if (mv.mInfo.getPostID() == postID) {
				new ServerTask(TalkActivity.singleton, "削除失敗") {
					@Override
					public void doInSubThread() throws SwallowException {
						SCM.deleteMessage(postID);
					}

					@Override
					protected void onPostExecute(Boolean aBoolean) {
						if (aBoolean) {
							parent.removeAt(index);
						}
					}
				};
				break;
			}
		}
	}
}
