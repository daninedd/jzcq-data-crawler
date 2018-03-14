package com.caile.jczq.data.crawler;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**@author denglei
 * Created by Administrator on 2018/3/14.
 */
@Service
@RestController
public class HistoryCraw {


    @RequestMapping("/league")
    public String league(){

        return "ok";
    }
}
