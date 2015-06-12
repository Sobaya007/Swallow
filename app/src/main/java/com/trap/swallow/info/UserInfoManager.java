package com.trap.swallow.info;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.talk.MyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sobayaou on 2015/06/08.
 */
public final class UserInfoManager {

    private static final String MY_USER_ID_KEY = "MY_USER_ID";

    private static List<UserInfo> userInfoList = Collections.synchronizedList(new ArrayList<UserInfo>());
    private static UserInfo myUserInfo;

    public static boolean reload() {
        try {
            SCM.loadUserInfo(userInfoList);
            myUserInfo = new UserInfo(SCM.swallow.modifyUser(null, null, null, null, null, null, null, null, null, null, null));
            MyUtils.sp.edit().putInt(MY_USER_ID_KEY, myUserInfo.user.getUserID()).apply();
        } catch (SwallowException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static UserInfo findUserByID(int ID) {
        for (UserInfo u : userInfoList)
            if (u.user.getUserID() == ID)
                return u;
        reload();
        for (UserInfo u : userInfoList)
            if (u.user.getUserID() == ID)
                return u;
        return null;
    }

    public static int getUserNum() {
        return userInfoList.size();
    }

    public static UserInfo findUserByIndex(int index) {
        return userInfoList.get(index);
    }

    public static UserInfo getMyUserInfo() {
        return myUserInfo;
    }

    public static int getMyUserID() {
        return MyUtils.sp.getInt(MY_USER_ID_KEY, -1);
    }
}
