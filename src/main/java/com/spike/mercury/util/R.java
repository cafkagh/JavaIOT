package com.spike.mercury.util;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class R extends HashMap<String, Object> {

    private R() {
        put("code", "0000");
        put("msg", "success");
        put("ts", System.currentTimeMillis());

    }

    public static R error() {
        return error(500, "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error(500, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String data) {
        R r = new R();
        r.put("data", data);
        return r;
    }

    public static R ok(String msg, String data) {
        R r = new R();
        r.put("msg", msg);
        r.put("data", data);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.put("data",map);
        return r;
    }

    public static R ok() {
        return new R();
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public String getString() {
        return JSONObject.toJSONString(this);
    }
}

