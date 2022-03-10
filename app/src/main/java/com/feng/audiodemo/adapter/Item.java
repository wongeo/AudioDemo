package com.feng.audiodemo.adapter;


public class Item {

    private String mName;

    private String mUri;

    public Item(String name, String uri) {
        mName = name;
        mUri = uri;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = uri;
    }
}
