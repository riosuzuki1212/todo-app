package com.example.todoapp.service;

import com.example.todoapp.entity.Todo; // データベースエンティティ
import com.example.todoapp.model.SolrTodoItem; // Solr用のデータモデル
import com.example.todoapp.repository.TodoRepository; // データベースリポジトリ
import org.springframework.stereotype.Service; // サービス層のコンポーネントであることを示す
import org.springframework.transaction.annotation.Transactional; // トランザクション管理のため

import java.io.IOException; // 入出力エラーのため
import java.util.List; // リストを扱うため
import java.util.Optional; // 値が存在しない可能性のある型を扱うため
import java.util.Date; // 日付を扱うため

import org.apache.solr.client.solrj.SolrServerException; // Solrサーバーエラーのため

@Service // Springがこのクラスをサービスとして管理することを示す
public class TodoService {

    private final TodoRepository todoRepository; // ToDoのデータベース操作を担当
    private final SolrTodoService solrTodoService; // Solrの操作を担当

    // コンストラクタインジェクション: SpringがTodoRepositoryとSolrTodoServiceを自動で注入する
    public TodoService(TodoRepository todoRepository, SolrTodoService solrTodoService) {
        this.todoRepository = todoRepository;
        this.solrTodoService = solrTodoService;
    }

    // すべてのToDoを取得するメソッド
    public List<Todo> findAllTodos() {
        return todoRepository.findAll();
    }

    // IDでToDoを取得するメソッド
    public Optional<Todo> findTodoById(Long id) {
        return todoRepository.findById(id);
    }

    // 新しいToDoを作成するメソッド
    @Transactional // データベース操作が単一のトランザクションとして扱われるようにする
    public Todo createTodo(Todo todo) throws IOException, SolrServerException {
        // 作成日時と更新日時をセット
        if (todo.getCreatedAt() == null) {
            todo.setCreatedAt(new Date());
        }
        todo.setUpdatedAt(new Date());

        // データベースに保存
        Todo savedTodo = todoRepository.save(todo);

        // Solrにもインデックス（登録）
        // SolrTodoItem に変換して SolrService に渡す
        solrTodoService.indexTodoItem(convertToSolrTodoItem(savedTodo));

        return savedTodo; // 保存されたToDoを返す
    }

    // 既存のToDoを更新するメソッド
    @Transactional // トランザクション管理
    public Optional<Todo> updateTodo(Long id, Todo updatedTodo) throws IOException, SolrServerException {
        return todoRepository.findById(id).map(todo -> {
            // 既存のToDoの情報を更新
            todo.setTitle(updatedTodo.getTitle());
            todo.setDescription(updatedTodo.getDescription());
            todo.setCompleted(updatedTodo.isCompleted());
            todo.setUpdatedAt(new Date()); // 更新日時をセット

            // データベースに保存（更a新）
            Todo savedTodo = todoRepository.save(todo);

            // Solrのインデックスも更新
            try {
                solrTodoService.indexTodoItem(convertToSolrTodoItem(savedTodo));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SolrServerException e) {
                throw new RuntimeException(String.valueOf(e));
            }

            return savedTodo; // 更新されたToDoを返す
        });
    }

    // ToDoを削除するメソッド
    @Transactional // トランザクション管理
    public void deleteTodo(Long id) throws IOException, SolrServerException {
        // データベースから削除
        todoRepository.deleteById(id);

        // Solrからも削除
        solrTodoService.deleteTodoItem(String.valueOf(id)); // SolrのIDはStringなので変換
    }

    // Solrを使ってToDoを検索するメソッド
    public List<SolrTodoItem> searchTodos(String query, int start, int rows) throws IOException, SolrServerException {
        return solrTodoService.searchTodoItems(query, start, rows);
    }

    // TodoエンティティをSolrTodoItemモデルに変換するヘルパーメソッド
    private SolrTodoItem convertToSolrTodoItem(Todo todo) {
        SolrTodoItem solrItem = new SolrTodoItem(
                String.valueOf(todo.getId()),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
        // Solrの単一値フィールド（title_s）にタイトルをコピー
        solrItem.setTitleExact(todo.getTitle()); //
        return solrItem;
    }
}