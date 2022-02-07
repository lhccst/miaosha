package com.lhc.ms.mapper;

import com.lhc.ms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author lhc
 * @create --
 */
@Mapper
public interface UserMapper {

    @Select("select * from user where id = #{userId}")
     User findById(Integer userId);
}
