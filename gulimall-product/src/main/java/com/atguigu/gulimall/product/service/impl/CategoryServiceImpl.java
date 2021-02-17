package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listTree() {
        //查询出所有项
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //查出所有项的根结点
        List<CategoryEntity> roots = entities.stream().filter((entity) -> {
                    return entity.getParentCid() == 0;
                }).map((root) -> {
                    root.setChildren(getChildren(root, entities));
                    return root;
                }).sorted((e1, e2) -> {
                    return (e1.getSort()==null?0:e1.getSort())-(e2.getSort()==null?0:e2.getSort());
                }).collect(Collectors.toList());
        return roots;
    }

    /**
     * 逻辑删除
     * @param idList 要删除的id列表
     */
    @Override
    public void removeMenuByIds(List<Long> idList) {
        //TODO 判断是否有关联的数据
        baseMapper.deleteBatchIds(idList);
    }

    /**
     * 根据当前结点查询路径
     * 优化成非递归
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new LinkedList<>();
        path.add(0,catelogId);
        CategoryEntity cur = baseMapper.selectById(catelogId);
        while(cur.getParentCid()!=0){
            cur = baseMapper.selectById(cur.getParentCid());
            path.add(0,cur.getCatId());
        }
        return path.toArray(new Long[path.size()]);
//        List<Long> path = new ArrayList<>();
//        List<Long> parentPath = findParentPath(attrGroupId,path);
//        Collections.reverse(parentPath);
//        return parentPath.toArray(new Long[parentPath.size()]);
    }

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    /**
     * 获取所有一级分类
     * @return
     */
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    /**
     * 私有的递归方法
     * 尾递归可以修改为非递归
     * 该函数将自身和所有父节点加入路径
     * @param catelogId 当前的id
     * @param paths 路径用引用传参
     * @return
     */
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //将当前id先加入路径
        paths.add(catelogId);
        CategoryEntity cur = this.getById(catelogId);
        if(cur.getParentCid()!=0){
            findParentPath(cur.getParentCid(),paths);
        }
        return paths;
    }





    /**
     * 查询子结点并返回列表
     * 不会永久递归的原因是当流中没有元素时，不会执行map中的操作
     * all传递的是引用，所以效率不会过低
     * @param root 需要封装的项
     * @param all  所有项
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter((e) -> {
            return e.getParentCid() == root.getCatId();
        }).map((e) -> {
            e.setChildren(getChildren(e, all));
            return e;
        }).sorted((e1, e2) -> {
            return (e1.getSort()==null?0:e1.getSort())-(e2.getSort()==null?0:e2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}
