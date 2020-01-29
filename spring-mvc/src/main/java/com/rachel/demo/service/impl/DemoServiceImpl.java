package com.rachel.demo.service.impl;

import com.rachel.demo.service.DemoService;
import com.rachel.spring.annocation.MyService;

@MyService
public class DemoServiceImpl implements DemoService {

    @Override
    public String getName(String name) {
        System.out.println("get name:" + name);
        return name;
    }
}
