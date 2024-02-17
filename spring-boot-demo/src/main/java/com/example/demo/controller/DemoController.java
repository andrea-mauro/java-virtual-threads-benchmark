package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController()
@RequestMapping("/demo")
public class DemoController {

    private final Environment environment;

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    private final String VIRTUAL_THREAD_PROFILE = "virtual";
    private final String MONO_VIRTUAL_THREAD_PROFILE = "mono-virtual";

    @Autowired
    public DemoController(final Environment environment) {

        this.environment = environment;
    }

    @GetMapping()
    public String demo() {

        var activeProfiles = Set.of(environment.getActiveProfiles());
        if (activeProfiles.contains(VIRTUAL_THREAD_PROFILE)) {
            return "Hello from the Spring Boot app running with Virtual Threads enabled!";
        } else if (activeProfiles.contains(MONO_VIRTUAL_THREAD_PROFILE)) {
            return "Hello from the Spring Boot app running on one Thread and with Virtual Threads enabled!";
        } else {
            return "Hello from the Spring Boot app running on normal Threads!";
        }
    }

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
