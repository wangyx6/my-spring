package com.rachel.demo.controller;

import com.rachel.demo.service.DemoService;
import com.rachel.spring.annocation.MyAutowired;
import com.rachel.spring.annocation.MyController;
import com.rachel.spring.annocation.MyRequestMapping;
import com.rachel.spring.annocation.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@MyController
@MyRequestMapping("/demo")
@Security(value = {"admin"})
public class DemoController {

    @MyAutowired
    private DemoService demoService;

    @MyRequestMapping("/zhangsan")
    @Security(value = {"zhangsan"})
    public String authZhangsan(HttpServletRequest request, HttpServletResponse response, String username){
        String retName = demoService.getName(username);
        return "welcome <" + retName + "> login--time" + new Date();
    }

    @MyRequestMapping("/lisi")
    @Security(value = {"lisi"})
    public String authLisi(HttpServletRequest request, HttpServletResponse response, String username){
        String retName = demoService.getName(username);
        return "welcome <" + retName + "> login--time" + new Date();
    }

    @MyRequestMapping("/admin")
    public String admin(HttpServletRequest request, HttpServletResponse response, String username){
        String retName = demoService.getName(username);
        return "welcome <" + retName + "> login--time" + new Date();
    }
}
