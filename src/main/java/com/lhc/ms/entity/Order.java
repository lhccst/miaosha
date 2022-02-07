package com.lhc.ms.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.jws.HandlerChain;
import java.util.Date;

/**
 * @author lhc
 * @create --
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Order {
    private Integer id;
    private Integer sid;
    private String name;
    private Date CreateTime;
}
