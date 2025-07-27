package com.akine.mytrain.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.common.util.SnowUtil;
import com.akine.mytrain.business.domain.TrainSeat;
import com.akine.mytrain.business.domain.TrainSeatExample;
import com.akine.mytrain.business.mapper.TrainSeatMapper;
import com.akine.mytrain.business.req.TrainSeatQueryReq;
import com.akine.mytrain.business.req.TrainSeatSaveReq;
import com.akine.mytrain.business.resp.TrainSeatQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainSeatService {

    private static Logger logger = LoggerFactory.getLogger(TrainSeatService.class);

    @Resource
    private TrainSeatMapper trainSeatMapper;

    public void save(TrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        TrainSeat trainSeat = BeanUtil.copyProperties(req, TrainSeat.class);
        if(ObjectUtil.isNull(trainSeat.getId())){
            trainSeat.setId(SnowUtil.getSnowflakeNextId());
            trainSeat.setCreateTime(now);
            trainSeat.setUpdateTime(now);
            trainSeatMapper.insert(trainSeat);
        }
        else{
            trainSeat.setUpdateTime(now);
            trainSeatMapper.updateByPrimaryKey(trainSeat);
        }

    }

    public PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req) {
        TrainSeatExample trainSeatExample = new TrainSeatExample();
        trainSeatExample.setOrderByClause("train_code asc, carriage_index asc, carriage_seat_index asc");
        TrainSeatExample.Criteria criteria = trainSeatExample.createCriteria();

        logger.info("查询页码:{}", req.getPage());
        logger.info("每页条数:{}", req.getSize());

        if(ObjectUtil.isNotEmpty(req.getTrainCode())){
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainSeat> trainSeatList = trainSeatMapper.selectByExample(trainSeatExample);

        PageInfo<TrainSeat> pageInfo = new PageInfo<>(trainSeatList);

        logger.info("总行数:{}", pageInfo.getTotal());
        logger.info("总页数:{}", pageInfo.getPages());

        List<TrainSeatQueryResp> list = BeanUtil.copyToList(trainSeatList, TrainSeatQueryResp.class);

        PageResp<TrainSeatQueryResp> PageResp = new PageResp<>();
        PageResp.setList(list);
        PageResp.setTotal(pageInfo.getTotal());
        return PageResp;
    }

    public void delete(Long id) {
        trainSeatMapper.deleteByPrimaryKey(id);
    }

}
