package com.example.ragbackend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.example.ragbackend.audit.mapper",
        "com.example.ragbackend.chat.mapper",
        "com.example.ragbackend.chunk.mapper",
        "com.example.ragbackend.evaluation.mapper",
        "com.example.ragbackend.knowledge.mapper",
        "com.example.ragbackend.document.mapper"
})
public class MybatisPlusConfig {
}
