package com.example.todoapp.model;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import java.util.Date;

public class SolrTodoItem {

    @Id
    @Field("id")
    private String id;

    @Field("title") // 分析されるタイトル
    private String title;

    @Field("description")
    private String description;

    @Field("completed")
    private boolean completed;

    @Field("createdAt") // 登録日時
    private Date createdAt;

    @Field("updatedAt") // 更新日時
    private Date updatedAt;

    @Field("title_s") // ★単一値のタイトル (検索表示用)
    private String titleExact;

    public SolrTodoItem(String id, String title, String description, Boolean completed, Date createdAt, Date updatedAt) {}

    // コンストラクタを更新
    public SolrTodoItem(String id, String title, String description, boolean completed, Date createdAt, Date updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.titleExact = title; // コンストラクタで titleExact も初期化
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getTitleExact() { return titleExact; }
    public void setTitleExact(String titleExact) { this.titleExact = titleExact; }
}