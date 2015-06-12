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
import com.trap.swallow.info.TagInfoManager;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.info.UserInfoManager;
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
	private final List<MessageView> messageList = Collections.synchronizedList(new ArrayList<MessageView>());
	private final TalkActivity context;
	private final ViewGroup parent;
	private int editingPostId = -1; //編集中の投稿のID
	private int replyPostId = -1; //リプライ用の一時保存変数
	private ArrayList<FileInfo> postFileData = new ArrayList<>();
	private ArrayList<String> enqueteList = new ArrayList<>(); //現在の投稿のアンケート
	private ArrayList<String> codeList = new ArrayList<>(); //現在の投稿の挿入するソース

	public TalkManager(TalkActivity context, ViewGroup parent) {
		this.context = context;
		this.parent = parent;
		this.clipView = new FileClipView(context);
	}

	public final void init() throws SwallowException, IOException, ClassNotFoundException {

		UserInfoManager.reload();

		TagInfoManager.reload();

		//messageListの読み込み
		SCM.initMessageList(messageList);

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

	public final ArrayList<MessageView> loadNextMessage() {

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

	public final ArrayList<MessageView> loadPreviousMessageUntil(long until) {

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

	public final ArrayList<MessageView> refreshOnTagSelectChanged() throws SwallowException {

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

	public final void refreshOnUserInfoChanged_1() {

		UserInfoManager.reload();

		TagInfoManager.reload();
	}

	public final void refreshOnUserInfoChanged_2() {
		//messageListの更新
		for (MessageView mv : messageList) {
			mv.refleshOnUserInfoChanged();
		}
	}

//	public final ArrayList<MessageView> refreshOnUserSelected() {
//		messageList.clear();
//
//	}

	public final MessageView submit(String text) throws SwallowException {
        //ファイル関連
		ArrayList<Integer> fileId = new ArrayList<>();
		ArrayList<FileInfo> postFileData = (ArrayList<FileInfo>)this.postFileData.clone();
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
		this.postFileData.clear();
        codeList.clear();

		MessageView mv = new MessageView(mInfo);
		Animation anim = createAnimationOnReflesh(5);
		mv.anim = anim;
		messageList.add(mv);
		return mv;
	}

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
		mv.initOnMainThread();
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
		mv.initOnMainThread();
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

	public final Message findMessageById(int postId) {
		try {
			return SCM.swallow.findMessage(null, null, null, null, new Integer[]{postId}, null, null, null, null, null, null, null, null)[0];
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
			return SCM.swallow.findFile(null, null, null, null, new Integer[]{id}, null, null, null)[0];
		} catch (SwallowException e) {
			MyUtils.showShortToast(context, "ファイルの検索に失敗しました");
		}
		return null;
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
		TagInfoManager.setSelection(mv.mInfo.getTagID());
		//タグ選択リストを変更
				((Button) context.findViewById(R.id.tag_select_button)).setText(TagInfoManager.getSelectedTagText());
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

    public final void addCode(String code) {
        codeList.add(code);
    }

    public final boolean hasCode() {
        return codeList.size() > 0;
    }

    public final int getCodeNum() {
        return codeList.size();
    }

    public final void removeCode(int index) {
        codeList.remove(index);
    }
}
