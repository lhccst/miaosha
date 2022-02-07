package com.lhc.ms.mapper;

import com.lhc.ms.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @author lhc
 * @create --
 */

@Mapper
@Repository
public interface StockMapper {

    @Select("select * from stock where id = #{id}")
    Stock getById(Integer id);

    int updateById(Stock stock);

    int updateSale(Stock stock);
}
