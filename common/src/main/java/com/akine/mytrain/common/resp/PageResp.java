package com.akine.mytrain.common.resp;

import java.io.Serializable;
import java.util.List;

public class PageResp<T> implements Serializable {
    //总条数
    private Long total;

    //当前页的列表
    private List<T> list;

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
