package com.akine.mytrain.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.akine.mytrain.business.domain.ConfirmOrder;
import com.akine.mytrain.business.domain.ConfirmOrderExample;
import com.akine.mytrain.business.domain.DailyTrainTicket;
import com.akine.mytrain.business.enums.ConfirmOrderStatusEnum;
import com.akine.mytrain.business.enums.SeatColEnum;
import com.akine.mytrain.business.enums.SeatTypeEnum;
import com.akine.mytrain.business.mapper.ConfirmOrderMapper;
import com.akine.mytrain.business.req.ConfirmOrderDoReq;
import com.akine.mytrain.business.req.ConfirmOrderQueryReq;
import com.akine.mytrain.business.req.ConfirmOrderTicketReq;
import com.akine.mytrain.business.resp.ConfirmOrderQueryResp;
import com.akine.mytrain.common.context.LoginMemberContext;
import com.akine.mytrain.common.exception.BusinessException;
import com.akine.mytrain.common.exception.BusinessExceptionEnum;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.common.util.SnowUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ConfirmOrderService {

    private static Logger logger = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if(ObjectUtil.isNull(confirmOrder.getId())){
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        }
        else{
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        logger.info("查询页码:{}", req.getPage());
        logger.info("每页条数:{}", req.getSize());

        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);

        logger.info("总行数:{}", pageInfo.getTotal());
        logger.info("总页数:{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> PageResp = new PageResp<>();
        PageResp.setList(list);
        PageResp.setTotal(pageInfo.getTotal());
        return PageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }


    public void doConfirm(ConfirmOrderDoReq req) {
        // 保存确认订单表，状态初始
        DateTime now = DateTime.now();
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> tickets = req.getTickets();

        ConfirmOrder confirmOrder = new ConfirmOrder();

        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(date);
        confirmOrder.setTrainCode(trainCode);
        confirmOrder.setStart(start);
        confirmOrder.setEnd(end);
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));

        confirmOrderMapper.insert(confirmOrder);

        // 查出余票记录，获取真实库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        logger.info("查出余票记录:{}", dailyTrainTicket);

        // 扣减余票数量，判断余票是否足够
        reduceTickets(req, dailyTrainTicket);

        // 计算相对于第一个座位的偏移值
        // 比如选择的是C1, D2， 则偏移值是[0, 5]
        // 比如选择的是A1, B1, C1， 则偏移值是:[0, 1, 2]
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if(StrUtil.isNotBlank(ticketReq0.getSeat())){
            logger.info("本次购票有选座");
            // 查出本次选座的座位类型有哪些列，用于计算所选座位与第一个座位的偏移值
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            logger.info("本次选座的座位类型包含的列：{}", colEnumList);

            // 组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1, C1, D1, F1, A2, C2, D2, F2}
            List<String> referSeatList = new ArrayList<>();
            for(int i = 1; i <= 2; i++){
                for(SeatColEnum seatColEnum : colEnumList){
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            logger.info("用于作参照的两排座位:{}", referSeatList);

            // 相对偏移值
            List<Integer> offsetList = new ArrayList<>();
            // 绝对偏移值
            List<Integer> absoluteOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                int index = referSeatList.indexOf(ticketReq.getSeat());
                absoluteOffsetList.add(index);
            }
            logger.info("计算得到所有座位的绝对偏移值:{}", absoluteOffsetList);

            for(Integer index : absoluteOffsetList){
                int offset = index - absoluteOffsetList.get(0);
                offsetList.add(offset);
            }

            logger.info("计算得到所有座位的相对第一个座位的偏移值:{}", offsetList);
        }
        else{
            logger.info("本次购票没有选座");
        }

        // 选座

            // 一个车厢一个车厢的获取座位数据

            // 挑选符合条件的座位，如果这个车厢不满足，则进入下个车厢(多个选座应该在同一个车厢)

        // 选中座位后事务处理:

            // 座位表修改售卖情况sell
            // 余票详情表修改余票
            // 为会员增加购票记录
            // 更新确认订单为成功
    }

    private static void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for(ConfirmOrderTicketReq ticketReq: req.getTickets()){
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);

            switch (seatTypeEnum){
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz() - 1;
                    if(countLeft < 0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countLeft);
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz() - 1;
                    if(countLeft < 0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countLeft);
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw() - 1;
                    if(countLeft < 0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countLeft);
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw() - 1;
                    if(countLeft < 0){
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countLeft);
                }
            }
        }
    }


}
