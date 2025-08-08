package com.akine.mytrain.business.service;

import cn.hutool.core.date.DateUtil;
import com.akine.mytrain.business.enums.RedisKeyPreEnum;
import com.akine.mytrain.business.mapper.ConfirmOrderMapper;
import com.akine.mytrain.business.req.ConfirmOrderDoReq;
import com.akine.mytrain.common.context.LoginMemberContext;
import com.akine.mytrain.common.exception.BusinessException;
import com.akine.mytrain.common.exception.BusinessExceptionEnum;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BeforeConfirmOrderService {

    private static final Logger logger = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

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

    @Resource
    private SkTokenService skTokenService;

    @Resource
    private RedissonClient redissonClient;


    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public void beforeDoConfirm(ConfirmOrderDoReq req) throws InterruptedException {
        // 校验令牌余量
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
        if (validSkToken) {
            logger.info("令牌校验通过");
        } else {
            logger.info("令牌校验不通过");
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-" + DateUtil.formatDate(req.getDate()) + "-" + req.getTrainCode();
        RLock lock = redissonClient.getLock(lockKey);
        boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS);

        if (tryLock) {
            logger.info("恭喜抢到锁了");
        } else {
            logger.info("很遗憾，没抢到锁");
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
        }

        logger.info("准备发送MQ，等待出票");
    }


    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public void beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        logger.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }


}
