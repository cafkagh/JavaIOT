package com.spike.mercury.service;

import com.spike.mercury.dao.HardwareDao;
import com.spike.mercury.model.Hardware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HardwareService {
    @Autowired
    private HardwareDao hardwareDao;

    public List<Hardware> SelectAllHardware(Integer type){
        return hardwareDao.selectAllHardware(type);
    }

    public Integer getHardwareCountByCid(String cid){
        return hardwareDao.getHardwareCountByCid(cid);
    }

    public boolean setHardwareCountOnline(String cid, Integer online){
        return hardwareDao.setHardwareCountOnline(cid, online) > 0;
    }

}
