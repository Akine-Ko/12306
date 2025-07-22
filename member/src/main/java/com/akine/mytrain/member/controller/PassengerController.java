package com.akine.mytrain.member.controller;

import com.akine.mytrain.common.context.LoginMemberContext;
import com.akine.mytrain.common.resp.CommonResp;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.member.req.PassengerQueryReq;
import com.akine.mytrain.member.req.PassengerSaveReq;
import com.akine.mytrain.member.resp.PassengerQueryResp;
import com.akine.mytrain.member.service.PassengerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

    @Resource
    private PassengerService passengerService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody PassengerSaveReq req) {
        passengerService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<PassengerQueryResp>> quertList(@Valid PassengerQueryReq req) {
        req.setMemberId(LoginMemberContext.getId());
        PageResp<PassengerQueryResp> list = passengerService.queryList(req);
        return new CommonResp<>(list);
    }

}
