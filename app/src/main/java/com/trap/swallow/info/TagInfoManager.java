package com.trap.swallow.info;

import android.widget.CheckBox;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.swallow.R;
import com.trap.swallow.talk.MyUtils;
import com.trap.swallow.talk.TalkActivity;

/**
 * Created by sobayaou on 2015/06/08.
 */
public final class TagInfoManager {

    private static final String SELECTED_TAG_KEY = "TAG_SELECTED4";
    private static final String NOTIFY_TAG_KEY = "TAG_NOTIFY4";
    public static final int EDITED_TAG_ID = 1;
    public static final int DELETED_TAG_ID = 2;
    public static final int CONFIRMATION_TAG_ID = 3;
    public static final int YOJO_TAG_ID = 4;
    public static final int ANONYMOUS_TAG_ID = 5;
    public static final int IMPORTANT_TAG_ID = 6;

    private static List<TagInfo> tagInfoList = Collections.synchronizedList(new ArrayList<TagInfo>());

    public static boolean reload() {
        ArrayList<Tag> tagList = new ArrayList<>();
        try {
            SCM.loadTagList(tagList);
        } catch (SwallowException e) {
            e.printStackTrace();
            return false;
        }
        tagInfoList.clear();
        for (Tag t : tagList)
            tagInfoList.add(new TagInfo(t));

        if (!hasVisibleTag()) {
            TagInfo newTag = addTag("!general", null, true);
            selectTag(newTag);
        }

        for (TagInfo t : getSelectedTag()) {
            if (t == null) {
                MyUtils.sp.edit().putString(SELECTED_TAG_KEY,
                        Integer.toString(findTagByIndex(0, true).tag.getTagID())).apply();
                break;
            }
        }

        if (MyUtils.sp.getString(SELECTED_TAG_KEY, null) == null) {
            MyUtils.sp.edit().putString(SELECTED_TAG_KEY,
                    Integer.toString(findTagByIndex(0, true).tag.getTagID())).apply();
        }

        for (String selected : MyUtils.sp.getString(SELECTED_TAG_KEY, null).split(",")) {
            if (selected.length() > 0) {
                TagInfo tag = findTagByID(Integer.parseInt(selected));
                if (tag != null) {
                    tag.isSelected = true;
                } else {
                    selectTag(findTagByIndex(0, true));
                }
            }
        }

        if (MyUtils.sp.getString(NOTIFY_TAG_KEY, null) == null)
            setAllNotification();
        else {
            String[] str = MyUtils.sp.getString(NOTIFY_TAG_KEY, null).split(",");
            for (String s : str) {
                if (s.length() > 0) {
                    if (findTagByID(Integer.parseInt(s)) == null) {
                        setAllNotification();
                        break;
                    }
                }
            }
        }
        return true;
    }

    public static TagInfo findTagByID(int ID) {
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagID() == ID)
                return t;
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagID() == ID)
                return t;
        return null;
    }

    public static TagInfo findTagByName(String name) {
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagName().equals(name))
                return t;
        reload();
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagName().equals(name))
                return t;
        return null;
    }

    public static TagInfo findTagByIndex(int index, boolean visible) {
        int count = 0;
        for (TagInfo t : tagInfoList) {
            if (t.tag.getInvisible() == visible) continue;
            if (t.tag.getParticipant().length != 0) continue;
            if (index == count) return t;
            count++;
        }
        reload();

        count = 0;
        for (TagInfo t : tagInfoList) {
            if (t.tag.getInvisible() == visible) continue;
            if (t.tag.getParticipant().length != 0) continue;
            if (index == count) return t;
            count++;
        }
        return null;
    }

    public static TagInfo findGroupTagByIndex(int index) {
        int count = 0;
        for (TagInfo t : tagInfoList) {
            if (!MyUtils.contains(t.tag.getParticipant(), UserInfoManager.getMyUserID())) continue;
            if (index == count) return t;
            count++;
        }
        reload();

        count = 0;
        for (TagInfo t : tagInfoList) {
            if (!MyUtils.contains(t.tag.getParticipant(), UserInfoManager.getMyUserID())) continue;
            if (index == count) return t;
            count++;
        }
        return null;
    }

    public static TagInfo addTag(String name, Integer[] participants, boolean visible) {
        TagInfo newTag;
        try {
            newTag = new TagInfo(SCM.swallow.createTag(name, participants, !visible, null));
        } catch (SwallowException e) {
            e.printStackTrace();
            return null;
        }
        tagInfoList.add(newTag);
        return newTag;
    }

    public static boolean hasVisibleTag() {
        return getVisibleTagNum() > 0;
    }

    public static TagInfo[] getSelectedTag() {
        ArrayList<TagInfo> result = new ArrayList<>();
        for (TagInfo t : tagInfoList)
            if (t.isSelected)
                result.add(t);
        return result.toArray(new TagInfo[0]);
    }

    public static void selectTag(TagInfo tag) {
        tag.isSelected = !tag.isSelected;
        StringBuilder sb = new StringBuilder();
        for (TagInfo t : tagInfoList) {
            if (t.isSelected()) {
                sb.append(t.tag.getTagID());
                sb.append(',');
            }
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        MyUtils.sp.edit().putString(SELECTED_TAG_KEY, sb.toString()).apply();
    }

    public static void setNotification(String[] notifications) {
        StringBuilder sb = new StringBuilder();
        for (String notification : notifications) {
            TagInfo t = findTagByName(notification);
            sb.append(t.tag.getTagID());
            sb.append(',');
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        MyUtils.sp.edit().putString(NOTIFY_TAG_KEY, sb.toString()).apply();
    }

    public static TagInfo[] getNotification() {
        ArrayList<TagInfo> result = new ArrayList<>();
        String[] notificationIDs = MyUtils.sp.getString(NOTIFY_TAG_KEY, null).split(",");
        for (String notification : notificationIDs) {
            if (notification.length() > 0) {
                int notificationID = Integer.parseInt(notification);
                result.add(findTagByID(notificationID));
            }
        }
        return result.toArray(new TagInfo[0]);
    }

    public static Integer[] getNotificaionTagID() {
        ArrayList<Integer> result = new ArrayList<>();
        String[] notificationIDs = MyUtils.sp.getString(NOTIFY_TAG_KEY, null).split(",");
        for (String notification : notificationIDs) {
            if (notification.length() > 0) {
                int notificationID = Integer.parseInt(notification);
                result.add(notificationID);
            }
        }
        return result.toArray(new Integer[0]);
    }

    public static void setAllNotification() {
        StringBuilder sb = new StringBuilder();
        for (TagInfo t : tagInfoList) {
            if (!t.tag.getInvisible()) {
                sb.append(t.tag.getTagID());
                sb.append(',');
            }
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        MyUtils.sp.edit().putString(NOTIFY_TAG_KEY, sb.toString()).apply();
    }

    public static Integer[] getSelectedTagIDForSend() {
        ArrayList<Integer> array = new ArrayList<>();
        for (TagInfo tag : tagInfoList) {
            if (!tag.tag.getInvisible() && tag.isSelected())
                array.add(tag.tag.getTagID());
        }
        //既読をつけるかどうかでタグを追加
        if (MyUtils.getReceivedFlag() && !array.contains(CONFIRMATION_TAG_ID)) {
            array.add(CONFIRMATION_TAG_ID);
        }
        //幼女かどうかでタグを追加
        if (MyUtils.sp.getBoolean(MyUtils.YOJO_CHECK_KEY, false) && !array.contains(YOJO_TAG_ID)) {
            array.add(YOJO_TAG_ID);
        }
        //強制通知をつけるかどうかでタグを追加
        if (((CheckBox)TalkActivity.singleton.findViewById(R.id.mention_check)).isChecked()
                && !array.contains(IMPORTANT_TAG_ID)) {
            array.add(IMPORTANT_TAG_ID);
        }
        return array.toArray(new Integer[0]);
    }

    public static Integer[] getSelectedTagIDForReceive() {
        ArrayList<Integer> array = new ArrayList<>();
        for (TagInfo tag : tagInfoList) {
            if (!tag.tag.getInvisible() && tag.isSelected())
                array.add(tag.tag.getTagID());
        }
        return array.toArray(new Integer[0]);
    }

    public static void setSelection(Integer[] selectedTagID) {
        for (TagInfo t : tagInfoList) {
            t.isSelected = false;
            for (int ID : selectedTagID) {
                if (t.tag.getTagID() == ID) {
                    t.isSelected = true;
                    break;
                }
            }
        }
    }

    public static String getSelectedTagText() {
        StringBuilder sb = new StringBuilder();
        for (TagInfo t : tagInfoList) {
            if (t.isSelected && !t.tag.getInvisible()) {
                sb.append(t.tag.getTagName());
                sb.append(',');
            }
        }
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }


    public static int getVisibleTagNum() {
        int count = 0;
        for (TagInfo t : tagInfoList) {
            if (!t.tag.getInvisible() && t.tag.getParticipant().length == 0)
                count++;
        }
        return count;
    }

    public static int getGroupTagNum() {
        int count = 0;
        for (TagInfo t : tagInfoList) {
            if (t.tag.getInvisible()) continue;
            if (t.tag.getParticipant().length == 0) continue;
            if (!MyUtils.contains(t.tag.getParticipant(), UserInfoManager.getMyUserID())) continue;
                count++;
        }
        return count;
    }

    public static final Integer[] getObserveTagIDInRunning() {
        ArrayList<Integer> result = new ArrayList<>();
        for (TagInfo tagInfo : tagInfoList) {
            if (tagInfo.isSelected())
                result.add(tagInfo.tag.getTagID());
        }
        return result.toArray(new Integer[0]);
    }

    public static class TagInfo {

        public final Swallow.Tag tag;
        private boolean isSelected = false;

        private TagInfo(Swallow.Tag tag) {
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            return ((TagInfo)o).tag.getTagID() == this.tag.getTagID();
        }

        public boolean isSelected() {
            return isSelected;
        }
    }

}
