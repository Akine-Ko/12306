package com.akine.mytrain.business.req;

import com.akine.mytrain.common.req.PageReq;

public class TrainCarriageQueryReq extends PageReq {


    private String trainCode;


    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    @Override
    public String toString() {
        return "TrainCarriageQueryReq{" +
                "trainCode='" + trainCode + '\'' +
                "} " + super.toString();
    }
}