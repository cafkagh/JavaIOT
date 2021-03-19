package com.spike.mercury.controller;

import com.spike.mercury.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.spike.mercury.util.R;
import com.spike.mercury.controller.SocketServer;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class Rest {

    @Autowired
    private  SocketServer SocketServer;

    @Resource
    private RedisUtil redisUtil;

    @RequestMapping(value = "/admin/getClients")
    public R getClients() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, String> ips = new HashMap<String, String>();
        Set ipKeys =redisUtil.keys("*_ip");
        for (Object key : ipKeys) {
            ips.put(key.toString().replace("_ip",""),redisUtil.get(key.toString()).toString());
        }
        data.put("clients",SocketServer.getClientPool().keySet());
        data.put("hardwares",SocketServer.getHardwarePool().inverse());
        data.put("ips",ips);
        return R.ok(data);
    }

    @RequestMapping(value = "/admin/closeclient/{clientID}")
    public Boolean closeclient(@PathVariable Integer clientID) {
        return SocketServer.closeClient(clientID);
    }

    @RequestMapping(value = "admin/clean")
    public Boolean clean() {
        return true;
    }
}
