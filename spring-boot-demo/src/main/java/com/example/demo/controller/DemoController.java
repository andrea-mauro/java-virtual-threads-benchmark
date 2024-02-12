package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/demo")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/cpu-light")
    public String getCpuLight() {

        var initialTime = System.currentTimeMillis();
        someIoBlockingOperation(100);
        someIoBlockingOperation(100);

        logger.info("Request processed in {} ms", System.currentTimeMillis() - initialTime);
        return "Hello, World!";
    }

    @GetMapping("/cpu-intensive")
    public String getCpuIntensive() {

        var initialTime = System.currentTimeMillis();
        someIoBlockingOperation(100);
        someCpuIntensiveTask(10);
        someIoBlockingOperation(100);

        logger.info("Request processed in {} ms", System.currentTimeMillis() - initialTime);
        return "Hello, World!";
    }

    private void someIoBlockingOperation(int ms) {

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            logger.error("Error while performing IO operation", e);
        }
    }

    private void someCpuIntensiveTask(int ms) {

        var currentTime = System.currentTimeMillis();
        while (currentTime + ms > System.currentTimeMillis()) {
            // do nothing
        }
    }
}
