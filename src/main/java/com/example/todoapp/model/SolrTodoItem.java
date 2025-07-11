package com.example.todoapp.model;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id; // Spring Data の Id アノテーション
import java.util.Date;

public class SolrTodoItem {

    @Id // Spring Data Solr がこのフィールドをドキュメントIDとして認識するために必要
    @Field("id") // Solrフィールド名とのマッピング
    private String id;

    @Field("title") // Solrの'title'フィールドにマップ
    private String title;

    @Field("description") // Solrの'description'フィールドにマップ
    private String description;

    @Field("completed") // Solrの'completed'フィールドにマップ
    private boolean completed; // boolean型

    @Field("createdAt") // Solrの'createdAt'フィールドにマップ
    private Date createdAt; // 登録日時

    @Field("updatedAt") // Solrの'updatedAt'フィールドにマップ
    private Date updatedAt; // 更新日時

    @Field("title_strnew") // (検索表示用)
    private String titlestrnew;


    public SolrTodoItem(String id, String title, String description, boolean completed, Date createdAt, Date updatedAt, String titleExact) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = completed; // boolean型
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.titlestrnew = titleExact;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; } // boolean型
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }


    public String getTitleExact() { return titlestrnew; }
    public void setTitleExact(String titleExact) { this.titlestrnew = titleExact; }
}