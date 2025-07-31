package com.akine.mytrain.business.controller;

import com.akine.mytrain.business.req.DailyTrainTicketQueryReq;
import com.akine.mytrain.business.resp.DailyTrainTicketQueryResp;
import com.akine.mytrain.business.service.DailyTrainTicketService;
import com.akine.mytrain.common.resp.CommonResp;
import com.akine.mytrain.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/daily-train-ticket")
public class DailyTrainTicketController {

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;


    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryList(req);
        return new CommonResp<>(list);
    }


}
