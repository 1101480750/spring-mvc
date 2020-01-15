package com.spring.demo.controller;

import com.spring.demo.service.DemoService;
import com.spring.demo.springframework.annotation.GPAutowired;
import com.spring.demo.springframework.annotation.GPController;
import com.spring.demo.springframework.annotation.GPRequestMapping;
import com.spring.demo.springframework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@GPController
@GPRequestMapping("/demo")
public class TestController {

    @GPAutowired
    private DemoService demoService;

    @GPRequestMapping("/query")
    public String getName(HttpServletRequest req, HttpServletResponse resp,@GPRequestParam("name") String name) throws Exception{
        String myName = demoService.getName(name);
        resp.getWriter().write(myName);
        return myName;
    }
}
