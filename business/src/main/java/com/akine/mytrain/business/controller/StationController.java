package com.akine.mytrain.business.controller;

import com.akine.mytrain.business.resp.StationQueryResp;
import com.akine.mytrain.business.service.StationService;
import com.akine.mytrain.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/station")
public class StationController {

    @Resource
    private StationService stationService;

    @GetMapping("/query-all")
    public CommonResp<List<StationQueryResp>> queryAll() {
        List<StationQueryResp> list = stationService.queryAll();
        return new CommonResp<>(list);
    }

}
