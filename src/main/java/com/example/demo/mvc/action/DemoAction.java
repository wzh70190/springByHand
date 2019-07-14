package com.example.demo.mvc.action;

import com.example.demo.service.impl.IDemonService;
import com.example.mvcframework.annotation.GPAutowired;
import com.example.mvcframework.annotation.GPController;
import com.example.mvcframework.annotation.GPRequestMapping;
import com.example.mvcframework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@GPController
@GPRequestMapping("/demo")
public class DemoAction {

    @GPAutowired
    private IDemonService demonService;


    @GPRequestMapping("query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @GPRequestParam("name") String name) throws IOException {
        String getName = demonService.get(name);
        resp.getWriter().write(getName);
    }

    @GPRequestMapping("add.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @GPRequestParam("a") Integer a, @GPRequestParam("b") Integer b) throws IOException {
        resp.getWriter().write(a + "+" + b + "=" + (a + b));
    }

    @GPRequestMapping("remove.json")
    public void remove(HttpServletRequest req, HttpServletResponse resp, @GPRequestParam("id") Integer id) throws IOException {
    }
}
