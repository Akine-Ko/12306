package com.akine.mytrain.business.mapper;

import com.akine.mytrain.business.domain.TrainCarriage;
import com.akine.mytrain.business.domain.TrainCarriageExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TrainCarriageMapper {
    long countByExample(TrainCarriageExample example);

    int deleteByExample(TrainCarriageExample example);

    int deleteByPrimaryKey(Long id);

    int insert(TrainCarriage record);

    int insertSelective(TrainCarriage record);

    List<TrainCarriage> selectByExample(TrainCarriageExample example);

    TrainCarriage selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TrainCarriage record, @Param("example") TrainCarriageExample example);

    int updateByExample(@Param("record") TrainCarriage record, @Param("example") TrainCarriageExample example);

    int updateByPrimaryKeySelective(TrainCarriage record);

    int updateByPrimaryKey(TrainCarriage record);
}