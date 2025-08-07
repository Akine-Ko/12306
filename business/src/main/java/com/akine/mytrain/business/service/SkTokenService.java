package com.akine.mytrain.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.common.util.SnowUtil;
import com.akine.mytrain.business.domain.SkToken;
import com.akine.mytrain.business.domain.SkTokenExample;
import com.akine.mytrain.business.mapper.SkTokenMapper;
import com.akine.mytrain.business.req.SkTokenQueryReq;
import com.akine.mytrain.business.req.SkTokenSaveReq;
import com.akine.mytrain.business.resp.SkTokenQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkTokenService {

    private static Logger logger = LoggerFactory.getLogger(SkTokenService.class);

    @Resource
    private SkTokenMapper skTokenMapper;

    public void save(SkTokenSaveReq req) {
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if(ObjectUtil.isNull(skToken.getId())){
            skToken.setId(SnowUtil.getSnowflakeNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        }
        else{
            skToken.setUpdateTime(now);
            skTokenMapper.updateByPrimaryKey(skToken);
        }

    }

    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();

        logger.info("查询页码:{}", req.getPage());
        logger.info("每页条数:{}", req.getSize());

        PageHelper.startPage(req.getPage(), req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        PageInfo<SkToken> pageInfo = new PageInfo<>(skTokenList);

        logger.info("总行数:{}", pageInfo.getTotal());
        logger.info("总页数:{}", pageInfo.getPages());

        List<SkTokenQueryResp> list = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> PageResp = new PageResp<>();
        PageResp.setList(list);
        PageResp.setTotal(pageInfo.getTotal());
        return PageResp;
    }

    public void delete(Long id) {
        skTokenMapper.deleteByPrimaryKey(id);
    }

}
