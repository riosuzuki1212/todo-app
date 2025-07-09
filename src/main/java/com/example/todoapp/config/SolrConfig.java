package com.example.todoapp.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SolrConfig {

    private static final Logger logger = LoggerFactory.getLogger(SolrConfig.class);

    @Value("${solr.host}")
    private String solrHost; // application.properties から "http://localhost:8983/solr" を受け取る想定
    private static final String COLLECTION_NAME = "todo_items"; // コア名

    @Bean
    public SolrClient solrClient() {
        String baseSolrUrl = solrHost;
        if (solrHost.endsWith("/")) {
            baseSolrUrl = solrHost.substring(0, solrHost.length() - 1);
        }

        // HttpSolrClient を構築する際に、コア名まで含めた完全なURLをBuilderに渡す
        String fullSolrCoreUrl = baseSolrUrl + "/" + COLLECTION_NAME;

        logger.info("SolrClient will be built with full Solr Core URL: {}", fullSolrCoreUrl);

        // Builder を使って構築し直す (protected コンストラクタ回避)
        return new HttpSolrClient.Builder(fullSolrCoreUrl).build();
    }
}
