package com.example.hostgame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HostGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(HostGameApplication.class, args);
    }
}
