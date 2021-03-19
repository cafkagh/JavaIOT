selectAllHardware
===
    * 获取允许调用的服务列表
    SELECT * from hardware where type = #type#;
    
getHardwareCountByCid
===
    * 获取硬件个数
    SELECT count(*) from hardware where cid = #cid#;
    
setHardwareCountOnline
===
    * 更新硬件在线状态
    update hardware set online = #online# where cid = #cid#;