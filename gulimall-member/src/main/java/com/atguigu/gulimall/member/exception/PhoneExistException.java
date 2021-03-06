package com.atguigu.gulimall.member.exception;

public class PhoneExistException extends RuntimeException{

    public PhoneExistException() {
        super("手机号码已经存在");
    }


}
