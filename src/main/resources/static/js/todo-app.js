// タスクをサーバーから読み込んで画面に表示する関数
function loadTodos(searchQuery = null) { // searchQurery 引数を追加
    let url = "/api/todos"; // デフォルトは全件取得API

    if (searchQuery) {
        url = `/api/todos/search?q=${encodeURIComponent(searchQuery)}`; // 検索API
    }

    $.get(url, function (data) {
        $("#todo-list").empty();

        data.forEach(function (todo) {
            const checkedClass = todo.completed ? "text-decoration-line-through" : "";

            // 表示するタイトルを title_s (titleExact) から取得
            // SolrTodoItem から返される場合は todo.titleExact になる
            // Todoエンティティから返される場合は todo.title になる
            const displayedTitle = todo.titleExact || todo.title;

            // 登録日時をフォーマット
            let createdAtDisplay = '';
            if (todo.createdAt) {
                // 日付オブジェクトに変換（APIからの文字列形式を考慮）
                const date = new Date(todo.createdAt);
                // 日本語のロケールで整形
                createdAtDisplay = date.toLocaleString('ja-JP', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            }

            const listItem = `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                        <span class="${checkedClass}">${displayedTitle}</span>
                        ${todo.description ? `<br><small class="text-muted">詳細: ${todo.description}</small>` : ''}
                        ${createdAtDisplay ? `<br><small class="text-muted">登録日時: ${createdAtDisplay}</small>` : ''} </div>
                    <div>
                        <button class="btn btn-sm btn-outline-success me-1 toggle-btn" data-id="${todo.id}">✓</button>
                        <button class="btn btn-sm btn-outline-danger delete-btn" data-id="${todo.id}">✕</button>
                    </div>
                </li>`;
            $("#todo-list").append(listItem);
        });
    }).fail(function(xhr, status, error) {
        console.error("ToDoリストの取得に失敗しました:", status, error);
        alert("ToDoリストの取得に失敗しました。サーバーログを確認してください。");
    });
}

// タスクを追加する処理（フォーム送信時）
$("#todo-form").submit(function (e) {
    e.preventDefault();
    const title = $("#todoTitle").val();
    // description もフォームから取得する場合、HTMLにid="todoDescription"の要素を追加し、以下を有効化
    // const description = $("#todoDescription").val();

    $.ajax({
        url: "/api/todos",
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            title: title,
            // description: description, // description も送る場合
            completed: false
        }),
        success: function () {
            $("#todoTitle").val("");
            // $("#todoDescription").val(""); // description 入力欄もクリアする場合
            loadTodos(); // 成功したらリストを更新
        },
        error: function(xhr, status, error) {
            console.error("ToDoの追加に失敗しました:", status, error);
            alert("ToDoの追加に失敗しました。サーバーログを確認してください。");
            // loadTodos(); // エラー時は強制的に再ロードしない方が良い場合もある
        }
    });
});

// ボタンを押して完了／未完了を切り替える処理
$("#todo-list").on("click", ".toggle-btn", function () {
    const id = $(this).data("id");
    $.ajax({
        url: `/api/todos/${id}`,
        method: "PUT",
        success: function () {
            loadTodos(); // 成功したらリストを更新
        },
        error: function(xhr, status, error) {
            console.error("ToDoの切り替えに失敗しました:", status, error);
            alert("ToDoの状態切り替えに失敗しました。サーバーログを確認してください。");
        }
    });
});

// ボタンを押して削除する処理
$("#todo-list").on("click", ".delete-btn", function () {
    const id = $(this).data("id");
    $.ajax({
        url: `/api/todos/${id}`,
        method: "DELETE",
        success: function () {
            loadTodos(); // 成功したらリストを更新
        },
        error: function(xhr, status, error) {
            console.error("ToDoの削除に失敗しました:", status, error);
            alert("ToDoの削除に失敗しました。サーバーログを確認してください。");
        }
    });
});

// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
// ↓↓↓ 検索機能のためのJavaScript ↓↓↓
// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

// 検索フォームの送信処理
$("#search-form").submit(function (e) {
    e.preventDefault(); // フォームのページ再読み込みを止める
    const query = $("#searchQuery").val(); // 検索キーワードを取得

    if (query.trim() === "") {
        loadTodos(); // 検索キーワードが空なら全件表示に戻す
    } else {
        loadTodos(query); // 検索キーワードでタスクを読み込む
    }
});

// ページを開いた時、最初に一度だけタスクを表示する
$(document).ready(function () {
    loadTodos();
});