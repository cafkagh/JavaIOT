package com.spike.mercury.model;
import org.beetl.sql.core.annotatoin.AssignID;
import org.beetl.sql.core.annotatoin.Table;

import java.util.Date;

@Table(name = "hardware")
public class Hardware {
    private Integer id;
    private String cid;
    private Integer type;
    private String curl;
    private String rurl;
    private String cpw;
    private String salt;
    private String name;
    private String bz;
    private Integer online;
    private String data_fmt_r;
    private String data_fmt_w;
    private Integer show_order;
    private Integer del;
    private String add_user;
    private Date created_at;
    private Date updated_at;
    private String symbol;

     @AssignID
    public Integer getId() {
        return id;
    }
}
