package com.akine.mytrain.member.controller.admin;

import com.akine.mytrain.common.resp.CommonResp;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.member.req.TicketQueryReq;
import com.akine.mytrain.member.resp.TicketQueryResp;
import com.akine.mytrain.member.service.TicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ticket")
public class TicketAdminController {

    @Resource
    private TicketService ticketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>> queryList(@Valid TicketQueryReq req) {
        PageResp<TicketQueryResp> list = ticketService.queryList(req);
        return new CommonResp<>(list);
    }

}
