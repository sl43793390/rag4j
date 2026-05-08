package com.sl.rag4j;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sl.rag4j.mapper")
public class Rag4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rag4jApplication.class, args);
    }

}