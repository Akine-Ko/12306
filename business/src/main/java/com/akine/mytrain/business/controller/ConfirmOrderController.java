package com.akine.mytrain.business.controller;

import com.akine.mytrain.business.req.ConfirmOrderDoReq;
import com.akine.mytrain.business.service.BeforeConfirmOrderService;
import com.akine.mytrain.business.service.ConfirmOrderService;
import com.akine.mytrain.common.exception.BusinessExceptionEnum;
import com.akine.mytrain.common.resp.CommonResp;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @Resource
    private ConfirmOrderService confirmOrderService;

    private static final Logger logger = LoggerFactory.getLogger(ConfirmOrderController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @SentinelResource(value = "confirmOrderDo", blockHandler = "doConfirmBlock")
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) throws InterruptedException {
        // 图形验证码校验
        String imageCodeToken = req.getImageCodeToken();
        String imageCode = req.getImageCode();
        String imageCodeRedis = redisTemplate.opsForValue().get(imageCodeToken);
        logger.info("从redis中获取到的验证码：{}", imageCodeRedis);
        if (ObjectUtils.isEmpty(imageCodeRedis)) {
            return new CommonResp<>(false, "验证码已过期", null);
        }
        // 验证码校验，大小写忽略，提升体验，比如Oo Vv Ww容易混
        if (!imageCodeRedis.equalsIgnoreCase(imageCode)) {
            return new CommonResp<>(false, "验证码不正确", null);
        } else {
            // 验证通过后，移除验证码
            redisTemplate.delete(imageCodeToken);
        }

        beforeConfirmOrderService.beforeDoConfirm(req);
        return new CommonResp<>();
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    public CommonResp<Object> doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        logger.info("购票请求被限流：{}", req);
        CommonResp<Object> commonResp = new CommonResp<>();
        commonResp.setSuccess(false);
        commonResp.setMessage(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION.getDesc());
        return commonResp;
    }

}
