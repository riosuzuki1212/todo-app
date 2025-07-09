package com.example.todoapp.entity; // Todoというデータの場所はここ　実態？の認識

import jakarta.persistence.*; // データベースとつなげるためにインポート
import java.util.Date; // Date型を使用するために追加

@Entity // このクラスはデータベースのテーブル　宣言
public class Todo { // Todoという名前のデータの形を作る

    @Id // このidはデータのキーとして使う
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動で番号を1つずつ増やしてくれるっぽい
    private Long id; // ToDoのID　？？
    private String title; // やることの名前を格納
    private String description; // ToDoの詳細を格納
    private boolean completed; // やったかどうかを格納
    private Date createdAt; // ToDoが作成された日時を格納
    private Date updatedAt; // ToDoが最後に更新された日時を格納

    public Todo() { // 空のコンストラクタ（何も入ってないとき用）
    }

    // ★コンストラクタを更新または追加
    public Todo(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        // createdAtとupdatedAtは通常、サービス層で設定されるか、

    }

    public Long getId() { // idを取り出す
        return id;
    }

    public String getTitle() { // 名前を取り出す
        return title;
    }

    public String getDescription() { // 詳細を取り出す
        return description;
    }

    public boolean isCompleted() { // やったかどうかを取り出す
        return completed;
    }

    public Date getCreatedAt() { // 作成日時を取り出す
        return createdAt;
    }

    public Date getUpdatedAt() { // 更新日時を取り出す
        return updatedAt;
    }

    public void setId(Long id) {  // idをセットする 基本自動で取ってくれるっポイ
        this.id = id;
    }

    public void setTitle(String title) { // 名前を変更できる
        this.title = title;
    }

    public void setDescription(String description) { // 詳細を変更できる
        this.description = description;
    }

    public void setCompleted(boolean completed) { // やったかどうかの状態を変えられる
        this.completed = completed;
    }

    public void setCreatedAt(Date createdAt) { // 作成日時をセットできる
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) { // 更新日時をセットできる
        this.updatedAt = updatedAt;
    }
}