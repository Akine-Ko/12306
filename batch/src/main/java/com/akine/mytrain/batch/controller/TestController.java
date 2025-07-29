package com.akine.mytrain.batch.controller;

import com.akine.mytrain.batch.feign.BusinessFeign;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Resource
    BusinessFeign businessFeign;

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/hello")
    public String hello(){
        String hello = businessFeign.hello();
        logger.info(hello);
        return "hello world, batch!";
    }
}
