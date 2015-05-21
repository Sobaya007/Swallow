package com.trap.swallow.server;

import java.io.IOException;
import java.util.ArrayList;

import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.talk.MessageView;
import com.trap.swallow.talk.TalkActivity;
import com.trap.swallow.talk.TalkManager;

public class SCM {

	public static SCM scm;

	public Swallow swallow;
	public byte[] session;

	public SCM(Swallow swallow, byte[] session) {
		this.swallow = swallow;
		this.session = session;
	}

	public Message sendMessage(Message mf) {
		Message m = swallow.createMessage(mf.getMessage(), mf.getAttribute(), mf.getFileID(), mf.getReply(), mf.getTagID(), mf.getDest(), mf.getEnquete(), null);
		return m;
	}

	public Message editMessage(Message mf) {
		Message m = swallow.createMessage(mf.getMessage(), mf.getAttribute(), mf.getFileID(), mf.getReply(), mf.getTagID(), mf.getDest(), mf.getEnquete(), mf.getPostID());
		return m;
	}

	public boolean deleteMessage(int postId) {
		swallow.createMessage(null, null, null, null, null, null, null, postId);
		return true;
	}

	//配列の整理も含めてやる
	public boolean loadMessage(ArrayList<Integer> tags, ArrayList<MessageView> messages, final TalkActivity activity, TalkManager tvManager) {

		//サーバーにメッセージリクエストを飛ばす
		Message[] mInfoList = postFindMessage(tags);

		for (Message mInfo : mInfoList) {
			if (mInfo.getAttribute() == null || mInfo.getAttribute().length == 0) {//post
				try {
					MessageView mv = new MessageView(activity, mInfo, tvManager);
					//メッセージリストに新規メッセージを追加
					messages.add(mv);
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
					return false;
				}
			}
		}

		return true;
	}

	public final boolean loadTagList(ArrayList<Tag> tagList) {
		Tag[] t = swallow.findTag(null, null, null, null, null, null, null, null);
		for (int i = 0; i < t.length; i++)
			tagList.add(t[i]);
		return true;
	}

	//タグ追加を送る
	public final boolean sendAddTag(String tag, int[] resultId) {
		Tag t = swallow.createTag(tag);
		resultId[0] = t.getTagID();
		return true;
	}

	public final boolean loadUserInfo(ArrayList<User> userInfo) {
		User[] u = swallow.findUser(null, null, null, null, null, null, null, null);
		for (int i = 0; i < u.length; i++)
			userInfo.add(u[i]);
		return true;
	}

	private final Message[] postFindMessage(ArrayList<Integer> tagIdList) {
		Integer[] list = new Integer[tagIdList.size()];
		Message[] m = swallow.findMessage(0, 50, null, null, null,
				null, list, null, null, null, null, null, null);
		return m;
	}



	/*final int userId = 0;//擬似
	Calendar now = Calendar.getInstance(); //擬似
	String text = "あいうえおかきくけこさしすせお"
	String date = now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
	MessageView mv = new MessageView(addTarget, generalPostId++, userId, date, text,
			this);

	parent.addView(mv, mv.lp);
	TranslateAnimation animation = new TranslateAnimation(
			TranslateAnimation.RELATIVE_TO_SELF, -1.0f,
			TranslateAnimation.RELATIVE_TO_SELF, 0,
			TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
			TranslateAnimation.RELATIVE_TO_SELF, 0.0f);
	animation.setDuration(500);
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
	mv.setAnimation(animation);
	mv.invalidate();*/
}
