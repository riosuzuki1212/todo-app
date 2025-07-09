package com.example.todoapp.controller;

import com.example.todoapp.entity.Todo; // ToDoの情報の型を使う
import com.example.todoapp.repository.TodoRepository; // データベースとやり取りするための変数
import com.example.todoapp.model.SolrTodoItem; // Solr用のToDoアイテムの型を使う
import com.example.todoapp.service.SolrTodoService; // Solrとやり取りするための変数
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // HTTPステータスコードを返すため
import org.springframework.http.ResponseEntity; // HTTPレスポンス全体を操作するため
import org.springframework.stereotype.Controller; // HTML画面を返すコントローラー
import org.springframework.ui.Model; // 画面にデータを渡すための変数
import org.springframework.web.bind.annotation.*; // URLやリクエストに使うクラス

import java.io.IOException; // I/Oエラーを扱うため
import java.util.List; // リストを扱うため
import java.util.Optional; // nullチェックのため
import java.util.Date; // 日時を扱うため
import org.apache.solr.client.solrj.SolrServerException; // Solrサーバーエラーを扱うため


@Controller
@RequestMapping
public class TodoController {

    private final TodoRepository todoRepository;
    private final SolrTodoService solrTodoService;

    @Autowired
    public TodoController(TodoRepository todoRepository, SolrTodoService solrTodoService) {
        this.todoRepository = todoRepository;
        this.solrTodoService = solrTodoService;
    }

    // ① 初期表示：ToDoの一覧ページを表示 (データベースから取得)
    @GetMapping("/")
    public String index(Model model) {
        List<Todo> todos = todoRepository.findAll();
        model.addAttribute("todos", todos);
        model.addAttribute("todo", new Todo());
        return "index";
    }

    // ② 通常のToDo追加（画面リロードあり）
    @PostMapping("/add")
    public String addTodo(@ModelAttribute Todo todo) {
        if (todo.getCreatedAt() == null) {
            todo.setCreatedAt(new Date());
        }
        todo.setUpdatedAt(new Date());
        todoRepository.save(todo);
        return "redirect:/";
    }

    // ③ チェックの切り替え（画面リロードあり）
    @PostMapping("/toggle/{id}")
    public String toggleDone(@PathVariable Long id) {
        todoRepository.findById(id).ifPresent(todo -> {
            todo.setCompleted(!todo.isCompleted());
            todo.setUpdatedAt(new Date());
            todoRepository.save(todo);
        });
        return "redirect:/";
    }

    // ④ ToDoの削除（画面リロードあり）
    @PostMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id) {

        todoRepository.deleteById(id);
        return "redirect:/";
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ↓↓↓ ここからは AJAX を使うための処理 ↓↓↓
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

    // ⑤ AJAX用：ToDo一覧を返す（JSON形式） - データベースから全件取得
    @GetMapping("/api/todos")
    @ResponseBody
    public List<Todo> getTodos() {
        return todoRepository.findAll();
    }

    // ⑥ AJAX用：ToDoを新規追加（JSONを受け取って保存）
    @PostMapping("/api/todos")
    @ResponseBody
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        try {
            if (todo.getCreatedAt() == null) {
                todo.setCreatedAt(new Date());
            }
            todo.setUpdatedAt(new Date());
            Todo savedTodo = todoRepository.save(todo);

            // Solr連携はここで行われている
            solrTodoService.indexTodoItem(convertToSolrTodoItem(savedTodo));

            return new ResponseEntity<>(savedTodo, HttpStatus.CREATED);
        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ⑦ AJAX用：ToDoの完了/未完了を切り替える
    @PutMapping("/api/todos/{id}")
    @ResponseBody
    public ResponseEntity<Todo> toggleTodo(@PathVariable Long id) {
        try {
            Optional<Todo> optionalTodo = todoRepository.findById(id);
            if (optionalTodo.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Todo todo = optionalTodo.get();
            todo.setCompleted(!todo.isCompleted());
            todo.setUpdatedAt(new Date());
            Todo updatedTodo = todoRepository.save(todo);

            // Solr連携はここで行われている
            solrTodoService.indexTodoItem(convertToSolrTodoItem(updatedTodo));

            return new ResponseEntity<>(updatedTodo, HttpStatus.OK);
        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ⑧ AJAX用：ToDoを削除する
    @DeleteMapping("/api/todos/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTodoAjax(@PathVariable Long id) {
        try {
            if (!todoRepository.existsById(id)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            todoRepository.deleteById(id);

            // Solr連携はここで行われている
            solrTodoService.deleteTodoItem(String.valueOf(id));

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ★ ヘルパーメソッド (convertToSolrTodoItem)
    private SolrTodoItem convertToSolrTodoItem(Todo todo) {
        SolrTodoItem solrItem = new SolrTodoItem(
                String.valueOf(todo.getId()),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );

        return solrItem;
    }
}