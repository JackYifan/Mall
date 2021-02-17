package com.atguigu.gulimall.search.service;


import com.atguigu.common.to.es.ESSkuModel;

import java.io.IOException;
import java.util.List;

/**
 * @AUTHOR: raymond
 * @DATETIME: 2020/5/13  22:43
 * DESCRIPTION:
 **/
public interface ProductSaveService {
    boolean productStatusUp(List<ESSkuModel> esSkuModels) throws IOException;
}
