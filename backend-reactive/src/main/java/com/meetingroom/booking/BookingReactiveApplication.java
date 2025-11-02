package com.meetingroom.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication(exclude = {RedisReactiveAutoConfiguration.class})
@EnableR2dbcAuditing
public class BookingReactiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingReactiveApplication.class, args);
    }
}
