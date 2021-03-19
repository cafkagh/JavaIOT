package com.spike.mercury.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class Web {

    @RequestMapping(value = "/admin/index")
    public String index() {
        return "index";
    }
}
