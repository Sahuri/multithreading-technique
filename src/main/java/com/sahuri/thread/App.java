package com.sahuri.thread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class App{
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    /** RestTemplate disediakan sebagai bean supaya bisa diganti/diuji, bukan dibuat di dalam service. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

