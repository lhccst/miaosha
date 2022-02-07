package com.lhc.ms.controller;


import com.google.common.util.concurrent.RateLimiter;
import com.lhc.ms.service.OrderService;
import com.lhc.ms.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

//    令牌桶
    private RateLimiter rateLimiter = RateLimiter.create(10);


    @GetMapping("kill5")
    public R kill5(Integer id,Integer userId,String md5) {
        try {
            int orderId = orderService.kill5(id,userId,md5);
            System.out.println("秒杀订单id为" + orderId);
            return R.ok().add("秒杀订单id", orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error().setMessage(e.getMessage());
        }
    }
    /**
     * @Author lhc
     * @Description 接口隐藏 + 乐观锁 + 令牌桶接口限流 + redis限时抢购
     * @Date 20:54 2022/1/31
     * @Param [id]
     * @return com.lhc.ms.util.R
     **/
    @GetMapping("kill4")
    public R kill4(Integer id,Integer userId,String md5) {
        try {
            int orderId = orderService.killMd5(id,userId,md5);
            System.out.println("秒杀订单id为" + orderId);
            return R.ok().add("秒杀订单id", orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error().setMessage(e.getMessage());
        }
    }
    /**
     * @Author lhc
     * @Description 根据商品id 用户id 生成 md5 加密字符串
     * @Date 17:33 2022/1/31
     * @Param [id, userId]
     * @return java.lang.String
     **/
    @GetMapping("getmd5")
    public String getMd5(Integer id,Integer userId) {
        String md5;
        try{
            md5 = orderService.getMd5(id,userId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return md5;
    }

    /**
     * @Author lhc
     * @Description 乐观锁 + 令牌桶接口限流 + redis限时抢购
     **/
    @GetMapping("kill3")
    public R kill3(Integer id) {
        if (!stringRedisTemplate.hasKey("kill" + id)){
            throw new RuntimeException("不在抢购时间内");
        }
        if(!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
            throw new RuntimeException("当前活动火爆,请稍后再试");
        }
        try {
            int orderId = orderService.kill2(id);
            System.out.println("秒杀订单id为" + orderId);
            return R.ok().add("秒杀订单id", orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error();
        }
    }
    /**
     * @Author lhc
     * @Description 乐观锁
     * @Date 17:37 2022/1/27
     **/
    @GetMapping("kill2")
    public R kill2(Integer id) {
        //        System.out.println("秒杀商品id为"+id);
        try {
            int orderId = orderService.kill2(id);
            System.out.println("秒杀订单id为" + orderId);
            return R.ok().add("秒杀订单id", orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error();
        }
    }

    @GetMapping("kill")
    public R kill(Integer id) {
//        System.out.println("秒杀商品id为"+id);
        try {
            synchronized (this) {
                int orderId = orderService.kill(id);
                System.out.println("秒杀订单id为" + orderId);
                return R.ok().add("秒杀订单id", orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.error();
        }
    }


}
