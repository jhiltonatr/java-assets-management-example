package com.example.company.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SampleApplication {

    public final static Integer foo = 0;

    public static void main(String[] args) {
        int foo;
        SpringApplication.run(SampleApplication.class, args);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello from the standards-built Spring Boot app";
    }
}