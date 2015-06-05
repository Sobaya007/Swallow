package com.trap.swallow.info;

/**
 * Created by Sobaya on 2015/05/23.
 */
public class TagInfo {

    public String tagName;
    public int tagID;
    public boolean isSelected = false;

    public TagInfo(String tagName, int tagID) {
        this.tagName = tagName;
        this.tagID = tagID;
    }

    @Override
    public boolean equals(Object o) {
        return ((TagInfo)o).tagID == this.tagID;
    }
}
