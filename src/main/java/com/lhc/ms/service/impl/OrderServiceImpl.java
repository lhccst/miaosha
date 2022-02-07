package com.lhc.ms.service.impl;

import com.google.common.util.concurrent.RateLimiter;
import com.lhc.ms.entity.Order;
import com.lhc.ms.entity.Stock;
import com.lhc.ms.mapper.OrderMapper;
import com.lhc.ms.mapper.StockMapper;
import com.lhc.ms.mapper.UserMapper;
import com.lhc.ms.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import com.lhc.ms.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author lhc
 * @create --
 */
@Service
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    StockMapper stockMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    //    令牌桶
    private RateLimiter rateLimiter = RateLimiter.create(10);
    // 单用户访问次数
    private final int LIMIT = 3;

    /**
     * @Author lhc
     * @Description 接口隐藏 + 乐观锁 + 令牌桶接口限流 + redis限时抢购 + 单用户接口调用频率限制
     * @Param [id, userId, md5]
     * @return int
     **/
    @Override
    public int kill5(Integer id, Integer userId, String md5) {
        //redis限时抢购
        if (!stringRedisTemplate.hasKey("kill" + id)){
            throw new RuntimeException("不在抢购时间内");
        }

        //接口限流
        if(!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
            throw new RuntimeException("当前活动火爆,请稍后再试");
        }
        //接口隐藏
        String key = "key" + id + userId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if(!md5.equals(value)){
            //证明不是同一个用户
            throw new RuntimeException("非法请求");
        }
        //单用户接口调用频率限制
        SaveUserCount(userId);
        int count = getUserCount(userId);
        log.info("用户请求了[{}]次",count);
        if(count > LIMIT){
            throw new RuntimeException("请求过于频繁请稍后再试");
        }
        //        校验库存
        Stock stock = checkStock(id);
        //        扣除库存(乐观锁)
        updateStock(stock);
        //        新增订单
        return createOrder(stock);
    }



    private void SaveUserCount(Integer userId){
        String limitKey = "limit" + userId;
        String value = stringRedisTemplate.opsForValue().get(limitKey);
        if(value == null) {
            stringRedisTemplate.opsForValue().set(limitKey,"1",60,TimeUnit.SECONDS);
        }else{
            stringRedisTemplate.opsForValue().increment(limitKey,1);
        }
    }

    private int getUserCount(Integer userId) {
        String limitKey = "limit" + userId;
        return Integer.parseInt(stringRedisTemplate.opsForValue().get(limitKey));
    }
    /**
     * @Author lhc
     * @Description 接口隐藏 + 乐观锁 + 令牌桶接口限流 + redis限时抢购
     * @Param [id, userId, md5]
     * @return int
     **/
    @Override
    public int killMd5(Integer id, Integer userId, String md5) {
        //redis限时抢购
        if (!stringRedisTemplate.hasKey("kill" + id)){
            throw new RuntimeException("不在抢购时间内");
        }
        //接口隐藏
        String key = "key" + id + userId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if(!md5.equals(value)){
            //证明不是同一个用户
            throw new RuntimeException("非法请求");
        }
        //接口限流
        if(!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
            throw new RuntimeException("当前活动火爆,请稍后再试");
        }
        //        校验库存
        Stock stock = checkStock(id);
        //        扣除库存(乐观锁)
        updateStock(stock);
        //        新增订单
        return createOrder(stock);
    }


    @Override
    public String getMd5(Integer id, Integer userId) {
        //验证Userid 存在用户信息
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户信息不存在");
        }
        log.info("用户信息： [{}]", user.toString());
        //验证id 存在商品信息
        Stock stock = stockMapper.getById(id);

        if (stock == null) {
            throw new RuntimeException("商品信息不合法");
        }
        log.info("商品信息: [{}]", stock.toString());
        String key = "key" + id + userId;
        String value = DigestUtils.md5DigestAsHex((key + "zj%a").getBytes());
        stringRedisTemplate.opsForValue().set(key,value,60, TimeUnit.SECONDS);
        log.info("redis写入: [{}] [{}]",key,value);
        return value;
    }


    /**
     * @return
     * @Author lhc
     * @Description 乐观锁
     * @Date 15:46 2022/1/26
     * @Param
     **/
    @Override
    public int kill2(Integer id) {
        //        校验库存
        Stock stock = checkStock(id);
        //        扣除库存
        updateStock(stock);
        //        新增订单
        return createOrder(stock);
    }

    //        校验库存
    private Stock checkStock(Integer id) {
        Stock stock = stockMapper.getById(id);
        if (stock.getCount().equals(stock.getSale())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }

    //        扣除库存
    private void updateStock(Stock stock) {
        int rows = stockMapper.updateSale(stock);
        if (rows == 0) {
            throw new RuntimeException("购买失败请重试");
        }
    }

    //        新增订单
    private Integer createOrder(Stock stock) {
        Order order = new Order();
        order.setName(stock.getName()).setSid(stock.getId()).setCreateTime(new Date());
        orderMapper.add(order);
        log.info("商品秒杀成功,商品id为" + order.getId());
        return order.getId();
    }

    /**
     * @return int
     * @Author lhc
     * @Description 悲观锁
     * @Date 17:35 2022/1/25
     * @Param [id]
     **/
    @Override
    //线程范围>事务范围 要在控制器调用用synchronized
    public int kill(Integer id) {
        Stock stock = stockMapper.getById(id);
        //        校验库存
        log.info(stock.getCount().equals(stock.getSale()) + "");
        if (stock.getCount().equals(stock.getSale())) {
            throw new RuntimeException("库存不足");
        } else {
            //        扣除库存
            stock.setSale(stock.getSale() + 1);
            int i = stockMapper.updateById(stock);
            //        新增订单
            Order order = new Order();
            order.setName(stock.getName()).setSid(stock.getId()).setCreateTime(new Date());
            orderMapper.add(order);
            log.info("商品秒杀成功,商品id为" + order.getId());
            return order.getId();
        }
    }
}
