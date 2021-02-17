package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * @AUTHOR: raymond
 * @DATETIME: 2020/5/16  09:43
 * DESCRIPTION:
 **/
public interface MallSearchService {

    /**
     * 检索服务
     * @param searchParam 所有检索参数
     * @return 检索结果
     */
    SearchResult search(SearchParam searchParam);

}
