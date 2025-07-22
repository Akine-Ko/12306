package com.akine.mytrain.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.akine.mytrain.common.context.LoginMemberContext;
import com.akine.mytrain.common.resp.MemberLoginResp;
import com.akine.mytrain.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MemberInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(MemberInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if(StrUtil.isNotBlank(token)){
            log.info("获取会员登录token: {}", token);
            JSONObject loginMember = JwtUtil.getJSONObject(token);
            log.info("当前登录会员:{}", loginMember);
            MemberLoginResp member = JSONUtil.toBean(loginMember, MemberLoginResp.class);
            LoginMemberContext.setMember(member);
        }

        return true;
    }
}
