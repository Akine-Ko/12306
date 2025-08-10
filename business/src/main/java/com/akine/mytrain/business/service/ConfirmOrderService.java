package com.akine.mytrain.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.akine.mytrain.business.domain.*;
import com.akine.mytrain.business.dto.ConfirmOrderMQDto;
import com.akine.mytrain.business.enums.ConfirmOrderStatusEnum;
import com.akine.mytrain.business.enums.RedisKeyPreEnum;
import com.akine.mytrain.business.enums.SeatColEnum;
import com.akine.mytrain.business.enums.SeatTypeEnum;
import com.akine.mytrain.business.mapper.ConfirmOrderMapper;
import com.akine.mytrain.business.req.ConfirmOrderDoReq;
import com.akine.mytrain.business.req.ConfirmOrderQueryReq;
import com.akine.mytrain.business.req.ConfirmOrderTicketReq;
import com.akine.mytrain.business.resp.ConfirmOrderQueryResp;
import com.akine.mytrain.common.exception.BusinessException;
import com.akine.mytrain.common.exception.BusinessExceptionEnum;
import com.akine.mytrain.common.resp.PageResp;
import com.akine.mytrain.common.util.SnowUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private SkTokenService skTokenService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        }
        else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");

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


    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlock")
    @Async
    public void doConfirm(ConfirmOrderMQDto dto) throws InterruptedException {
        MDC.put("LOG_ID", dto.getLogId());
        logger.info("异步出票开始:{}", dto);
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-" + DateUtil.formatDate(dto.getDate()) + "-" + dto.getTrainCode();
        RLock lock = redissonClient.getLock(lockKey);
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(0, 10,TimeUnit.SECONDS);

            if (tryLock) {
                logger.info("恭喜抢到锁了");
            }
            else {
                logger.info("没抢到锁，有其它消费线程正在出票，不做任何处理");
                return;
            }

            while (true){
                // 从数据库里查出订单
                ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
                confirmOrderExample.setOrderByClause("id asc");
                ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();
                criteria.andDateEqualTo(dto.getDate())
                        .andTrainCodeEqualTo(dto.getTrainCode())
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
                PageHelper.startPage(1, 5);
                List<ConfirmOrder> list = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);
                if (CollUtil.isEmpty(list)) {
                    logger.info("没有需要处理的订单，结束循环");
                    break;
                }
                else {
                    logger.info("本次处理{}条确认订单", list.size());
                }

                list.forEach(confirmOrder -> {
                    try {
                        sell(confirmOrder);
                    }
                    catch (BusinessException e) {
                        if(e.getE().equals(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR)){
                            logger.info("本订单余票不足，继续售卖下一个订单");
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateStatus(confirmOrder);
                        }
                        else{
                            throw e;
                        }
                    }
                });
            }

        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (tryLock && lock.isHeldByCurrentThread()) {
                logger.info("购票流程结束，释放锁！lockKey：{}", lockKey);
                lock.unlock();
            }
        }
    }

    /**
     * 售票
     * @param confirmOrder
     */
    private void sell(ConfirmOrder confirmOrder) {
        // 构造ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(confirmOrder.getDate());
        req.setTrainCode(confirmOrder.getTrainCode());
        req.setStart(confirmOrder.getStart());
        req.setEnd(confirmOrder.getEnd());
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));
        req.setImageCode("");
        req.setImageCodeToken("");
        req.setLogId("");

        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        // 将订单设置成处理中，避免重复处理
        logger.info("将确认订单更新成处理中，避免重复处理，confirm_order.id: {}", confirmOrder.getId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateStatus(confirmOrder);

        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> tickets = req.getTickets();

        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        logger.info("查出余票记录：{}", dailyTrainTicket);

        // 预扣减余票数量，并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);

        // 最终的选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        // 计算相对第一个座位的偏移值
        // 比如选择的是C1,D2，则偏移值是：[0,5]
        // 比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if (StrUtil.isNotBlank(ticketReq0.getSeat())) {
            logger.info("本次购票有选座");
            // 查出本次选座的座位类型都有哪些列，用于计算所选座位与第一个座位的偏离值
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            logger.info("本次选座的座位类型包含的列：{}", colEnumList);

            // 组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1, C1, D1, F1, A2, C2, D2, F2}
            List<String> referSeatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            logger.info("用于作参照的两排座位：{}", referSeatList);

            List<Integer> offsetList = new ArrayList<>();
            // 绝对偏移值，即：在参照座位列表中的位置
            List<Integer> aboluteOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                int index = referSeatList.indexOf(ticketReq.getSeat());
                aboluteOffsetList.add(index);
            }
            logger.info("计算得到所有座位的绝对偏移值：{}", aboluteOffsetList);
            for (Integer index : aboluteOffsetList) {
                int offset = index - aboluteOffsetList.get(0);
                offsetList.add(offset);
            }
            logger.info("计算得到所有座位的相对第一个座位的偏移值：{}", offsetList);

            getSeat(date,
                    trainCode,
                    ticketReq0.getSeatTypeCode(),
                    ticketReq0.getSeat().split("")[0],
                    offsetList, // 从A1得到A
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex(),
                    finalSeatList);

        }
        else {
            logger.info("本次购票没有选座");
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                getSeat(date,
                        trainCode,
                        ticketReq.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex(),
                        finalSeatList);
            }
        }

        logger.info("最终选座：{}", finalSeatList);

        // 选中座位后事务处理：
        // 座位表修改售卖情况sell；
        // 余票详情表修改余票；
        // 为会员增加购票记录
        // 更新确认订单为成功
        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, tickets, confirmOrder);
        }
        catch (Exception e) {
            logger.error("保存购票信息失败", e);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
    }

    /**
     * 挑座位，如果有选座则一次性挑完，如果无选座，则一个一个挑
     * @param date
     * @param trainCode
     * @param SeatType
     * @param column
     * @param offsetList
     */

    private void getSeat(Date date, String trainCode, String SeatType, String column, List<Integer> offsetList
                        , Integer startIndex, Integer endIndex, List<DailyTrainSeat> finalSeatList) {

        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageService.selectBySeatType(date, trainCode, SeatType);
        logger.info("共查出{}个符合条件的车厢",carriageList.size());

        // 一个车厢一个车厢的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : carriageList) {
            logger.info("开始从车厢{}选座", dailyTrainCarriage.getIndex());
            getSeatList = new ArrayList<>();
            List<DailyTrainSeat> seatList = dailyTrainSeatService.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            logger.info("车厢{}的座位数：{}", dailyTrainCarriage.getIndex(), seatList.size());

            for (int i = 0; i < seatList.size(); i++) {
                DailyTrainSeat dailyTrainSeat = seatList.get(i);
                String col = dailyTrainSeat.getCol();
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();


                boolean alreadyChooseFlag = false;
                // 判断当前座位不能被选中过
                for (DailyTrainSeat finalSeat: finalSeatList) {
                    if(finalSeat.getId().equals(dailyTrainSeat.getId())){
                        alreadyChooseFlag = true;
                        break;
                    }
                }

                if(alreadyChooseFlag){
                    logger.info("座位{}被选中过，不能重复选中，继续判断下一个座位", seatIndex);
                    continue;
                }

                // 判断column，有值的话要比对列号
                if (StrUtil.isNotBlank(col)) {
                    logger.info("无选座");
                } else {
                    if (!column.equals(col)) {
                        logger.info("座位{}列值不对，继续判断下一个座位，当前列值:{}，目标列值:{}", seatIndex, col, column);
                        continue;
                    }
                }

                boolean isChoose = calSell(dailyTrainSeat, startIndex, endIndex);
                if (isChoose) {
                    getSeatList.add(dailyTrainSeat);
                    logger.info("选中座位");
                } else {
                    continue;
                }

                // 根据offset选剩下的座位
                boolean isGetAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetList)) {
                    logger.info("有偏移值:{},校验偏移的座位是否可选", offsetList);
                    for (int j = 1; j < offsetList.size(); j++) {
                        Integer offset = offsetList.get(j);
                        // 座位在库的索引是从1开始
                        int nextIndex = i + offset;

                        // 有选座时，一定是在同一个车厢
                        if (nextIndex >= seatList.size()) {
                            logger.info("座位{}不可选，偏移后的索引超出了这个车厢的座位数", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }


                        DailyTrainSeat nextDailyTrainSeat = seatList.get(nextIndex);
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
                            getSeatList.add(nextDailyTrainSeat);
                            logger.info("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                        } else {
                            logger.info("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }

                if (!isGetAllOffsetSeat) {
                    getSeatList = new ArrayList<>();
                    continue;
                }
                logger.info("getSeatList:{}", getSeatList);
                // 保存选好的座位
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 更新状态
     * @param confirmOrder
     */
    public void updateStatus(ConfirmOrder confirmOrder) {
        ConfirmOrder confirmOrderForUpdate = new ConfirmOrder();
        confirmOrderForUpdate.setId(confirmOrder.getId());
        confirmOrderForUpdate.setUpdateTime(new Date());
        confirmOrderForUpdate.setStatus(confirmOrder.getStatus());
        confirmOrderMapper.updateByPrimaryKeySelective(confirmOrderForUpdate);
    }

    /**
     * 计算某座位在区间内是否可卖
     * 例：sell= "10001", 本次购买区间站1~4，则区间已售000
     * 全部是0表示这个区间可买；只要有1就表示区间内已售过票
     * <p>
     * 选中后，要计算购票后的sell，比如原来是10001， 本次购票区间站1~4
     * 方案：构造本次购票造成的售卖信息01110，和原sell 10001按位或，最终得到11111
     */
    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex){
        // 10001
        String sell = dailyTrainSeat.getSell();
        // 000
        String sellPart = sell.substring(startIndex, endIndex);
        if(Integer.parseInt(sellPart) > 0){
            logger.info("座位{}在本次车站区间{} ~ {}已售过票，不可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        }
        else {
            logger.info("座位{}在本次车站区间{} ~ {}未售过票，可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            String curSell = sellPart.replace('0', '1');
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());

            // 当前区间售票信息curSell与库里的已售信息sell按位或， 即可得到该座位卖出此票后的售票详情
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            StrUtil.fillBefore(newSell, '0', sell.length());

            logger.info("座位{}被选中，原售票信息:{}，车站区间{} ~ {}，最终售票信息:{}"
                    , dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex
                    , endIndex, newSell);

            dailyTrainSeat.setSell(newSell);
            return true;
        }
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

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public void doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        logger.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }


    /**
     * 查询前面还有几个人在排队
     * @param id
     * @return
     */
    public Integer queryLineCount(Long id){
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case INIT -> 999; // 需要查表得到实际排队数量
            case PENDING -> 0; // 排队0
            case SUCCESS -> -1; //成功
            case FAILURE -> -2; // 失败
            case EMPTY -> -3;   // 无票
            case CANCEL -> -4;  // 取消
        };

        if(result == 999){
            ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());

            return Math.toIntExact(confirmOrderMapper.countByExample(confirmOrderExample));
        }
        else {
            return result;
        }
    }
}
