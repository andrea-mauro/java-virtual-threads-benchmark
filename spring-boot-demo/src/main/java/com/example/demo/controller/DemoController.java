package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class DemoController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    
    @GetMapping("/demo")
    public void get() throws InterruptedException {
        Thread.sleep(200);
        /** FIXME to simulate some CPU work
         var currentTime = System.currentTimeMillis();
         while(currentTime + 10 > System.currentTimeMillis()) {
         // simulate some intensive CPU work
         }
         */
        Thread.sleep(200);
        logger.info("Running on Thread {}", Thread.currentThread().getName());
    }
}
