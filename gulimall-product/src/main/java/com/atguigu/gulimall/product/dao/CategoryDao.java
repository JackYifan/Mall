package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author wyf
 * @email 3190103178@zju.edu.cn
 * @date 2021-02-09 10:04:38
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
