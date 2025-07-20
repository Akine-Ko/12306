package com.akine.mytrain.member.controller;

import com.akine.mytrain.common.resp.CommonResp;
import com.akine.mytrain.member.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Resource
    private MemberService memberService;

    @GetMapping("/hello")
    public CommonResp<String> hello(){
        CommonResp<String> commonResp = new CommonResp<>();
        commonResp.setContent("hello world");
        return commonResp;
    }
}
