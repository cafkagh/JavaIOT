package com.spike.mercury.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.spike.mercury.util.RedisUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.spike.mercury.service.HardwareService;

@Component("SocketServer")
@RestController
@Configuration
public class SocketServer {
    @Value("${input.host}")
    private String inputHost;

    @Value("${input.port}")
    private Integer inputPort;

    @Value("${socketServer.host}")
    private String listenHost;
    @Value("${socketServer.port}")
    private Integer listenPort;
    @Value("${socketServer.timeout}")
    private Integer listenTimeout;

    @Value("${heartBeat.timeout}")
    private Integer heartBeatTimeout;


    @Resource
    private RedisUtil redisUtil;

    @Resource
    HardwareService hardwareService;

    // @Autowired
    // JdbcTemplate jdbcTemplate;

    // 操作类型
    private final String[] allowType = {"HS", "STATE", "HB", "DATA", "TIMING"};

    // 锁
    private Lock lock = new ReentrantLock();

    private Lock clientPoolLock = new ReentrantLock();
    private Lock hardwarePoolLock = new ReentrantLock();

    //日志
    private Logger logger = Logger.getLogger(SocketServer.class);

    // 连接池
    private Map<Integer, Socket> clientPool = new HashMap<Integer, Socket>();

    // 设备池
    private BiMap<String, Integer> hardwarePool = HashBiMap.create();

    // 线程池
    private ExecutorService threadPool = Executors.newFixedThreadPool(256);

    public Map getClientPool(){
        return clientPool;
    }

    public BiMap getHardwarePool(){
        return hardwarePool;
    }

    // byte转字符串
    public String b2s(byte[] bytes, int len)
    {
        String r;
        try {
            r = new String(bytes, 0, len, "UTF-8");
        }  catch (Exception e) {
            r = "";
        }
        return r;
    }

    // 获取socketID
    public Integer socketID(Socket socket){
        return System.identityHashCode(socket);
    }

    // 获取Cid
    public String getCid(Integer clientID){
        hardwarePoolLock.lock();
        String Cid = hardwarePool.inverse().get(clientID);
        hardwarePoolLock.unlock();
        return Cid;
    }

    //获取clientID
    public int getClientID(String Cid){
        hardwarePoolLock.lock();
        int clientID = hardwarePool.get(Cid);
        hardwarePoolLock.unlock();
        return clientID;
    }
    //获取Socket
    public Socket getSocket(int ClientID){
        clientPoolLock.lock();
        Socket socket = clientPool.get(ClientID);
        clientPoolLock.unlock();
        return socket;
    }

    //写入clientPool
    public void putClientPool(int socketID,Socket socket){
        clientPoolLock.lock();
        clientPool.put(socketID,socket);
        clientPoolLock.unlock();
    }
    //写入hardwarePool
    public void putHardwarePool(String cid,int socketID){
        hardwarePoolLock.lock();
        hardwarePool.put(cid,socketID);
        hardwarePoolLock.unlock();
    }

    public boolean hasClientPoolKey(int socketID){
        clientPoolLock.lock();
        boolean has = clientPool.containsKey(socketID);
        clientPoolLock.unlock();
        return has;
    }

    public boolean hasHardwarePoolKey(String Cid){
        hardwarePoolLock.lock();
        boolean has = hardwarePool.containsKey(Cid);
        hardwarePoolLock.unlock();
        return has;
    }
    public boolean hasHardwarePoolValue(int socketID){
        hardwarePoolLock.lock();
        boolean has = hardwarePool.containsValue(socketID);
        hardwarePoolLock.unlock();
        return has;
    }

    //删除clientPool
    public void removeClientPool(int socketID){
        clientPoolLock.lock();
        clientPool.remove(socketID);
        clientPoolLock.unlock();
    }
    //删除hardwarePool
    public void removeHardwarePool(String cid){
        hardwarePoolLock.lock();
        hardwarePool.remove(cid);
        hardwarePoolLock.unlock();
    }
    //


    // json字符串转Map
    public Map<String, String> jsonStringToMapping(String jsonString) {
        //将json字符串封装到Map
        Map<String, String> map = (Map<String, String>) JSONObject.parse(jsonString);
        return map;
    }
    public String mappingToJsonString(Map jsonMap) {
        JSONObject jsonObj=new JSONObject(jsonMap);
        return jsonObj.toString();
    }

    // 握手处理
    public boolean handshake(Socket client, String cid){
        System.out.println("handshake:[");
        Integer socketID = socketID(client);
        boolean hsok = false;

        // jdbc方式
        // String sql = "SELECT count(*) FROM hardware WHERE cid = ?";
		// Integer count = Integer.valueOf((String)jdbcTemplate.queryForObject(sql, new Object[] { cid }, String.class));

        Integer count = hardwareService.getHardwareCountByCid(cid);
        if(count > 0){
            if(!hasHardwarePoolValue(socketID)){
                putHardwarePool(cid,socketID);
            }
            // jdbcTemplate.update("update hardware set online = 1 where cid = ?", cid);
            hardwareService.setHardwareCountOnline(cid,1);
            //调试
            // System.out.println(JSON.toJSONString(hardwarePool));
            sendMessage(client,"{\"TYPE\":\"HSOK\",\"DATA\":\"HSOK\"}");
            hsok = true;
        }else{
            sendMessage(client,"{\"TYPE\":\"HSOK\",\"DATA\":\"NONE\"}");
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // logger.info("handshake hardwarePool-size:"+hardwarePool.size());
        System.out.println("]");
        return hsok;
    }

    // 续期
    public void Renewal(Integer clientID){
        try {
            logger.info("续期" + clientID);
            if(hasHardwarePoolValue(clientID)){
                String Cid =getCid(clientID);
                redisUtil.set(Cid, String.valueOf(clientID), listenTimeout);
            }
            redisUtil.set(clientID + "_ip", String.valueOf(getSocket(clientID).getRemoteSocketAddress()), listenTimeout );
            redisUtil.set(String.valueOf(clientID), String.valueOf(System.currentTimeMillis()), listenTimeout);

        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    // 多线程 消息处理
    public boolean messageHandle(Socket client, Map data){
        String type = data.get("TYPE").toString();
        Integer socketID = socketID(client);
        if(!Arrays.asList(allowType).contains(type)){
            return  false;
        }
        if(Objects.equals(type, "HS")){
            //认证
            String cid = data.get("CID").toString();
            if(handshake(client,cid)){
                Renewal(socketID);
            }
        }else{
            //监测是否认证
            if(!hasHardwarePoolValue(socketID)){
                sendMessage(client,"{\"TYPE\":\"AUTHERROR\",\"DATA\":\"No identity authentication\"}" );
                closeClient(client);
                return  false;
            }
            //在线续期
            Renewal(socketID);
            String cid = getCid(socketID);
            switch(type){
                case "STATE" :
                    String state_rdsk = cid + "_state";
                    redisUtil.set(state_rdsk,JSON.toJSONString(data));
                    break;
                case "HB" :
                    break;
                case "DATA" :
                    data.put("TIME",(long)System.currentTimeMillis()/1000);
                    String jsonString = mappingToJsonString(data);
                    redisUtil.rSet(cid + "_data",jsonString);
                    redisUtil.leftTrim(cid + "_data", 0, 600);

                    sendMessage(client,"{\"TYPE\":\"DATAOK\"}" );
                    break;
                case "TIMING" :
                    break;
                default :
                    //不符合要求
                    closeClient(client);//断开
            }
        }
        return true;
    }

    //发送
    public boolean sendMessage(Socket client, String msg){
        try {
            try {
                PrintWriter os=new PrintWriter(client.getOutputStream());
                os.println(msg);
                os.flush();
                return true;
            }catch (SocketException e){
                logger.error("消息发送异常");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessage(String cid, String msg){
        if(hasHardwarePoolKey(cid)){
            Integer clientID = getClientID(cid);
            if(hasClientPoolKey(clientID)){
                Socket client = getSocket(clientID);
                return sendMessage(client, msg);
            }
        }
        return false;
    }

    public boolean closeClient(Integer clientID){
        if(hasClientPoolKey(clientID)){
            Socket client = getSocket(clientID);
            return closeClient(client);
        }
        return  false;
    }

    public boolean closeClient(Socket client){
        lock.lock();
        System.out.println("closeClient:[");
        Integer socketID = socketID(client);

        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (hasClientPoolKey(socketID)){
            removeClientPool(socketID);
        }
        logger.info("closeClient clientPool-size:"+clientPool.size());

        if(hasHardwarePoolValue(socketID)){
            String cid = getCid(socketID);
            removeHardwarePool(cid);
            // jdbcTemplate.update("update hardware set online = 0 where cid = ?", cid);
            hardwareService.setHardwareCountOnline(cid,0);
        }

        // logger.info("closeClient hardwarePool-size:"+hardwarePool.size());
        logger.error("连接接已断开:" + socketID);
        System.out.println("]");
        lock.unlock();
        return true;
    }

    public String html(){
        return "HTTP/1.1 301 Moved Permanently\r\n"
            +"Content-Length: 162\r\n"
            +"Connection: keep-alive\r\n"
            +"Content-Type: text/html\r\n"
            +"Keep-Alive: timeout=4\r\n"
            +"Location: https://www.spike.org.cn/\r\n"
            +"Proxy-Connection: keep-alive\r\n"
            +"Server: nginx\r\n"
            +"Strict-Transport-Security: max-age=31536000\r\n\r\n"
            +"<html>"
            +"<head><title>301 Moved Permanently</title></head>"
            +"<body>"
            +"<center><h1>301 Moved Permanently</h1></center>"
            +"<hr><center>nginx</center>"
            +"</body>"
            +"</html>";
    }

    public void accetpClient() {
        Runnable accetpClient=()->{
            try {
                ServerSocket server = new ServerSocket(listenPort,10, InetAddress.getByName(listenHost));
                logger.warn("socket服务器已启动" + server);
                while (true) {
                    Socket socket = null;
                    try {
                        socket = server.accept();
                        int socketID = socketID(socket);
                        logger.debug("建立连接ID:" + socketID + "->" + socket.toString());
                        putClientPool(socketID,socket);
                        redisUtil.set(socketID + "_ip", String.valueOf(socket.getRemoteSocketAddress()), listenTimeout );
                        Socket finalSocket = socket;
                        Runnable runnable=()->{
                            try {
                                InputStream inputStream = finalSocket.getInputStream();
                                byte[] bytes = new byte[1024];
                                int len;
                                while ((len = inputStream.read(bytes)) != -1) {
                                    String recvString = b2s(bytes,len);
                                    if(recvString.startsWith("{") && recvString.endsWith("}")){
                                        logger.debug("接收到消息 [" + socketID + "] " + recvString);
                                        try{
                                            Map data = jsonStringToMapping(recvString);
                                            try{
                                                messageHandle(finalSocket,data);
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }catch (Exception e){
                                            logger.error("数据格式错误 :[" + recvString + "]");
                                        }
                                    }else if(recvString.startsWith("GET") || recvString.startsWith("POST")){
                                        logger.debug("接收到http请求：" + socketID);
                                        sendMessage(finalSocket,html());
                                        closeClient(finalSocket);
                                    }else{
                                        logger.debug("接收到其他请求： [" + socketID + "] " + recvString);
                                    }
                                }
                                logger.debug("结束连接ID:" + socketID);
                                inputStream.close();
                            } catch (Exception e) {
                            }
                            closeClient(finalSocket);//断开
                            logger.error("线程结束:" + socketID);
                        };
                        threadPool.submit(runnable);
                    } catch (IOException e) {
                        logger.fatal("监听错误");
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                logger.fatal(e);
                logger.fatal("socket服务器启动失败");
            }
        };
        threadPool.submit(accetpClient);
    }

    // 在线监测
    public void heartBeat(){
        Runnable renewalClient=()->{
            logger.warn("连接检查线程已启动");
            while(true){
                try {
                    Thread.sleep(heartBeatTimeout);
                    clientPoolLock.lock();
                    logger.info("heartBeat clientPool-size:" + clientPool.size());
                    clientPoolLock.unlock();

                    redisUtil.set("heartBeatAlive", String.valueOf(System.currentTimeMillis()), 10);

                    Map<Integer, Socket> clientPoolCopy = new HashMap<Integer, Socket>();
                    clientPoolCopy.putAll(clientPool);
                    for (Map.Entry<Integer, Socket> entry : clientPoolCopy.entrySet()) {
                        if(!redisUtil.hasKey(String.valueOf(entry.getKey()))){
                            System.out.println("heartBeat:[");
                            logger.fatal("活动超时强制下线：" + entry.getKey());
                            closeClient(entry.getValue());
                            System.out.println("]");
                        }
                    }
                    clientPoolCopy = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadPool.submit(renewalClient);
    }

    public void input(){
        Runnable input=()->{
            try {
                logger.warn("输入程序已启动");
                ServerSocket inputServer = new ServerSocket(12345);
                while(true){
                    Socket inputSocket = inputServer.accept();
                    logger.debug("建立输入连接");
                    InputStream inputStream = inputSocket.getInputStream();
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = inputStream.read(bytes)) != -1) {
                        String recvString = b2s(bytes, len);
                        String[] data = recvString.split("->");
                        if(data.length==2){
                            logger.info(recvString);
                            boolean sendRes = sendMessage(data[0],data[1]);
                            PrintWriter os = new PrintWriter(inputSocket.getOutputStream());
                            os.println(sendRes);
                            os.flush();
                        }
                    }
                    inputSocket.close();
                    logger.debug("输入连接已终止");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.fatal("die");
        };
        threadPool.submit(input);
    }

    @Bean
    public void serverstart(){
        accetpClient();
        heartBeat();
        // logger.fatal(hardwareService.setHardwareCountOnline("1",1));
    }

    public void SocketServer(){
        logger.fatal("SocketServer 构造函数");
    }

    @RequestMapping(value = "/{cid}/{action}")
    public boolean hello(@PathVariable String cid, @PathVariable String action) {
        boolean sendRes = false;
        JSONObject jsonObject = JSONObject.parseObject(action);
        jsonObject.put("TYPE","CDATA");
        action = JSONObject.toJSONString(jsonObject);
        sendRes = sendMessage(cid,action+"\n");
        logger.debug("发送数据:[ " + cid + " ] --> " + action);
        return sendRes;
    }
}
