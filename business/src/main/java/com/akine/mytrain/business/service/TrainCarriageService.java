package com.akine.mytrain.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.akine.mytrain.business.domain.TrainCarriage;
import com.akine.mytrain.business.domain.TrainCarriageExample;
import com.akine.mytrain.business.enums.SeatColEnum;
import com.akine.mytrain.business.mapper.TrainCarriageMapper;
import com.akine.mytrain.business.req.TrainCarriageQueryReq;
import com.akine.mytrain.business.req.TrainCarriageSaveReq;
import com.akine.mytrain.business.resp.TrainCarriageQueryResp;
import com.akine.mytrain.common.exception.BusinessException;
import com.akine.mytrain.common.exception.BusinessExceptionEnum;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainCarriageService {

    private static Logger logger = LoggerFactory.getLogger(TrainCarriageService.class);

    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    public void save(TrainCarriageSaveReq req) {
        DateTime now = DateTime.now();

        // 自动计算出列数和总作为数
        List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(req.getSeatType());
        req.setColCount(seatColEnums.size());
        req.setSeatCount(req.getColCount() * req.getRowCount());

        TrainCarriage trainCarriage = BeanUtil.copyProperties(req, TrainCarriage.class);
        if(ObjectUtil.isNull(trainCarriage.getId())){
            //保存之前，先校验唯一键是否存在
            TrainCarriage trainCarriageDB = selectByUnique(req.getTrainCode(), req.getIndex());
            if(ObjectUtil.isNotEmpty(trainCarriageDB)){
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR);
            }

            trainCarriage.setId(SnowUtil.getSnowflakeNextId());
            trainCarriage.setCreateTime(now);
            trainCarriage.setUpdateTime(now);
            trainCarriageMapper.insert(trainCarriage);
        }
        else{
            trainCarriage.setUpdateTime(now);
            trainCarriageMapper.updateByPrimaryKey(trainCarriage);
        }

    }

    private TrainCarriage selectByUnique(String trainCode, Integer index) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andIndexEqualTo(index);
        List<TrainCarriage> list = trainCarriageMapper.selectByExample(trainCarriageExample);

        if(CollUtil.isNotEmpty(list)){
            return list.get(0);
        }
        else{
            return null;
        }
    }

    public PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.setOrderByClause("train_code asc, `index` asc");
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();

        logger.info("查询页码:{}", req.getPage());
        logger.info("每页条数:{}", req.getSize());

        if(ObjectUtil.isNotEmpty(req.getTrainCode())){
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainCarriage> trainCarriageList = trainCarriageMapper.selectByExample(trainCarriageExample);

        PageInfo<TrainCarriage> pageInfo = new PageInfo<>(trainCarriageList);

        logger.info("总行数:{}", pageInfo.getTotal());
        logger.info("总页数:{}", pageInfo.getPages());

        List<TrainCarriageQueryResp> list = BeanUtil.copyToList(trainCarriageList, TrainCarriageQueryResp.class);

        PageResp<TrainCarriageQueryResp> PageResp = new PageResp<>();
        PageResp.setList(list);
        PageResp.setTotal(pageInfo.getTotal());
        return PageResp;
    }

    public void delete(Long id) {
        trainCarriageMapper.deleteByPrimaryKey(id);
    }


    public List<TrainCarriage> selectByTrainCode(String trainCode) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.setOrderByClause("`index` asc");
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();
        criteria.andTrainCodeEqualTo(trainCode);
        return trainCarriageMapper.selectByExample(trainCarriageExample);
    }



}
