package com.lhc.ms.util;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lhc
 * @create --
 */
@Data
public class R {
    private Boolean success;
    private Integer code;
    private String message;
    private Map<String,Object> data = new HashMap<String, Object>();

    private R(){}

    public static R ok(){
        R r = new R();
        r.setSuccess(true);
        r.setCode(ResultCode.SUCCESS);
        r.setMessage("成功");
        return r;
    }

    public static R error(){
        R r = new R();
        r.setSuccess(false);
        r.setCode(ResultCode.ERROR);
        r.setMessage("失败");
        return r;
    }



    public R setCode(Integer code) {
        this.code = code;
        return this;
    }

    public R setMessage(String message) {
        this.message = message;
        return this;
    }

    public R setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

   /* public R setData(Map<String,Object> data) {
        this.data = data;
        return this;
    }*/
    public R add(Map<String, Object> data){
        this.data = data;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public R add(String key,Object value) {
        this.getData().put(key,value);
        return this;
    }
}
