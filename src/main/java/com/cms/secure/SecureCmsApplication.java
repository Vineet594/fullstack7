package com.cms.secure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SecureCmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureCmsApplication.class, args);
    }
}
