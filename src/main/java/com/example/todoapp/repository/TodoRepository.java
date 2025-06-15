package com.example.todoapp.repository;

import com.example.todoapp.entity.Todo; // Todoのデータの形を使うよ
import org.springframework.data.jpa.repository.JpaRepository; // データベースを使いやすくする道具だよ

// TodoRepository はデータベースとやりとりするやつ
public interface TodoRepository extends JpaRepository<Todo, Long> {
    // 特別なことを書かなくても、データの保存・取り出し・削除などができるようになる
}
