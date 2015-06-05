package com.trap.swallow.info;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.trap.swallow.server.Swallow;
import com.trap.swallow.talk.MyUtils;

import java.io.ByteArrayInputStream;

/**
 * Created by sobayaou on 2015/06/04.
 */
public class UserInfo {

    public Swallow.User user;
    public Bitmap profileImage;

    public UserInfo(Swallow.User user) {
        this.user = user;
        if (user.getImage() != null)
        this.profileImage = BitmapFactory.decodeStream(new ByteArrayInputStream(MyUtils.getThumbnailByteArray(user.getImage(), 100, 100)));
    }
}
