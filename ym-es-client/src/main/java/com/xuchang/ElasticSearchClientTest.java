package com.xuchang;

import com.xuchang.common.NewsContants;
import com.xuchang.common.News;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.elasticsearch.client.RequestOptions.DEFAULT;


public class ElasticSearchClientTest {
    private static final String indexName = "news";

    private RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost("localhost", 9201, "http"),
                    new HttpHost("localhost", 9202, "http"),
                    new HttpHost("localhost", 9203, "http")));

    /**
     * 添加索引
     */
    @Test
    public void createIndex() {
        CreateIndexRequest request = new CreateIndexRequest(indexName);//创建索引
        request.settings(Settings.builder()
                .put("index.number_of_shards", 5)
                .put("index.number_of_replicas", 2)
        );

        request.mapping("{\"properties\":{\"newsId\":{\"type\":\"text\",\"index\":false},\"newsTitle\":{\"type\":\"text\",\"index\":true,\"analyzer\":\"ik_smart\"},\"newsContont\":{\"type\":\"text\",\"index\":true,\"analyzer\":\"ik_smart\"},\"readCount\":{\"type\":\"integer\",\"index\":true}}}",//类型映射，需要的是一个JSON字符串
                XContentType.JSON);

        //为索引设置一个别名
        request.alias(
                new Alias("news_alias")
        );
        //可选参数
        request.setTimeout(TimeValue.timeValueMinutes(2));//超时,等待所有节点被确认(使用TimeValue方式)
        request.setMasterTimeout(TimeValue.timeValueMinutes(1));//连接master节点的超时时间(使用TimeValue方式)
        request.waitForActiveShards(ActiveShardCount.from(2));//在创建索引API返回响应之前等待的活动分片副本的数量，以int形式表示。

        try {
            CreateIndexResponse createIndexResponse = client.indices().create(request, DEFAULT);

            if (createIndexResponse.isAcknowledged()) {
                System.out.println("所有节点都已确认请求" + createIndexResponse.index());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除索引
     */
    @Test
    public void deleteIndex() {
        DeleteIndexRequest delectRequest = new DeleteIndexRequest(indexName);
        try {
            AcknowledgedResponse response = client.indices().delete(delectRequest, DEFAULT);
            if (response.isAcknowledged()) {
                System.out.println("所有节点都已确认请求");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 单条插入文档
     */
    @Test
    public void createDocument() {
        IndexRequest request = new IndexRequest(indexName);

        News news = new News();
        news.setId("00001");
        news.setNewId(UUID.randomUUID().toString());
        news.setNewsTitle("fox最近不一样了");
        news.setNewsContont("fox被爱情滋润后越来越年轻");
        news.setReadCount(8888);

        request.id(UUID.randomUUID().toString()).source(JSON.toJSONString(news), XContentType.JSON);
        try {
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            System.out.println("文档新增成功" + response.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 批量插入文档
     */
    @Test
    public void batchCreateBatchDocument() {
        BulkRequest bulkRequest = new BulkRequest();
        IndexRequest request = null;
        for (int i = 0; i < NewsContants.count - 1; i++) {
            request = new IndexRequest(indexName);
            News news = new News();
            news.setId("000" + i);
            news.setNewId(UUID.randomUUID().toString());
            news.setNewsTitle(NewsContants.newsTitle[i]);
            news.setNewsContont(NewsContants.newsContonts[i]);
            news.setReadCount(NewsContants.randomReadCount());
            request.id(news.getId()).source(JSON.toJSONString(news), XContentType.JSON);
            bulkRequest.add(request);
        }

        try {
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            System.out.println("新增成功" + response.status());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据关键字查询
     */
    @Test
    public void queryByTerm() {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("newsTitle", "fox"));
//        sourceBuilder.from(3);//分页查询
//        sourceBuilder.size(3);//分页查询
        SearchRequest rq = new SearchRequest(indexName);
        rq.source(sourceBuilder);
        try {
            SearchResponse response = client.search(rq, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文档
     */
    public void deleteDocument() {

    }

    /**
     * 高亮查询
     */
    @Test
    public void highlightShow() {

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font style='color:red'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.field("newsTitle");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("newsTitle", "fox"));
        sourceBuilder.from(3);//分页查询
        sourceBuilder.size(3);//分页查询
        sourceBuilder.highlighter(highlightBuilder);

        SearchRequest rq = new SearchRequest(indexName);
        rq.source(sourceBuilder);

        try {
            SearchResponse response = client.search(rq, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
                System.out.println(hit.getHighlightFields());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 多词条匹配
     */
    @Test
    public void manyWordQuery() {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilders.boolQuery().must();

//        sourceBuilder.query(QueryBuilders.matchQuery("newsTitle", "fox  霍华德"));
//        sourceBuilder.query(QueryBuilders.queryStringQuery("fox  霍华德"));
//        sourceBuilder.from(3);//分页查询
//        sourceBuilder.size(3);//分页查询

        SearchRequest rq = new SearchRequest(indexName);
        rq.source(sourceBuilder);

        try {
            SearchResponse response = client.search(rq, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 布尔查询
     */
    @Test
    public void boolQuery() {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder =QueryBuilders.boolQuery();
        boolQueryBuilder.should().add(QueryBuilders.matchQuery("newsTitle","fox"));
        boolQueryBuilder.should().add(QueryBuilders.matchQuery("newsContont","林书豪"));

        sourceBuilder.query(boolQueryBuilder);

        SearchRequest rq = new SearchRequest(indexName);
        rq.source(sourceBuilder);
        try {
            SearchResponse response = client.search(rq, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 布尔查询-范围查询
     */
    @Test
    public void boolRangeQuery() {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder =QueryBuilders.boolQuery();
        boolQueryBuilder.must().add(QueryBuilders.rangeQuery("readCount").gte(1).lte(40000));//范围查询

        sourceBuilder.query(boolQueryBuilder);

        SearchRequest rq = new SearchRequest(indexName);
        rq.source(sourceBuilder);
        try {
            SearchResponse response = client.search(rq, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
