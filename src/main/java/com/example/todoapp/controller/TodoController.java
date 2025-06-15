package com.example.todoapp.controller; // コントローラーのある場所

import com.example.todoapp.entity.Todo; // ToDoの情報の型を使うよ
import com.example.todoapp.repository.TodoRepository; // データベースとやり取りするための変数
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // HTML画面を返すコントローラー
import org.springframework.ui.Model; // 画面にデータを渡すための変数
import org.springframework.web.bind.annotation.*; // URLやリクエストに使うクラス

import java.util.List; // ToDoをまとめて扱うためのリスト

@Controller // このクラスはHTML画面用のコントローラー
public class TodoController {

    @Autowired // 自動でtodoRepositoryの中身を入れてくれる
    private TodoRepository todoRepository; // データベースにアクセスするための変数

    // ① 初期表示：ToDoの一覧ページを表示
    @GetMapping("/") // 「/」のページにアクセスされたら動く
    public String index(Model model) { // modelにデータを詰めてHTMLに渡す
        List<Todo> todos = todoRepository.findAll(); // すべてのToDoをデータベースから取り出す
        model.addAttribute("todos", todos); // ToDoの一覧を画面に渡す
        model.addAttribute("todo", new Todo()); // 空のToDoも渡す（フォーム用）
        return "index"; // 「index.html」を返す
    }

    // ② 通常のToDo追加（画面リロードあり）
    @PostMapping("/add") // 「/add」にPOSTで送られたら動く
    public String addTodo(@ModelAttribute Todo todo) { // HTMLフォームからのToDoを受け取る
        todoRepository.save(todo); // データベースに保存
        return "redirect:/"; // リロードする
    }

    // ③ チェックの切り替え（画面リロードあり）
    @PostMapping("/toggle/{id}")
    public String toggleDone(@PathVariable Long id) {
        todoRepository.findById(id).ifPresent(todo -> {
            todo.setCompleted(!todo.isCompleted()); // 完了⇔未完了の切り替え
            todoRepository.save(todo); // 保存
        });
        return "redirect:/"; // リロード
    }

    // ④ ToDoの削除（画面リロードあり）
    @PostMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id) {
        todoRepository.deleteById(id); // 指定されたToDoを削除
        return "redirect:/";
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ↓↓↓ ここからは AJAX を使うための処理
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

    // ⑤ AJAX用：ToDo一覧を返す（JSON形式）
    @GetMapping("/api/todos")
    @ResponseBody // HTMLじゃなくてJSONを返す
    public List<Todo> getTodos() {
        return todoRepository.findAll(); // 全ToDoをJSONで返す
    }

    // ⑥ AJAX用：ToDoを新規追加（JSONを受け取って保存）
    @PostMapping("/api/todos")
    @ResponseBody
    public Todo createTodo(@RequestBody Todo todo) {
        return todoRepository.save(todo); // 受け取ったToDoを保存して返す
    }

    // ⑦ AJAX用：ToDoの完了/未完了を切り替える
    @PutMapping("/api/todos/{id}")
    @ResponseBody
    public Todo toggleTodo(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        todo.setCompleted(!todo.isCompleted()); // チェックを切り替える
        return todoRepository.save(todo); // 変更して保存
    }

    // ⑧ AJAX用：ToDoを削除する
    @DeleteMapping("/api/todos/{id}")
    @ResponseBody
    public void deleteTodoAjax(@PathVariable Long id) {
        todoRepository.deleteById(id); // 削除だけして何も返さない
    }
}
