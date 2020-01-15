package com.spring.demo.service.impl;

import com.spring.demo.service.DemoService;
import com.spring.demo.springframework.annotation.GPService;

@GPService
public class DemoServiceImpl implements DemoService {

    @Override
    public String getName(String name) {
        return "My name is ".concat(name);
    }
}
