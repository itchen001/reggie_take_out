package com.chen.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chen.reggie.common.BaseContext;
import com.chen.reggie.common.R;
import com.chen.reggie.entity.ShoppingCart;
import com.chen.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加到购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据：{}", shoppingCart);
        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);

        if (dishId != null) {
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        }else {
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中
        //SQL: select * from shopping_cart where user_id = ? and dish_id = ? / setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null) {
            //如果已经存在，就在原来的数量上加1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);

            shoppingCartService.updateById(cartServiceOne);

        }else {
            //如果不存在，则添加到购物车，数量默认为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartService.save(shoppingCart);

            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车");
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    @DeleteMapping("/clean")
    public R<String> clean() {
        log.info("清空购物车");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(queryWrapper);

        return R.success("清空成功");
    }

    /**
     *点击减号，订单减一或删除订单
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        log.info("减", shoppingCart);

        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();

        if (dishId != null) {
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
            ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
            Integer number = cartServiceOne.getNumber();
            if (number > 1) {
                cartServiceOne.setNumber(number -1);
                shoppingCartService.updateById(cartServiceOne);
            }else {
                shoppingCartService.removeById(cartServiceOne);
            }

        }else {
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
            ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
            Integer number = cartServiceOne.getNumber();
            if (number > 1) {
                cartServiceOne.setNumber(number -1);
                shoppingCartService.updateById(cartServiceOne);
            }else {
                shoppingCartService.removeById(cartServiceOne);
            }
        }


        return R.success(shoppingCart);
    }

}
