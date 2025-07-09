package com.example.todoapp.controller;

// ... 既存のimport文 ...
import com.example.todoapp.entity.Todo;
import com.example.todoapp.model.SolrTodoItem; // Solr用のToDoアイテムの型を使う
import com.example.todoapp.repository.TodoRepository;
import com.example.todoapp.service.SolrTodoService; // Solrとやり取りするための変数
import org.springframework.http.HttpStatus; // HTTPステータスコードを返すため
import org.springframework.http.ResponseEntity; // HTTPレスポンス全体を操作するため

import java.io.IOException; // I/Oエラーを扱うため
import java.util.List; // リストを扱うため
import java.util.Optional; // nullチェックのため
import java.util.Date; // 日時を扱うため
import org.apache.solr.client.solrj.SolrServerException; // Solrサーバーエラーを扱うため
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller; // HTML画面を返すコントローラー
import org.springframework.ui.Model; // 画面にデータを渡すための変数
import org.springframework.web.bind.annotation.*; // URLやリクエストに使うクラス


@Controller
@RequestMapping // Controllerレベルでパスを指定しないことで、メソッドレベルで柔軟にパスを設定
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
    // ↓↓↓ ここからは AJAX を使うための処理
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

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

            solrTodoService.deleteTodoItem(String.valueOf(id));

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ↓↓↓ Solr検索のための新しいエンドポイント ↓↓↓
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

    // ⑨ AJAX用：SolrでToDoを検索する (title_s フィールドを使用)
    @GetMapping("/api/todos/search") // ★パスを変更せず、検索クエリパラメータで制御
    @ResponseBody
    public ResponseEntity<List<SolrTodoItem>> searchTodos(
            @RequestParam String q, // 検索クエリ（例: q=キーワード）
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int rows) {
        try {
            // qパラメータを使って title_s フィールドを検索
            List<SolrTodoItem> results = solrTodoService.searchTodoItems("title_s:" + q, start, rows); // ★検索クエリを title_s に指定
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (IOException | SolrServerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ↓↓↓ ヘルパーメソッド (エンティティとSolrモデルの変換) ↓↓↓
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

    private SolrTodoItem convertToSolrTodoItem(Todo todo) {
        SolrTodoItem solrItem = new SolrTodoItem(
                String.valueOf(todo.getId()),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
        solrItem.setTitleExact(todo.getTitle()); // title_s 用の値を設定
        return solrItem;
    }
}