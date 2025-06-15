package com.example.todoapp;

import org.springframework.boot.SpringApplication; // Spring Boot（スプリングブート）を使うための道具だよ
import org.springframework.boot.autoconfigure.SpringBootApplication; // 自動で設定してくれる便利な道具を使うよ

@SpringBootApplication // Spring Bootのはここから始まる
public class TodoApplication { // アプリの本体メインのクラス
    public static void main(String[] args) { // アプリをスタートするときにまずここが動く。main関数みたいなもの？
        SpringApplication.run(TodoApplication.class, args); // Spring Bootのエンジンを動かす
    }
}
