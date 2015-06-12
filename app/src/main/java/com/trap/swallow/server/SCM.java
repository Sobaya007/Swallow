package com.trap.swallow.server;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.trap.swallow.info.TagInfoManager;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.talk.MessageView;
import com.trap.swallow.talk.MyUtils;
import com.trap.swallow.talk.TalkActivity;
import com.trap.swallow.talk.TalkManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SCM {

	private static final int INITIAL_LOAD_MESSAGE_NUM = 10;
	private static final int ADDITIONAL_LOAD_MESSAGE_NUM = 10;

	public static Swallow swallow;
	public static SwallowSecurity sec;
	public static String userName;
	public static String password;
	private static long latestPostedTime;
	private static long oldestPostedTime;

	private SCM() {}

	public static Message sendMessage(String text, Integer[] fileID, Integer[] replyID, Integer[] tagID, Integer[] destID, String[] enquete) throws SwallowException {
		//空文字はサーバー側が受け付けないらしいので、全部nullに直す
		if (text != null && text.length() == 0)
			text = null;
		for (int i = 0; i < enquete.length; i++) {
			if (enquete[i] != null && enquete[i].length() == 0) {
				enquete[i] = null;
			}
		}
		return swallow.createMessage(text, fileID, tagID, replyID, destID, enquete, null);
	}

	public static Message editMessage(String text, Integer[] fileID, Integer[] replyID, Integer[] tagID, Integer[] destID, String[] enquete, int postID) throws SwallowException {
		//空文字はサーバー側が受け付けないらしいので、全部nullに直す
		if (text != null && text.length() == 0)
			text = null;
		for (int i = 0; i < enquete.length; i++) {
			if (enquete[i] != null && enquete[i].length() == 0) {
				enquete[i] = null;
			}
		}
		return swallow.createMessage(text, fileID, tagID, replyID, destID, enquete, postID);
	}

	public static void deleteMessage(int postID) throws SwallowException {
		swallow.createMessage(null, null, null, null, null, null, postID);
	}

	public static void initMessageList(List<MessageView> messageList) throws SwallowException {
		//タグのみで検索をかけて、インデックスで絞る
		Message[] messages = swallow.findMessage(0, INITIAL_LOAD_MESSAGE_NUM, null, null,
				null, null, TagInfoManager.getSelectedTagIDForReceive(),
				null, null, null, null, null, null);
		for (Message m : messages) {
			messageList.add(new MessageView(m));
		}
		if (messages.length > 0) {
			latestPostedTime = messages[0].getPosted();
			oldestPostedTime = messages[messages.length - 1].getPosted();
		} else {
			latestPostedTime = oldestPostedTime = System.currentTimeMillis();
		}
	}

	public static void loadOlderMessageToList(List<MessageView> messageList) throws SwallowException {
		//時間とタグで検索をかけて、インデックスで絞る
		Message[] messages = swallow.findMessage(0, ADDITIONAL_LOAD_MESSAGE_NUM, null,
				oldestPostedTime - 1, null, null, TagInfoManager.getSelectedTagIDForReceive(),
				null, null, null, null, null, null);
		for (Message m : messages) {
			messageList.add(new MessageView(m));
		}
		if (messages.length != 0)
			oldestPostedTime = messages[messages.length-1].getPosted();
	}

	public static void loadNewMessagesToList(List<MessageView> messageList) throws SwallowException {
		//時間とタグで検索をかけて、インデックスで絞る
		Message[] messages = swallow.findMessage(0, ADDITIONAL_LOAD_MESSAGE_NUM,
				latestPostedTime+1, null, null, null, TagInfoManager.getSelectedTagIDForReceive(),
				null, null, null, null, null, null);
		for (Message m : messages) {
			messageList.add(new MessageView(m));
		}
		if (messages.length != 0)
			latestPostedTime = messages[0].getPosted();
	}

	public static void loadOlderMessageToListUntil(List<MessageView> messageList, long until) throws SwallowException {
		//時間とタグで検索をかけて、インデックスで絞る
		Message[] messages = swallow.findMessage(0, Integer.MAX_VALUE/2, until+1,
				oldestPostedTime-1, null, null, TagInfoManager.getSelectedTagIDForReceive(),
				null, null, null, null, null, null);
		for (Message m : messages) {
			messageList.add(new MessageView(m));
		}
		if (messages.length != 0)
			oldestPostedTime = messages[messages.length-1].getPosted();
	}

//	public static void loadUserMessageToList(List<MessageView> messageList, int userID) throws SwallowException {
//		Message[] messages = swallow.findMessage(0, INITIAL_LOAD_MESSAGE_NUM, null, null,
//				null,
//		for (Message m : messages) {
//			messageList.add(new MessageView(m));
//		}
//		if (messages.length > 0) {
//			latestPostedTime = messages[0].getPosted();
//			oldestPostedTime = messages[messages.length - 1].getPosted();
//		} else {
//			latestPostedTime = oldestPostedTime = System.currentTimeMillis();
//		}
//	}

	public static final void loadTagList(List<Tag> tagInfoList) throws SwallowException {
		Tag[] tagArray;
		tagInfoList.clear();
		final int LOAD_NUM = 5;
		int index = 0;
		do {
			tagArray = swallow.findTag(index, index + LOAD_NUM, null, null, null, null, null);
			for (Tag t : tagArray) {
				tagInfoList.add(t);
			}
			index += LOAD_NUM;
		} while (tagArray.length == LOAD_NUM);
	}

	//タグ追加を送る
	public static final Tag sendAddTag(String tag, boolean invisible) throws SwallowException {
		return swallow.createTag(tag, invisible);
	}

	public static final void loadUserInfo(List<UserInfo> userInfo) throws SwallowException {
		final int LOAD_NUM = 10;
		int index = 0;
		userInfo.clear();
		do {
			User[] u = swallow.findUser(index, index + LOAD_NUM, null, null, null, null, null, null);
			for (User anU : u) userInfo.add(new UserInfo(anU));
			index += 5;
		} while (userInfo.size() == LOAD_NUM);
	}

}
