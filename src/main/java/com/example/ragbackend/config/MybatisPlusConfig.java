package com.example.ragbackend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.ragbackend.knowledge.mapper")
public class MybatisPlusConfig {

}
