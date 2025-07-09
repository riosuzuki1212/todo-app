// タスクをサーバーから読み込んで画面に表示する関数
function loadTodos() {
    $.get("/api/todos", function (data) {
        // サーバーに今あるタスク全部もらう（GETリクエスト）

        $("#todo-list").empty();
        // 今表示されているリストを全部消す（毎回新しく表示し直すため）

        data.forEach(function (todo) {
            // サーバーから返ってきたタスクリスト（配列）を1つずつ取り出して使う

            const checkedClass = todo.completed ? "text-decoration-line-through" : "";
            // タスクが完了済みだったら取り消し線のクラスをつける

            // 1つのタスクをHTMLの形にする（タイトルと2つのボタン）
            const listItem = `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <span class="${checkedClass}">${todo.title}</span>
                    <div>
                        <button class="btn btn-sm btn-outline-success me-1 toggle-btn" data-id="${todo.id}">✓</button>
                        <button class="btn btn-sm btn-outline-danger delete-btn" data-id="${todo.id}">✕</button>
                    </div>
                </li>`;
            $("#todo-list").append(listItem);
            // 作ったHTMLを画面のリストに追加する
        });
    });
}

// タスクを追加する処理（フォーム送信時）
$("#todo-form").submit(function (e) {
    e.preventDefault();
    // フォームのページを再読み込みを止める

    const title = $("#todoTitle").val();
    // 入力されたタスク名を取得

    // サーバーにタスクを新しく追加してもらう
    $.ajax({
        url: "/api/todos",               // 送る先（URL）
        method: "POST",                  // 新しく作るのでPOSTを使う
        contentType: "application/json", // JSON形式のデータを送る
        data: JSON.stringify({
            title: title,
            completed: false
        }), // タスクのデータをJSON形式で送る

        success: function () {
            $("#todoTitle").val("");
            // 入力欄を空っぽに戻す
            loadTodos();
            // タスクリストを更新して、画面に反映する
        }
    });
});

// ボタンを押して完了／未完了を切り替える処理
$("#todo-list").on("click", ".toggle-btn", function () {
    const id = $(this).data("id");
    // 押されたボタンがどのタスクなのかIDを調べる

    $.ajax({
        url: `/api/todos/${id}`, // そのタスクだけを指定したURLにする
        method: "PUT",           // 更新なのでPUTを使う

        success: function () {
            loadTodos(); // 成功したら、画面を更新
        }
    });
});

// ボタンを押して削除する処理
$("#todo-list").on("click", ".delete-btn", function () {
    const id = $(this).data("id");
    // どのタスクを削除するのかを調べる

    $.ajax({
        url: `/api/todos/${id}`,
        method: "DELETE", // 削除なのでDELETEを使う

        success: function () {
            loadTodos(); // 成功したらリストを更新して反映
        }
    });
});

// ページを開いた時、最初に一度だけタスクを表示する
$(document).ready(function () {
    loadTodos();
    // 初期表示のためにタスクをサーバーから読み込む
});
