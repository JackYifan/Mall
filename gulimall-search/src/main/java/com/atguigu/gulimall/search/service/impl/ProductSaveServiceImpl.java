package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.ESSkuModel;
import com.atguigu.gulimall.search.config.MallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Resource
    private RestHighLevelClient client;

    @Override
    public boolean productStatusUp(List<ESSkuModel> esSkuModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for (ESSkuModel esSkuModel : esSkuModels) {
            //构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(esSkuModel.getSkuId().toString());
            String s = JSON.toJSONString(esSkuModel);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest); //加到bulkRequest批量保存
        }
        //批量请求添加商品
        BulkResponse bulkResponse = client.bulk(bulkRequest, MallElasticSearchConfig.COMMON_OPTIONS);
        // TODO 如果批量保存出现错误
        boolean result = bulkResponse.hasFailures();
        List<String> collect = Arrays.stream(bulkResponse.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
        log.info("商品上架完成, {}", collect);
        return result;
    }
}
