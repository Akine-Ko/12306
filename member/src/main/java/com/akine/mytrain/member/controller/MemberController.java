package com.akine.mytrain.member.controller;

import com.akine.mytrain.common.resp.CommonResp;
import com.akine.mytrain.member.req.MemberLoginReq;
import com.akine.mytrain.member.req.MemberRegisterReq;
import com.akine.mytrain.member.req.MemberSendCodeReq;
import com.akine.mytrain.member.resp.MemberLoginResp;
import com.akine.mytrain.member.service.MemberService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public CommonResp<Integer> count() {
        int count = memberService.count();
        CommonResp<Integer> commonResp = new CommonResp<>();
        commonResp.setContent(count);
        return commonResp;
    }

    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq req) {
        long register = memberService.register(req);
        //CommonResp<Long> commonResp = new CommonResp<>();
        //commonResp.setContent(register);
        //return commonResp;

        return new CommonResp<>(register);
    }

    @PostMapping("/send-code")
    public CommonResp<Long> sendCode(@Valid @RequestBody MemberSendCodeReq req) {
        memberService.sendCode(req);
        return new CommonResp<>();
    }

    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid @RequestBody MemberLoginReq req) {
        MemberLoginResp resp = memberService.login(req);
        return new CommonResp<>(resp);
    }
}
