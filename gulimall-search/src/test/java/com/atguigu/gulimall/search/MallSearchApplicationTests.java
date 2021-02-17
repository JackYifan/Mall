package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.MallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MallSearchApplicationTests {


    @Autowired
    private RestHighLevelClient client;

    @Test
    public void insertIndex() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setName("Jane");
        user.setAge(19);
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString,XContentType.JSON);//要保存的内容用source API
        IndexResponse index = client.index(indexRequest, MallElasticSearchConfig.COMMON_OPTIONS);//发动请求,第二个参数是请求中的选项
        System.out.println(index);
    }

    @Test
    public void searchData() throws IOException {
        //创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");
        searchRequest.types("account");
        //指定检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));//使用工具类创建QueryBuilder
        //创建聚合请求
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("ageAgg").field("age").size(10);
        aggregation.subAggregation(AggregationBuilders.avg("accountAvg").field("account_number"));
        searchSourceBuilder.aggregation(aggregation);
        //发出请求
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = client.search(searchRequest, MallElasticSearchConfig.COMMON_OPTIONS);


        //处理结果
        SearchHits hits = response.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            String s = hit.getSourceAsString();
            System.out.println(s);
        }

        Aggregations aggregations = response.getAggregations();
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄"+keyAsString+"->"+bucket.getDocCount()+"个人");
        }

    }

    @Data
    class User{
        private String name;
        private Integer age;
    }

}
