package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegisterVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao memberLevelDao;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegisterVo vo) {
        //将vo中的数据封装好并插入数据库
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        //查询当前的用户名和密码是否已经存在
        //如果已经存在抛出异常
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUsername());

        //查询默认等级,并将当前用户设置为默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //将密码加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String password = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(password);


        entity.setUsername(vo.getUsername());
        entity.setMobile(vo.getPhone());
        baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException{
        MemberDao baseMapper = this.baseMapper;
        Integer phoneCount = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(phoneCount>0){
            //已经存在该手机号
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException{
        MemberDao baseMapper = this.baseMapper;
        Integer usernameCount = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(usernameCount>0){
            //已经存在该手机号
            throw new UsernameExistException();
        }


    }

    /**
     * 注册用户
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        //查出member
        String account = vo.getAccount();
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", account).or().eq("mobile", account));
        if(entity==null) return null;
        String passwordDB = entity.getPassword();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(vo.getPassword(), passwordDB);
        if(matches){
            return entity;
        }else{
            return null;
        }
    }


    /**
     * 注册或者更新使用微博登录的用户
     * @param socialUser 注册用户信息
     * @return 数据库中的信息
     */
    @Override
    public MemberEntity login(SocialUser socialUser) {
        //判断当前用户是否已经登陆过
        String uid = socialUser.getUid();
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(entity!=null){
            //用户已经注册过则更新数据
            MemberEntity updateMember = new MemberEntity();
            updateMember.setId(entity.getId());
            updateMember.setAccessToken(socialUser.getAccess_token());
            updateMember.setExpiresIn(socialUser.getExpires_in());
            baseMapper.updateById(updateMember);
            //将更新完后的数据返回
            entity.setAccessToken(socialUser.getAccess_token());
            entity.setExpiresIn(socialUser.getExpires_in());
            return entity;
        }else{
            //注册新的账号

            MemberEntity register = new MemberEntity();
            try {
                //使用微博的api获取用户信息
                Map<String,String> query = new HashMap<>();
                query.put("access_token",socialUser.getAccess_token());
                query.put("uid",socialUser.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com",
                        "/2/users/show.json",
                        "get",
                        new HashMap<>(),
                        query);
                if(response.getStatusLine().getStatusCode()==200){
                    //将返回体转换为json
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");
                    register.setNickname(name);
                    register.setGender("m".equals(gender)?1:0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //即使捕获到异常也需要执行一下操作，防止因为网络问题没有注册用户
            register.setSocialUid(socialUser.getUid());
            register.setAccessToken(socialUser.getAccess_token());
            register.setExpiresIn(socialUser.getExpires_in());
            baseMapper.insert(register);
            return register;
        }

    }

}
