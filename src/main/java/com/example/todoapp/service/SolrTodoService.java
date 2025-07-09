package com.example.todoapp.service;

import com.example.todoapp.model.SolrTodoItem;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.ArrayList;
import org.apache.solr.common.SolrDocument;

@Service
public class SolrTodoService {

    private final SolrClient solrClient;
    private static final String COLLECTION_NAME = "todo_items";

    public SolrTodoService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void indexTodoItem(SolrTodoItem todoItem) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", todoItem.getId());
        doc.addField("title", todoItem.getTitle());
        doc.addField("description", todoItem.getDescription());
        doc.addField("completed", todoItem.isCompleted());
        doc.addField("createdAt", todoItem.getCreatedAt());
        doc.addField("updatedAt", todoItem.getUpdatedAt());

        doc.addField("title_s", todoItem.getTitleExact());

        solrClient.add(COLLECTION_NAME, doc);
        solrClient.commit(COLLECTION_NAME);
    }

    public void deleteTodoItem(String id) throws IOException, SolrServerException {
        solrClient.deleteById(COLLECTION_NAME, id);
        solrClient.commit(COLLECTION_NAME);
    }

    public List<SolrTodoItem> searchTodoItems(String query, int start, int rows) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(start);
        solrQuery.setRows(rows);

        QueryResponse response = solrClient.query(COLLECTION_NAME, solrQuery);
        return response.getResults().stream()
                .map(doc -> {
                    // 各フィールドの値を取得し、Stringに変換する関数
                    String id = getStringValue(doc.getFieldValue("id"));
                    String title = getStringValue(doc.getFieldValue("title"));
                    String description = getStringValue(doc.getFieldValue("description"));
                    Boolean completed = getBooleanValue(doc.getFieldValue("completed")); // Boolean型用
                    Date createdAt = getDateValue(doc.getFieldValue("createdAt")); // Date型用
                    Date updatedAt = getDateValue(doc.getFieldValue("updatedAt")); // Date型用

                    SolrTodoItem item = new SolrTodoItem(
                            id,
                            title,
                            description,
                            completed,
                            createdAt,
                            updatedAt
                    );

                    // title_s の値も同様に処理
                    Object titleExactValue = doc.getFieldValue("title_s");
                    if (titleExactValue instanceof List) {
                        if (!((List<?>) titleExactValue).isEmpty()) {
                            item.setTitleExact(((List<?>) titleExactValue).get(0).toString());
                        } else {
                            item.setTitleExact(null); // または空文字列
                        }
                    } else if (titleExactValue != null) {
                        item.setTitleExact(titleExactValue.toString());
                    } else {
                        item.setTitleExact(null);
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ★ヘルパーメソッド群を追加

    // Object から String を安全に取得するヘルパー
    private String getStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : list.get(0).toString();
        }
        return value.toString();
    }

    // Object から Boolean を安全に取得するヘルパー
    private Boolean getBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : Boolean.valueOf(list.get(0).toString());
        }
        return Boolean.valueOf(value.toString());
    }

    // Object から Date を安全に取得するヘルパー
    private Date getDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : (Date) list.get(0); // Date型は直接キャストできるはず
        }
        return (Date) value; // Date型は直接キャストできるはず
    }
}