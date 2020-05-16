package com.fanjia.spring.demo.service.impl;

import com.fanjia.spring.demo.service.IDemoSerivce;
import com.fanjia.spring.mvcframework.annotation.FJService;

/**
 * description: DemoServiceImpl <br>
 * date: 2020/4/18 9:53 <br>
 * author: Administrator <br>
 * version: 1.0 <br>
 */
@FJService
public class DemoServiceImpl  implements IDemoSerivce {
    public String get(String name) {
        return "my name is"+name;
    }
}
