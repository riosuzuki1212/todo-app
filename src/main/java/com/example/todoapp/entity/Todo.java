package com.example.todoapp.entity; // Todoというデータの場所はここ　実態？の認識

import jakarta.persistence.*; // データベースとつなげるためにインポート

@Entity // このクラスはデータベースのテーブル　宣言
public class Todo { // Todoという名前のデータの形を作る

    @Id // このidはデータのキーとして使う
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動で番号を1つずつ増やしてくれるっぽい
    private Long id; // ToDoのID　？？

    private String title; // やることの名前を格納

    private boolean completed; // やったかどうかを格納

    public Todo() { // 空のコンストラクタ（何も入ってないとき用）
    }

    public Todo(String title, boolean completed) { // ToDoの名前と状態をセットするときに使う
        this.title = title; // 名前をセット
        this.completed = completed; // 状態（やったか）をセット
    }

    public Long getId() { // idを取り出す
        return id;
    }

    public String getTitle() { // 名前を取り出す
        return title;
    }

    public boolean isCompleted() { // やったかどうかを取り出す
        return completed;
    }

    public void setId(Long id) {  // idをセットする 基本自動で取ってくれるっポイ
        this.id = id;
    }

    public void setTitle(String title) { // 名前を変更できる
        this.title = title;
    }

    public void setCompleted(boolean completed) { // やったかどうかの状態を変えられる
        this.completed = completed;
    }
}
