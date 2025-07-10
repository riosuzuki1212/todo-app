// タスクをサーバーから読み込んで画面に表示する関数
function loadTodos(searchQuery = null) { // searchQurery 引数を追加
    let url = "/api/todos"; // デフォルトは全件取得API

    if (searchQuery) {
        // 検索クエリがある場合、Solr検索用のAPIエンドポイントを使用
        // バックエンドが /api/todos/search?q=キーワード の形式をサポートしていることを前提
        url = `/api/todos/search?q=${encodeURIComponent(searchQuery)}`;
    }

    $.get(url, function (data) {
        $("#todo-list").empty();

        // サーバーから返されたデータが配列であることを確認
        // Solrからのレスポンス形式によっては、data.response.docs のようになる場合もあるため、
        // 必要に応じて data = data.response.docs; のように調整してください。
        if (!Array.isArray(data)) {
            console.error("APIから返されたデータが配列ではありません:", data);
            // エラーハンドリングまたは適切なデータ抽出ロジックを追加
            // 例: Solr直接のレスポンスの場合
            // if (data && data.response && Array.isArray(data.response.docs)) {
            //     data = data.response.docs;
            // } else {
            //     alert("ToDoリストのデータ形式が不正です。");
            //     return;
            // }
        }


        data.forEach(function (todo) {
            // ここで todo オブジェクトの構造をデバッグすることも有効です
            // console.log("Processing todo:", todo);

            const checkedClass = todo.completed ? "text-decoration-line-through" : "";

            // 表示するタイトルを title_strnew から取得し、なければ title を使用
            // Solrから取得した際に title_strnew が単一文字列になっていることを前提
            const displayedTitle = todo.title_strnew || todo.title;
            // console.log("Displayed Title:", displayedTitle); // デバッグ用

            // 登録日時をフォーマット
            let createdAtDisplay = '';
            if (todo.createdAt) {
                try {
                    // APIからの文字列形式 (ISO 8601) を考慮して日付オブジェクトに変換
                    const date = new Date(todo.createdAt);
                    // Dateオブジェクトが有効か確認
                    if (!isNaN(date.getTime())) {
                        // 日本語のロケールで整形
                        createdAtDisplay = date.toLocaleString('ja-JP', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit'
                        });
                    } else {
                        console.warn("Invalid date for createdAt:", todo.createdAt);
                    }
                } catch (e) {
                    console.error("Error parsing createdAt date:", todo.createdAt, e);
                }
            }

            // ToDo IDのバリデーションを強化
            // data-id属性には、サーバーから返されたtodo.idをそのまま使用します。
            // nullやundefined、"null" のチェックは、APIのデータが信頼できるなら通常不要です。
            // しかし、念のためここでは残しておきます。
            const todoId = (todo.id !== null && todo.id !== undefined && todo.id !== "null") ? todo.id : '';
            // console.log("Todo ID (after check):", todoId); // デバッグ用

            // IDが有効な場合のみリストアイテムを生成
            if (todoId === '') {
                console.warn("Skipping todo item due to invalid ID:", todo);
                return; // IDが無効な場合はこのアイテムをスキップ
            }

            const listItem = `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                        <span class="${checkedClass}">${displayedTitle}</span>
                        ${todo.description ? `<br><small class="text-muted">詳細: ${todo.description}</small>` : ''}
                        ${createdAtDisplay ? `<br><small class="text-muted">登録日時: ${createdAtDisplay}</small>` : ''}
                    </div>
                    <div>
                        <button class="btn btn-sm btn-outline-success me-1 toggle-btn" data-id="${todoId}">✓</button>
                        <button class="btn btn-sm btn-outline-danger delete-btn" data-id="${todoId}">✕</button>
                    </div>
                </li>`;
            $("#todo-list").append(listItem);
        });
    }).fail(function(xhr, status, error) {
        console.error("ToDoリストの取得に失敗しました:", status, error, xhr.responseText);
        alert("ToDoリストの取得に失敗しました。サーバーログを確認してください。");
    });
}

// タスクを追加する処理（フォーム送信時）
$("#todo-form").submit(function (e) {
    e.preventDefault();
    const title = $("#todoTitle").val();
    // description もフォームから取得する場合、HTMLにid="todoDescription"の要素を追加し、以下を有効化
    const description = $("#todoDescription").val(); // Assuming #todoDescription exists in HTML

    // タイトルが空の場合は追加しないなどのバリデーションを追加すると良い
    if (title.trim() === "") {
        alert("タイトルを入力してください。");
        return;
    }

    $.ajax({
        url: "/api/todos",
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            title: title,
            description: description, // description も送る場合
            completed: false
        }),
        success: function () {
            $("#todoTitle").val("");
            $("#todoDescription").val(""); // description 入力欄もクリアする場合
            loadTodos(); // 成功したらリストを更新
        },
        error: function(xhr, status, error) {
            console.error("ToDoの追加に失敗しました:", status, error, xhr.responseText);
            alert("ToDoの追加に失敗しました。サーバーログを確認してください。");
            // loadTodos(); // エラー時は強制的に再ロードしない方が良い場合もある
        }
    });
});

// ボタンを押して完了／未完了を切り替える処理
$("#todo-list").on("click", ".toggle-btn", function () {
    const id = $(this).data("id");
    if (!id) { // IDの存在チェックを追加
        console.error("Toggle: ToDo IDが取得できませんでした。");
        alert("ToDoの状態切り替えに失敗しました（ID不明）。");
        return;
    }

    // PUTリクエストは通常、ボディに更新内容を含む
    // このAPIがボディなしで状態を切り替える（トグルする）仕様であればOK
    // そうでなければ、現在の完了状態を取得し、その逆を送信する必要がある
    $.ajax({
        url: `/api/todos/${id}`,
        method: "PUT",
        success: function () {
            loadTodos(); // 成功したらリストを更新
        },
        error: function(xhr, status, error) {
            console.error("ToDoの切り替えに失敗しました:", status, error, xhr.responseText);
            alert("ToDoの状態切り替えに失敗しました。サーバーログを確認してください。");
        }
    });
});

// ボタンを押して削除する処理
$("#todo-list").on("click", ".delete-btn", function () {
    const id = $(this).data("id");
    if (!id) { // IDの存在チェックを追加
        console.error("Delete: ToDo IDが取得できませんでした。");
        alert("ToDoの削除に失敗しました（ID不明）。");
        return;
    }

    if (confirm('このTodoを削除しますか？')) {
        $.ajax({
            url: `/api/todos/${id}`,
            method: "DELETE",
            success: function () {
                loadTodos(); // 成功したらリストを更新
            },
            error: function(xhr, status, error) {
                console.error("ToDoの削除に失敗しました:", status, error, xhr.responseText);
                alert("ToDoの削除に失敗しました。サーバーログを確認してください。");
            }
        });
    }
});

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

// クリアボタンのクリック処理 (仮定: HTMLに #clearSearchBtn がある)
$("#clearSearchBtn").on("click", function() {
    $("#searchQuery").val(""); // 検索ボックスをクリア
    loadTodos(); // 全件表示に戻す
});


// ページを開いた時、最初に一度だけタスクを表示する
$(document).ready(function () {
    loadTodos();
});