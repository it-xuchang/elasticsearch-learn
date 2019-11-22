package com.xuchang.common;

public class News {
    private String id;
    private String newId;
    private String newsTitle;
    private String newsContont;
    private int readCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNewId() {
        return newId;
    }

    public void setNewId(String newId) {
        this.newId = newId;
    }

    public String getNewsTitle() {
        return newsTitle;
    }

    public void setNewsTitle(String newsTitle) {
        this.newsTitle = newsTitle;
    }

    public String getNewsContont() {
        return newsContont;
    }

    public void setNewsContont(String newsContont) {
        this.newsContont = newsContont;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }
}
