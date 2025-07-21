package com.akine.mytrain.member.enums;

public enum PassengerTypeEnum {
    ADULT("1", "成人"),
    CHILD("2", "儿童"),
    STUDENT("3", "学生")
    ;

    private String code;

    PassengerTypeEnum(String code, String desc){
        this.code = code;
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private String desc;



    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PassengerTypeEnum{");
        sb.append("code='").append(code).append('\'');
        sb.append(", desc='").append(desc).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
