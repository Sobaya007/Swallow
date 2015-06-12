package com.trap.swallow.info;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.trap.swallow.server.Swallow.Tag;
import com.trap.swallow.talk.MyUtils;

/**
 * Created by sobayaou on 2015/06/08.
 */
public final class TagInfoManager {

    private static final String SELECTED_TAG_KEY = "TAG_SELECTED3";
    private static final String NOTIFY_TAG_KEY = "TAG_NOTIFY3";

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
            TagInfoManager.TagInfo newTag = TagInfoManager.addTag("general", true);
            TagInfoManager.selectTag(newTag);
        }

        if (MyUtils.sp.getString(SELECTED_TAG_KEY, null) == null) {
            selectTag(TagInfoManager.findTagByIndex(0, true));
        }
        for (String selected : MyUtils.sp.getString(SELECTED_TAG_KEY, null).split(",")) {
            if (selected.length() > 0)
                findTagByID(Integer.parseInt(selected)).isSelected = true;
        }
        if (MyUtils.sp.getString(NOTIFY_TAG_KEY, null) == null)
            setAllNotification();
        return true;
    }

    public static TagInfo findTagByID(int ID) {
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagID() == ID)
                return t;
        return null;
    }

    public static TagInfo findTagByName(String name) {
        for (TagInfo t : tagInfoList)
            if (t.tag.getTagName().equals(name))
                return t;
        return null;
    }

    public static TagInfo findTagByIndex(int index, boolean visible) {
        int count = 0;
        for (TagInfo t : tagInfoList) {
            if (t.tag.getInvisible() == visible) continue;
            if (index == count) return t;
            count++;
        }
        return null;
    }

    public static TagInfo addTag(String name, boolean visible) {
        TagInfo newTag;
        try {
            newTag = new TagInfo(SCM.sendAddTag(name, !visible));
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
            int notificationID = Integer.parseInt(notification);
            result.add(findTagByID(notificationID));
        }
        return result.toArray(new TagInfo[0]);
    }

    public static Integer[] getNotificaionTagID() {
        ArrayList<Integer> result = new ArrayList<>();
        String[] notificationIDs = MyUtils.sp.getString(NOTIFY_TAG_KEY, null).split(",");
        for (String notification : notificationIDs) {
            int notificationID = Integer.parseInt(notification);
            result.add(notificationID);
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
        if (MyUtils.getReceivedFlag()) {
            array.add(3);
        }
        //幼女かどうかでタグを追加
        if (MyUtils.sp.getBoolean(MyUtils.YOJO_CHECK_KEY, false)) {
            array.add(4);
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
            if (t.isSelected) {
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
            if (!t.tag.getInvisible())
                count++;
        }
        return count;
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
