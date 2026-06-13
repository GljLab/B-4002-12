package com.label4002.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Label4002Application {

    public static void main(String[] args) {
        SpringApplication.run(Label4002Application.class, args);
    }
}
