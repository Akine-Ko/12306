package com.akine.mytrain.common.context;

import com.akine.mytrain.common.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginMemberContext {
    private static final Logger logger = LoggerFactory.getLogger(LoginMemberContext.class);

    private static ThreadLocal<MemberLoginResp> member = new ThreadLocal<>();

    public static void setMember(MemberLoginResp member) {
        LoginMemberContext.member.set(member);
    }

    public static Long getId(){
        try{
            return member.get().getId();
        }catch(Exception e){
            logger.error("获取登录会员信息异常", e);
            throw e;
        }
    }

}
