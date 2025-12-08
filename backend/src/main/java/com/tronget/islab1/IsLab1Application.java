package com.tronget.islab1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IsLab1Application {

    public static void main(String[] args) {
        SpringApplication.run(IsLab1Application.class, args);
    }
}
