package com.trap.swallow.talk;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.trap.swallow.info.FileInfo;
import com.trap.swallow.info.TagInfo;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.server.SCM;
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
import java.util.List;

/*
 * トークに関する処理のみをするクラス
 */
public class TalkManager {


	private FileClipView clipView;

	private final ArrayList<UserInfo> userInfoList = new ArrayList<>();
	private final ArrayList<TagInfo> visibleTagList = new ArrayList<>();
	private final ArrayList<TagInfo> invisibleTagList = new ArrayList<>();
	private final List<MessageView> messageList = Collections.synchronizedList(new ArrayList<MessageView>());
	private final TalkActivity context;
	private final ViewGroup parent;
	private UserInfo myUserInfo;
	private int editingPostId = -1; //編集中の投稿のID
	private int replyPostId = -1; //リプライ用の一時保存変数
	private ArrayList<FileInfo> postFileData = new ArrayList<>();
	private ArrayList<String> enqueteList = new ArrayList<>(); //現在の投稿のアンケート

	public TalkManager(TalkActivity context, ViewGroup parent) {
		this.context = context;
		this.parent = parent;
		this.clipView = new FileClipView(context);
	}

	public final void init() throws SwallowException, IOException, ClassNotFoundException {

		//userInfoListの読み込み
		SCM.scm.loadUserInfo(userInfoList);

		//tagListの読み込み
		SCM.scm.loadTagList(visibleTagList, invisibleTagList);

		//タグが１つもなかったら、generalというタグを作成する
		if (visibleTagList.size() == 0) {
			//タグ作成
			String initialTagName = "general";
			int tagID = SCM.scm.sendAddTag(initialTagName, false);
			TagInfo tagInfo = new TagInfo(initialTagName, tagID);
			visibleTagList.add(tagInfo);
			//選択
			tagInfo.isSelected = true;
		}
		//幼女タグがなかったら作成
		{
			String yojoTagName = "yojo";
			if (findInVisibleTagByName(yojoTagName) == null) {
				//タグ作成
				int tagID = SCM.scm.sendAddTag(yojoTagName, true);
				TagInfo tagInfo = new TagInfo(yojoTagName, tagID);
				invisibleTagList.add(tagInfo);
			}
		}

		//初期状態でのタグ選択状況を設定
		{
			String value = MyUtils.sp.getString(MyUtils.SELECTED_TAG_KEY, null);
			if (value != null && value.length() > 0) {
				//Preferenceに選択されているタグのデータがあったら利用
				// ","で切る
				for (String str : value.split(",")) {
					TagInfo t = findVisibleTagById(Integer.parseInt(str));
					if (t != null) {
						t.isSelected = true;
					}
				}
			}  else {
				//Prefereneにタグ選択に関するデータがない場合、作っておく
				MyUtils.sp.edit().putString(MyUtils.SELECTED_TAG_KEY,
						getSelectedTagIDText()).apply();
			}
		}
		//初期状態でのタグ通知情報を設定
		{
			//Preferenceに通知情報がなかったら、作っておく
			if (MyUtils.sp.getString(MyUtils.NOTIFY_TAG_KEY, null) == null) {
				//とりあえず普通に選択しているものを通知にも登録
				MyUtils.sp.edit().putString(MyUtils.NOTIFY_TAG_KEY, getSelectedTagIDText()).apply();
			}
		}

		//自分のユーザーIDを取得
		myUserInfo = new UserInfo(SCM.scm.swallow.modifyUser(null, null, null, null, null, null, null, null, null, null, null));

		//自分のユーザーIDをPreferrenceへ
		{
			MyUtils.sp.edit().putInt(MyUtils.MY_USER_ID_KEY, myUserInfo.user.getUserID()).apply();
		}

		//messageListの読み込み
		SCM.scm.initMessageList(messageList, getSelectedTagIDList(), context, this);

	}

	public final void pushMessageList() {
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

	public final ArrayList<MessageView> loadPreviousMessage() {

		//userInfoListの読み込み
		try {
			SCM.scm.loadUserInfo(userInfoList);
		} catch (SwallowException e) {
			e.printStackTrace();
			return null;
		}

		//選択されているタグを一時的に保存
		ArrayList<Integer> selectedTagIDList = new ArrayList<>();
		for (int i = 0; i < getVisibleTagNum(); i++) {
			TagInfo t = findVisibleTagByIndex(i);
			if (t.isSelected)
				selectedTagIDList.add(t.tagID);
		}

		//tagListの読み込み
		try {
			SCM.scm.loadTagList(visibleTagList, invisibleTagList);
		} catch (SwallowException e) {
			e.printStackTrace();
			return null;
		}

		//現在のタグ選択状況を復元
		for (int i = 0; i < selectedTagIDList.size(); i++) {
			TagInfo t = findVisibleTagById(selectedTagIDList.get(i));
			if (t != null)
				t.isSelected = true;
		}

		ArrayList<MessageView> newMessageList = new ArrayList<>();
		//newMessageListの読み込み
		try {
			SCM.scm.loadOlderMessageToList(newMessageList, getSelectedTagIDList(), context, this);
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

	public final ArrayList<MessageView> loadNextMessage() throws SwallowException {

		//userInfoListの読み込み
		SCM.scm.loadUserInfo(userInfoList);

		//選択されているタグを一時的に保存
		ArrayList<Integer> selectedTagIDList = new ArrayList<>();
		for (int i = 0; i < getVisibleTagNum(); i++) {
			TagInfo t = findVisibleTagByIndex(i);
			if (t.isSelected)
				selectedTagIDList.add(t.tagID);
		}

		//tagListの読み込み
		SCM.scm.loadTagList(visibleTagList, invisibleTagList);

		//現在のタグ選択状況を復元
		for (int i = 0; i < selectedTagIDList.size(); i++) {
			TagInfo t = findVisibleTagById(selectedTagIDList.get(i));
			if (t != null)
				t.isSelected = true;
		}

		ArrayList<MessageView> newMessageList = new ArrayList<>();
		//newMessageListの読み込み
		SCM.scm.loadNewMessagesToList(newMessageList, getSelectedTagIDList(), context, this);

		for (int i = 0; i < newMessageList.size(); i++) {
			MessageView mv = newMessageList.get(i);
			Animation anim = createAnimationOnReflesh(i);
			mv.anim = anim;
			messageList.add(i,mv);
		}
		return newMessageList;
	}

	public final ArrayList<MessageView> refreshOnTagSelectChanged() throws SwallowException, IOException, ClassNotFoundException {

		//userInfoListの読み込み
		SCM.scm.loadUserInfo(userInfoList);

		//選択されているタグを一時的に保存
		ArrayList<Integer> selectedTagIDList = new ArrayList<>();
		for (int i = 0; i < getVisibleTagNum(); i++) {
			TagInfo t = findVisibleTagByIndex(i);
			if (t.isSelected)
				selectedTagIDList.add(t.tagID);
		}

		//tagListの読み込み
		SCM.scm.loadTagList(visibleTagList, invisibleTagList);

		//現在のタグ選択状況を復元
		for (int i = 0; i < selectedTagIDList.size(); i++) {
			TagInfo t = findVisibleTagById(selectedTagIDList.get(i));
			if (t != null)
				t.isSelected = true;
		}

		//messageListの更新
		messageList.clear();
		SCM.scm.initMessageList(messageList, getSelectedTagIDList(), context, this);

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

	public final void refreshOnUserInfoChanged_1() throws SwallowException {

		//userInfoListの読み込み
		SCM.scm.loadUserInfo(userInfoList);

		//選択されているタグを一時的に保存
		ArrayList<Integer> selectedTagIDList = new ArrayList<>();
		for (int i = 0; i < getVisibleTagNum(); i++) {
			TagInfo t = findVisibleTagByIndex(i);
			if (t.isSelected)
				selectedTagIDList.add(t.tagID);
		}

		//tagListの読み込み
		SCM.scm.loadTagList(visibleTagList, invisibleTagList);

		//現在のタグ選択状況を復元
		for (int i = 0; i < selectedTagIDList.size(); i++) {
			TagInfo t = findVisibleTagById(selectedTagIDList.get(i));
			if (t != null)
				t.isSelected = true;
		}
	}

	public final void refreshOnUserInfoChanged_2() {
		//messageListの更新
		for (MessageView mv : messageList) {
			mv.refleshOnUserInfoChanged();
		}
	}

	public final MessageView submit(final String text) throws SwallowException {
		ArrayList<Integer> fileId = new ArrayList<>();
		ArrayList<FileInfo> postFileData = (ArrayList<FileInfo>)this.postFileData.clone();
		for (FileInfo info : postFileData) {
			Swallow.File file = info.send();
			int fileID = file.getFileID();
			fileId.add(fileID);
		}
		Message mInfo;
		if (isEditMode()) {
			mInfo = SCM.scm.editMessage(text, fileId.toArray(new Integer[0]), replyPostId == -1 ? null : new Integer[]{replyPostId}, getSelectedTagIDList(), null, enqueteList.toArray(new String[0]), editingPostId);
		} else {
			mInfo = SCM.scm.sendMessage(text, fileId.toArray(new Integer[0]), replyPostId == -1 ? null : new Integer[]{replyPostId}, getSelectedTagIDList(), null, enqueteList.toArray(new String[0]));
		}
		if (enqueteList.size() > 0) {
			for (int i = 0; i < 10; i++) {
				SCM.scm.swallow.createAnswer(mInfo.getPostID(), (int)(Math.random() * enqueteList.size()));
			}
		}
		enqueteList.clear();
		this.postFileData.clear();

		MessageView mv = new MessageView(context, mInfo, null, TalkManager.this);
		Animation anim = createAnimationOnReflesh(5);
		mv.anim = anim;
		messageList.add(mv);
		return mv;
	}

//
//	//送信時にMessageInfoを作成
//	private final Message createMessage(String text, Integer[] fileId) {
//		//タグ設定
//		ArrayList<Integer> tagId = new ArrayList<>();
//		for (int i = 0; i < tagItemSelectedListForSend.length; i++)
//			if (tagItemSelectedListForSend[i])
//				tagId.add(tagList.get(i).tagID);
//		//リプ設定
//		ArrayList<Integer> replyId = new ArrayList<>();
//		//宛先設定
//		ArrayList<Integer> destId = new ArrayList<>();
//		if (receiveFlag) {
//			tagId.add(2); //2番は不可視タグConfirmation(既読チェック)用のもの
//		}
//		SCM.scm.swallow.createMessage()
//
//		Message mInfo = new Message(null, System.currentTimeMillis(), myUserId, text, fileId, tagId.toArray(new Integer[0]), replyId.toArray(new Integer[0]), destId.toArray(new Integer[0]), enqueteList.toArray(new String[0]), 0, null, 0, null, 0, null);
//		return mInfo;
//	}

	private final Animation createAnimationOnInit(int index) {
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

	private final Animation createAnimationOnReflesh(int index) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, -1.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		animation.setStartOffset(300 + index * 100);
		return animation;
	}

	public final void addMessageViewToPrev(MessageView mv) {
		LineView lv = new LineView(context);
		lv.setAnimation(mv.anim);
		parent.addView(lv, 0);
		parent.addView(mv, 0, mv.lp);
		//ViewFlipperのアニメーションとかぶるので、若干遅延させる
//		mv.setAnimation(mv.anim);
//		mv.invalidate();
	}

	public final void addMessageViewToNext(MessageView mv) {
		LineView lv = new LineView(context);
		lv.setAnimation(mv.anim);
		parent.addView(lv);
		parent.addView(mv, mv.lp);
		//ViewFlipperのアニメーションとかぶるので、若干遅延させる
		mv.setAnimation(mv.anim);
		mv.invalidate();
	}

	public final void changeMessageView(MessageView after) {
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
			View v = parent.getChildAt(i);
			if (v instanceof MessageView) {
				MessageView mv = (MessageView)v;
				if (mv.mInfo.getPostID() == editingPostId) {
					parent.removeViewAt(i);
					parent.addView(after, i);
				}
			}
		}
	}

	private final byte[] convertToByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte [] buffer = new byte[1024];
		while(true) {
			int len = inputStream.read(buffer);
			if(len < 0) {
				break;
			}
			bout.write(buffer, 0, len);
		}
		return bout.toByteArray();
	}

	public final UserInfo findUserById(int id) {
		for (UserInfo u : userInfoList)
			if (u.user.getUserID() == id)
				return u;
		return null;
	}

	public final TagInfo findVisibleTagById(int id) {
		for (TagInfo t : visibleTagList)
			if (t.tagID == id)
				return t;
		return null;
	}

	public final TagInfo findInvisibleTagById(int id) {
		for (TagInfo t : invisibleTagList)
			if (t.tagID == id)
				return t;
		return null;
	}

	public final TagInfo findInVisibleTagByName(String name) {
		for (TagInfo t : invisibleTagList)
			if (t.tagName.equals(name))
				return t;
		return null;
	}

	public final TagInfo findVisibleTagByIndex(int index) {
		return visibleTagList.get(index);
	}

	public final TagInfo findVisibleTagByName(String name) {
		for (TagInfo t : visibleTagList)
			if (t.tagName.equals(name))
				return t;
		return null;
	}

	public final int getVisibleTagNum() {
		return visibleTagList.size();
	}

	public final void addVisibleTag(TagInfo t) {
		visibleTagList.add(t);
	}

	public final Message findMessageById(int postId) {
		try {
			return SCM.scm.swallow.findMessage(null, null, null, null, new Integer[]{postId}, null, null, null, null, null, null, null, null)[0];
		} catch (SwallowException e) {
			MyUtils.showShortToast(context, "メッセージの検索に失敗しました");
		}
		return null;
	}

	public final MessageView findMessageViewById(int postId) {
		for (MessageView mv : messageList) {
			if (mv.mInfo.getPostID() == postId) {
				return mv;
			}
		}
		return null;
	}

	public final File findFileById(int id) {
		try {
			return SCM.scm.swallow.findFile(null, null, null, null, new Integer[]{id}, null, null, null)[0];
		} catch (SwallowException e) {
			MyUtils.showShortToast(context, "ファイルの検索に失敗しました");
		}
		return null;
	}

	public final Integer[] getSelectedTagIDList() {
		ArrayList<Integer> array = new ArrayList<>();
		for (TagInfo tag : visibleTagList) {
			if (tag.isSelected)
				array.add(tag.tagID);
		}
		//既読をつけるかどうかでタグを追加
		if (getReceivedFlag()) {
			array.add(findInVisibleTagByName("confirmation").tagID);
		}
		//幼女かどうかでタグを追加
		if (MyUtils.sp.getBoolean(MyUtils.YOJO_CHECK_KEY, false)) {
			array.add(findInVisibleTagByName("yojo").tagID);
		}
		return array.toArray(new Integer[0]);
	}

	public final String getSelectedTagText() {
		StringBuilder sb= new StringBuilder();
		for (TagInfo tag : visibleTagList) {
			if (tag.isSelected) {
				sb.append(tag.tagName);
				sb.append(",");
			}
		}
		if (sb.length() > 0)
			sb.delete(sb.length()-1, sb.length());
		return sb.toString();
	}

	public final String getSelectedTagIDText() {
		StringBuilder sb= new StringBuilder();
		for (TagInfo tag : visibleTagList) {
			if (tag.isSelected) {
				sb.append(tag.tagID);
				sb.append(",");
			}
		}
		if (sb.length() > 0)
			sb.delete(sb.length()-1, sb.length());
		return sb.toString();
	}

	private final boolean getReceivedFlag() {
		return ((CheckBox)context.findViewById(R.id.checkReceivedBox)).isChecked();
	}

	private final void saveMessageViews(final ArrayList<MessageView> mvList) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ByteArrayOutputStream baos;
					ObjectOutputStream oos = new ObjectOutputStream(baos = new ByteArrayOutputStream());
					for (MessageView mv : mvList) {
						String key = MyUtils.MESSAGE_VIEW_KEY + mv.mInfo.getPostID();
						if (MyUtils.sp.getString(key,  null) == null) {
							//保存されていなかったら
							oos.writeObject(mv);
							String str = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT).trim();
							MyUtils.sp.edit().putString(key , str);
						}
					}
					oos.close();
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void run() {
		//画面サイズ取得
		WindowManager wm = (WindowManager)context.getSystemService(TalkActivity.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point size = new Point();
		disp.getSize(size);

		//既読をつける
		//範囲内かつ未読なら既読つける
		synchronized (messageList) {
			float y;
			for (MessageView mv : messageList) {
				synchronized (mv) {
					y = mv.getY();
					//範囲チェック
					if (-mv.getHeight() <= y && y <= size.y) {
						String key = MyUtils.HAS_READ_KEY + mv.mInfo.getPostID();
						if (MyUtils.sp.getBoolean(key, false) == false) {
							//未読チェック
							try {
								SCM.scm.swallow.createReceived(mv.mInfo.getPostID());
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

	public final UserInfo getMyUserInfo() {
		return myUserInfo;
	}

	public final boolean isReplyMode() {
		return replyPostId != -1;
	}

	public final void startReply(int replyPostId) {
		this.replyPostId = replyPostId;
		((TextView)context.findViewById(R.id.input_explain_text)).setText("リプなう");
	}

	public final void endReply() {
		replyPostId = -1;
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public final boolean isEditMode() {
		return editingPostId != -1;
	}

	public final void startEdit(int editingPostId) {

		((TextView)context.findViewById(R.id.input_explain_text)).setText("編集なう");
		//編集するMessageViewを検索
		MessageView mv = findMessageViewById(editingPostId);
		//送信側のタグ選択状況を変更
		for (int i = 0; i < getVisibleTagNum(); i++) {
			TagInfo t = findVisibleTagByIndex(i);
			t.isSelected = false;
		}
		for (int tagID : mv.mInfo.getTagID()) {
			TagInfo t = findVisibleTagById(tagID);
			if (t != null) { //t == nullはinvisibleのtag
				t.isSelected = true;
			}
		}
		//タグ選択リストを変更
		((Button)context.findViewById(R.id.tag_select_button)).setText(getSelectedTagText());
		//入力フォームに入っている文字列を変更
		((EditText)context.findViewById(R.id.input_text)).setText(mv.mInfo.getMessage());

		this.editingPostId = editingPostId;
	}

	public final void endEdit() {
		editingPostId = -1;
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public final void addFileToPost(FileInfo fileInfo) {
		postFileData.add(fileInfo);
		clipView.addImage(fileInfo.bmp);
	}

	public final void startPost() {
		editingPostId = -1;
		replyPostId = -1;

		((TextView)context.findViewById(R.id.input_explain_text)).setText("");
		((EditText)context.findViewById(R.id.input_text)).setText("");

	}

	public final void endPost() {
		((EditText)context.findViewById(R.id.input_text_dummy)).setText("");
		((EditText)context.findViewById(R.id.input_text)).setText("");
		clearFile();
		clearEnquete();
	}

	public final FileClipView getFileClipView() {
		return clipView;
	}

	public final void setMyUserInfo(Swallow.UserDetail myUserInfo) {
		this.myUserInfo = new UserInfo(myUserInfo);
	}

	public final boolean hasFile() {
		return postFileData.size() > 0;
	}

	public final boolean hasEnquete() {
		return enqueteList.size() > 0;
	}

	public final void addEnquete(String enquete) {
		enqueteList.add(enquete);
		((ImageButton)context.findViewById(R.id.enqueteButton)).setBackgroundResource(R.drawable.enquete_selected_selector);
	}

	public final void clearEnquete() {
		enqueteList.clear();
		((ImageButton)context.findViewById(R.id.enqueteButton)).setBackgroundResource(R.drawable.enquete_normal_selector);
	}

	public final void clearFile() {
		postFileData.clear();
		clipView.clearImage();
	}

	public final String[] getEnqueteArray() {
		return enqueteList.toArray(new String[0]);
	}

	public final int getFileNum() {
		return postFileData.size();
	}

	public final FileInfo getFile(int index) {
		return postFileData.get(index);
	}

	public final void removeFile(int index) {
		postFileData.remove(index);
		clipView.removeImage(index);
	}
}
