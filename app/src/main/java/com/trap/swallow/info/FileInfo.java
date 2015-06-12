package com.trap.swallow.info;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.talk.MyUtils;

import java.io.InputStream;

public class FileInfo {

    public String fileName;
    String mimeType;
    Integer[] tagIDs;
    Integer[] folderContent;
    Integer overwriteFileID;
    public byte[]  data;
    public Bitmap bmp;

    public FileInfo(ContentResolver contentResolver, Resources resources, Uri uri) throws Exception {

        String scheme = uri.getScheme();
        String path = null;
        if ("file".equals(scheme)) {
            path = uri.getPath();
        } else if("content".equals(scheme)) {
            Cursor cursor = contentResolver.query(uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                path = cursor.getString(0);
                cursor.close();
            }
        }
        String[] strs = path.split("/");

        this.fileName = strs[strs.length-1];
        this.mimeType = MyUtils.getMimeType(path);
        this.tagIDs = tagIDs;
        this.data = MyUtils.readFileToByte(path);
        this.folderContent = null;
        this.overwriteFileID = null;
        if (mimeType.startsWith("image")) {
            InputStream in = contentResolver.openInputStream(uri);
            bmp = BitmapFactory.decodeStream(in);
            in.close();
        } else {
            bmp = MyUtils.getImageFromPath(resources, mimeType);
        }
    }

    public Swallow.File send() throws SwallowException {
        return SCM.swallow.createFile(fileName, mimeType, tagIDs, folderContent, overwriteFileID, data);
    }
}
