package com.lhc.ms.service;

/**
 * @author lhc
 * @create --
 */
public interface OrderService {
    public int kill(Integer id);

    int kill2(Integer id);

    String getMd5(Integer id, Integer userId);

    int killMd5(Integer id, Integer userId, String md5);

    int kill5(Integer id, Integer userId, String md5);
}
