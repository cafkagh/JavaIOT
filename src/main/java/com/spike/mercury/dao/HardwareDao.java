package com.spike.mercury.dao;

import com.spike.mercury.model.Hardware;
import org.beetl.sql.core.annotatoin.Param;
// import org.beetl.sql.core.annotatoin.SqlResource;
import org.beetl.sql.core.mapper.BaseMapper;

import java.util.List;

// @SqlResource("hardware")
public interface HardwareDao extends BaseMapper<Hardware> {
    List<Hardware> selectAllHardware(@Param(value = "type") Integer type);

    Integer getHardwareCountByCid(@Param(value = "cid") String cid);

    Integer setHardwareCountOnline(@Param(value = "cid") String cid, @Param(value = "online") Integer online);

}
