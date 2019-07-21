package com.example.demo.service.impl;

import com.example.mvcframework.annotation.GPService;

@GPService
public class IDemonServiceImpl implements IDemonService {
    public String get(String name) {
        return "get the :"+name;
    }
}
