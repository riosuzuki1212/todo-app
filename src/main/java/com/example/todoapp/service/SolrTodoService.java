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
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.solr.client.solrj.util.ClientUtils;

@Service
public class SolrTodoService {

    private static final Logger logger = LoggerFactory.getLogger(SolrTodoService.class);

    private final SolrClient solrClient;
    // private static final String COLLECTION_NAME = "todo_items";

    public SolrTodoService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void indexTodoItem(SolrTodoItem todoItem) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();

        // IDがnullでないことを確認してから追加
        if (todoItem.getId() != null) {
            doc.addField("id", todoItem.getId()); // SolrのIDはString型なので、Stringとして追加
        } else {
            logger.warn("Attempted to index TodoItem with null ID. Skipping document: {}", todoItem.getTitle());
            return;
        }

        doc.addField("title", todoItem.getTitle());
        doc.addField("description", todoItem.getDescription());
        doc.addField("completed", todoItem.isCompleted()); // boolean型
        doc.addField("createdAt", todoItem.getCreatedAt());
        doc.addField("updatedAt", todoItem.getUpdatedAt());


        if (todoItem.getTitleExact() != null) {
            doc.addField("title_strnew", todoItem.getTitleExact());
        } else {
            doc.addField("title_strnew", todoItem.getTitle());
        }

        logger.info("Indexing document to Solr: {}", doc);
        solrClient.add(doc);
        solrClient.commit();
        logger.info("Document indexed and committed successfully: ID {}", todoItem.getId());
    }

    public void deleteTodoItem(String id) throws IOException, SolrServerException {
        logger.info("Deleting document from Solr: ID {}", id);
        solrClient.deleteById(id);
        solrClient.commit();
        logger.info("Document deleted and committed successfully: ID {}", id);
    }

    //　検索
    public List<SolrTodoItem> searchTodoItems(String query, int start, int rows) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();


        if (query != null && !query.trim().isEmpty()) {
            solrQuery.setQuery(query); // クエリが空でない場合はそのまま設定
            solrQuery.set("defType", "edismax"); // edismaxクエリタイプを使用
            solrQuery.set("q.op", "AND"); // AND条件で検索
            solrQuery.set("qf", "title_strnew description"); // title_strnew と description の両方で検索
        } else {
            solrQuery.setQuery("*:*"); // クエリが空の場合は全件検索
        }

        solrQuery.setStart(start);
        solrQuery.setRows(rows);

        logger.info("SolrClient query SolrQuery object: {}", solrQuery.toString());

        QueryResponse response = solrClient.query(solrQuery);
        logger.info("Solr query returned {} hits.", response.getResults().getNumFound());

        return response.getResults().stream()
                .map(doc -> {
                    String id = getStringValue(doc.getFieldValue("id"));
                    String title = getStringValue(doc.getFieldValue("title"));
                    String description = getStringValue(doc.getFieldValue("description"));

                    //受け取り
                    Boolean completedFromSolr = getBooleanValue(doc.getFieldValue("completed"));

                    Date createdAt = getDateValue(doc.getFieldValue("createdAt"));
                    Date updatedAt = getDateValue(doc.getFieldValue("updatedAt"));

                    // SolrTodoItemのコンストラクタに渡すための title_strnew の値を取得
                    String titleExactFromSolr = getStringValue(doc.getFieldValue("title_strnew"));

                    // SolrTodoItemの新しいコンストラクタを呼び出し
                    SolrTodoItem item = new SolrTodoItem(
                            id,
                            title,
                            description,
                            completedFromSolr != null ? completedFromSolr : false, // ここでbooleanに変換
                            createdAt,
                            updatedAt,
                            titleExactFromSolr // Solrからとったtitle_strnew の値
                    );

                    return item;
                })
                .collect(Collectors.toList());
    }

    // ヘルパーメソッド群
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

    private Boolean getBooleanValue(Object value) { // 戻り値は Boolean のまま
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : Boolean.valueOf(list.get(0).toString());
        }
        return Boolean.valueOf(value.toString());
    }

    private Date getDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;

            if (!list.isEmpty() && list.get(0) instanceof Date) {
                return (Date) list.get(0);
            } else if (!list.isEmpty() && list.get(0) instanceof String) {

                try {
                    return Date.from(java.time.Instant.parse((String) list.get(0)));
                } catch (Exception e) {
                    logger.error("Failed to parse date string from list: {}", list.get(0), e);
                    return null;
                }
            }
            return null;
        }
        // 単一値が文字列の場合
        if (value instanceof String) {
            try {
                return Date.from(java.time.Instant.parse((String)value));
            } catch (Exception e) {
                logger.error("Failed to parse date string: {}", value, e);
                return null;
            }
        }
        // 単一値がDateオブジェクトの場合
        if (value instanceof Date) {
            return (Date) value;
        }
        logger.warn("Unexpected type for date value: {}", value.getClass().getName());
        return null; // 予期せぬ型の場合
    }
}