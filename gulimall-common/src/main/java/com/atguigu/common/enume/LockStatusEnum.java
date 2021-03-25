package com.atguigu.common.enume;

/**
 * @Author Yifan Wu
 * Date on 2021/3/23  12:57
 */
public enum LockStatusEnum {
    LOCKED(1),
    UNLOCK(2),
    submit(3);

    private Integer status;
    LockStatusEnum(Integer status){
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
