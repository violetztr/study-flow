package com.studyflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@MapperScan(value = "com.studyflow", markerInterface = BaseMapper.class)
@SpringBootApplication
@EnableAsync
public class RuruCommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuruCommunityApplication.class, args);
    }
}
