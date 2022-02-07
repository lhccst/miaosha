package com.lhc.ms.mapper;



import com.lhc.ms.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OrderMapper {

      void add(Order stock);
}
