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
import java.util.ArrayList; // ArrayList が必要な場合のみインポート
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        doc.addField("id", todoItem.getId());
        doc.addField("title", todoItem.getTitle());
        doc.addField("description", todoItem.getDescription());
        doc.addField("completed", todoItem.isCompleted());
        doc.addField("createdAt", todoItem.getCreatedAt());
        doc.addField("updatedAt", todoItem.getUpdatedAt());

        doc.addField("title_strnew", todoItem.getTitleExact());

        logger.info("Indexing document to Solr: {}", doc);
        // SolrClient が既にコアのURLで構築されているため、ハンドラー名のみを渡す
        solrClient.add(doc);
        solrClient.commit();
        logger.info("Document indexed and committed successfully: ID {}", todoItem.getId());
    }

    public void deleteTodoItem(String id) throws IOException, SolrServerException {
        logger.info("Deleting document from Solr: ID {}", id);
        // SolrClient が既にコアのURLで構築されているため、ハンドラー名のみを渡す
        solrClient.deleteById(id);
        solrClient.commit();
        logger.info("Document deleted and committed successfully: ID {}", id);
    }

    public List<SolrTodoItem> searchTodoItems(String query, int start, int rows) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(start);
        solrQuery.setRows(rows);

        //String queryPath = "/select";
        //logger.info("SolrClient query will be executed with path: {}", queryPath);
        logger.info("SolrClient query SolrQuery object: {}", solrQuery.toString());

        // SolrClient が既にコアのURLで構築されているため、ハンドラー名のみを渡す
        QueryResponse response = solrClient.query(solrQuery);
        logger.info("Solr query returned {} hits.", response.getResults().getNumFound());

        return response.getResults().stream()
                .map(doc -> {
                    String id = getStringValue(doc.getFieldValue("id"));
                    String title = getStringValue(doc.getFieldValue("title"));
                    String description = getStringValue(doc.getFieldValue("description"));
                    Boolean completed = getBooleanValue(doc.getFieldValue("completed"));
                    Date createdAt = getDateValue(doc.getFieldValue("createdAt"));
                    Date updatedAt = getDateValue(doc.getFieldValue("updatedAt"));

                    SolrTodoItem item = new SolrTodoItem(
                            id,
                            title,
                            description,
                            completed,
                            createdAt,
                            updatedAt
                    );

                    Object titleExactValue = doc.getFieldValue("title_strnew");
                    if (titleExactValue instanceof List) {
                        if (!((List<?>) titleExactValue).isEmpty()) {
                            item.setTitleExact(((List<?>) titleExactValue).get(0).toString());
                        } else {
                            item.setTitleExact(null);
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

    // ヘルパーメソッド群 (変更なし)
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

    private Date getDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : (Date) list.get(0);
        }
        return (Date) value;
    }
}
