package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author wyf
 * @email 3190103178@zju.edu.cn
 * @date 2021-02-17 11:04:05
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
