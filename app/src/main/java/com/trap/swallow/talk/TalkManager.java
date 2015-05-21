package com.trap.swallow.talk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;
import android.widget.Toast;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.Swallow.File;
import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.swallow.R;

/*
 * トークに関する処理のみをするクラス
 */
public class TalkManager {

	public static enum InputType{POST, EDIT};

	private final ArrayList<User> userInfoList = new ArrayList<User>();
	private final ArrayList<Tag> tagList = new ArrayList<Tag>();
	private final ArrayList<MessageView> messageList = new ArrayList<MessageView>();
	private final TalkActivity context;
	private final ViewGroup parent;
	public boolean[] tagItemSelectedListForSend;
	public final ArrayList<Integer> selectedTagIdListForReceive = new ArrayList<Integer>();
	public int myUserId; //未実装
	public InputType inputType; //現在入力中の投稿のタイプ
	public int editingPostId; //編集中の投稿のID
	public ArrayList<File> postFileData = new ArrayList<File>();


	public TalkManager(TalkActivity context, ViewGroup parent, int myUserId) {
		this.context = context;
		this.parent = parent;
		this.myUserId = myUserId;
	}

	public final void init() {

		//userInfoListの更新
		while (SCM.scm.loadUserInfo(userInfoList) == false)
			Toast.makeText(context, R.string.user_load_failed_text, Toast.LENGTH_SHORT).show();

		myUserId = userInfoList.get(0).getUserID();

		//tagListの読み込み
		while (SCM.scm.loadTagList(tagList) == false)
			Toast.makeText(context, R.string.tag_load_failed_text, Toast.LENGTH_SHORT).show();

		//送信用選択タグリストの作成
		tagItemSelectedListForSend = new boolean[tagList.size()];
		tagItemSelectedListForSend[0] = true;

		//初期状態でのタグ選択状況を設定
		selectedTagIdListForReceive.add(tagList.get(0).getTagID());

		//messageListの更新
		while (SCM.scm.loadMessage(selectedTagIdListForReceive, messageList, context, this) == false)
			Toast.makeText(context, R.string.message_load_failed_text, Toast.LENGTH_SHORT).show();

		//レイアウトの更新
		for (int i = 0; i < messageList.size(); i++) {
			MessageView mv = messageList.get(i);
			Animation anim = createAnimationOnInit(i, i == messageList.size() - 1);
			addMessageView(mv, anim);
			mv.setAnimation(anim);
			mv.invalidate();
		}
	}

	public final void loadNewMessage() {

		//userInfoListの読み込み
		userInfoList.clear();
		while (SCM.scm.loadUserInfo(userInfoList) == false)
			Toast.makeText(context, R.string.user_load_failed_text, Toast.LENGTH_SHORT).show();

		//tagListの読み込み
		tagList.clear();
		if (SCM.scm.loadTagList(tagList) == false) {
			Toast.makeText(context, R.string.tag_load_failed_text, Toast.LENGTH_SHORT).show();
			return;
		}

		ArrayList<MessageView> newMessageList = new ArrayList<MessageView>();
		//newMessageListの読み込み
		if (SCM.scm.loadMessage(selectedTagIdListForReceive, newMessageList, context, this) == false) {
			Toast.makeText(context, R.string.message_load_failed_text, Toast.LENGTH_SHORT).show();
			return;
		}

		context.scrollView.pageScroll(ScrollView.FOCUS_DOWN);

		for (int i = 0; i < newMessageList.size(); i++) {
			MessageView mv = newMessageList.get(i);
			Animation anim = createAnimationOnReflesh(i, i == newMessageList.size() - 1);
			addMessageView(mv, anim);
			mv.setAnimation(anim);
			mv.invalidate();
		}
		messageList.addAll(newMessageList);
	}

	public final void refreshOnTagSelectChanged() {

		//userInfoListの読み込み
		userInfoList.clear();
		while (SCM.scm.loadUserInfo(userInfoList) == false)
			Toast.makeText(context, R.string.user_load_failed_text, Toast.LENGTH_SHORT).show();

		//tagListの読み込み
		tagList.clear();
		while (SCM.scm.loadTagList(tagList) == false)
			Toast.makeText(context, R.string.tag_load_failed_text, Toast.LENGTH_SHORT).show();

		//送信用タグリストの作成
		tagItemSelectedListForSend = new boolean[tagList.size()];
		for (int i = 0; i < tagItemSelectedListForSend.length; i++) {
			//受信用タグリストを反映
			tagItemSelectedListForSend[i] = selectedTagIdListForReceive.contains(tagList.get(i).getTagID());
		}

		//messageListの更新
		messageList.clear();
		while (SCM.scm.loadMessage(selectedTagIdListForReceive, messageList, context, this) == false)
			Toast.makeText(context, R.string.message_load_failed_text, Toast.LENGTH_SHORT).show();

		//レイアウトの更新
		parent.removeAllViews();
		for (int i = 0; i < messageList.size(); i++) {
			MessageView mv = messageList.get(i);
			Animation anim = createAnimationOnInit(i, i == messageList.size() - 1);
			addMessageView(mv, anim);
			mv.setAnimation(anim);
			mv.invalidate();
		}
	}

	public final boolean submit(String text) throws Exception {
		ArrayList<Integer> fileId = new ArrayList<Integer>();
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		for (File data : postFileData) {
			String path = data.getFileName();
			byte[] fileData = MyUtils.readFileToByte(path);
			String[] s = path.split("/");
			Swallow.File file = SCM.scm.swallow.createFile(s[s.length-1], data.getFileType(), data.getTagID(), data.getFolderContent(), null, fileData);
			int fileID = file.getFileID();
			fileId.add(fileID);
			sp.edit().putString("F" + fileID, new String(fileData));
		}
		postFileData.clear();
		Swallow.Message mInfo = createMessage(text, fileId.toArray(new Integer[0]));
		if (SCM.scm.sendMessage(mInfo) == null) {
			Toast.makeText(context, R.string.message_send_failed_text, Toast.LENGTH_SHORT).show();
			return false;
		} else {
			MessageView mv = new MessageView(context, mInfo, this);
			Animation anim = createAnimationOnReflesh(5, true);
			addMessageView(mv, anim);
			//ViewFlipperのアニメーションとかぶるので、若干遅延させる
			mv.setAnimation(anim);
			mv.invalidate();
			return true;
		}
	}

	public final boolean editMessage(String text) throws Exception {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		ArrayList<Integer> fileId = new ArrayList<Integer>();
		for (File data : postFileData) {
			Integer fileID = data.getFileID();
			if (fileID == null) {
				String path = data.getFileName();
				byte[] fileData = MyUtils.readFileToByte(path);
				String[] s = path.split("/");
				Swallow.File file = SCM.scm.swallow.createFile(s[s.length-1], data.getFileType(), data.getTagID(), data.getFolderContent(), null, fileData);
				fileID = file.getFileID();
				sp.edit().putString("F" + fileID, new String(fileData));
			}
			fileId.add(fileID);
		}
		Message mInfo = createMessage(text, fileId.toArray(new Integer[0]));
		if (SCM.scm.editMessage(mInfo) == null) {
			Toast.makeText(context, R.string.message_send_failed_text, Toast.LENGTH_SHORT).show();
			return false;
		} else {
			for (int i = 0; i < parent.getChildCount(); i++) {
				MessageView child = (MessageView)parent.getChildAt(i);
				if (child.mInfo.getPostID() == editingPostId) {
					MessageView mv = new MessageView(context, mInfo, this);
					Animation anim = createAnimationOnReflesh(5, true);
					addMessageView(mv, anim);
					mv.setAnimation(anim);
					mv.invalidate();
					break;
				}
			}
			return true;
		}
	}

	//送信時にMessageViewを作成
	private final Message createMessage(String text, Integer[] fileId) {
		ArrayList<Integer> tagId = new ArrayList<Integer>();
		ArrayList<Integer> replyId = new ArrayList<Integer>();
		ArrayList<Integer> destId = new ArrayList<Integer>();
		ArrayList<String> enquete = new ArrayList<String>();
		for (int i = 0; i < tagItemSelectedListForSend.length; i++)
			if (tagItemSelectedListForSend[i])
				tagId.add(tagList.get(i).getTagID());
		Message mInfo = new Message(null, System.currentTimeMillis(), myUserId, text, null, fileId, tagId.toArray(new Integer[0]), replyId.toArray(new Integer[0]), destId.toArray(new Integer[0]), enquete.toArray(new String[0]), 0, null, 0, null, 0, null);
		return mInfo;
	}

	private final Animation createAnimationOnInit(int index, boolean lastFlag) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 1.0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.0f);
		animation.setDuration(500);
		animation.setStartOffset(1000 + (index)
				* 100);
		if (lastFlag) {
			animation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation paramAnimation) {
					context.scrollView.pageScroll(ScrollView.FOCUS_DOWN);
				}
				@Override
				public void onAnimationRepeat(Animation paramAnimation) {}

				@Override
				public void onAnimationEnd(Animation paramAnimation) {
					context.scrollView.pageScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}
		return animation;
	}

	private final Animation createAnimationOnReflesh(int index, boolean lastFlag) {
		TranslateAnimation animation = new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, -1.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
				TranslateAnimation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		animation.setStartOffset(300 + index * 100);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation paramAnimation) {
				context.scrollView.pageScroll(ScrollView.FOCUS_DOWN);
			}
			@Override
			public void onAnimationRepeat(Animation paramAnimation) {}

			@Override
			public void onAnimationEnd(Animation paramAnimation) {
				context.scrollView.pageScroll(ScrollView.FOCUS_DOWN);
			}
		});
		return animation;
	}

	private final void addMessageView(MessageView mv, Animation animation) {
		parent.addView(mv, mv.lp);
		LineView lv = new LineView(context);
		lv.setAnimation(animation);
		parent.addView(lv);
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

	public final User findUserById(int id) {
		for (User u : userInfoList)
			if (u.getUserID() == id)
				return u;
		return null;
	}

	public final Tag findTagById(int id) {
		for (Tag t : tagList)
			if (t.getTagID() == id)
				return t;
		return null;
	}

	public final Tag findTagByIndex(int index) {
		return tagList.get(index);
	}

	public final Tag findTagByName(String name) {
		for (Tag t : tagList)
			if (t.getTagName().equals(name))
				return t;
		return null;
	}

	public final int getTagNum() {
		return tagList.size();
	}

	public final void addTag(Tag t) {
		tagList.add(t);
	}

    public final Message findMessageById(int postId) {
        for (Message m : SCM.scm.swallow.findMessage(null, null, null, null, null, null, null, null, null, null, null, null, null)) {
            if (m.getPostID() == postId) {
                return m;
            }
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
		for (File f :  SCM.scm.swallow.findFile(null, null, null, null, null, null, null, null) ) {
			if (f.getFileID() == id)
				return f;
		}
		return null;
	}

	public final Integer[] getTagIdSelectedForSend() {
		ArrayList<Integer> array = new ArrayList<Integer>();
		for (int i = 0; i < tagList.size(); i++) {
			if (tagItemSelectedListForSend[i])
				array.add(tagList.get(i).getTagID());
		}
		Integer[] result = new Integer[array.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = array.get(i);
		return result;
	}

	public void run() {
		//画面サイズ取得
		WindowManager wm = (WindowManager)context.getSystemService(TalkActivity.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point size = new Point();
		disp.getSize(size);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		//既読をつける
        synchronized (messageList) {
            float y;
            for (MessageView mv : messageList) {
                y = mv.getY();
                //範囲内かつ未読なら既読つける
                if (-mv.getHeight() <= y && y <= size.y) {
                    boolean flag = true;
                    Swallow.Received[] receiveds = mv.mInfo.getReceived();
                    if (receiveds != null) {
                        for (Swallow.Received r : receiveds) {
                            if (r.getUserID() == myUserId) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            SCM.scm.swallow.createReceived(mv.mInfo.getPostID());
                            sp.edit().putBoolean("R" + mv.mInfo.getPostID(), true);
                        }
                    }

                }
            }
        }
	}
}
