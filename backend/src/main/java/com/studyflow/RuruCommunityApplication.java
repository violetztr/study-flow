package com.studyflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value = "com.studyflow", markerInterface = BaseMapper.class)
@SpringBootApplication
public class RuruCommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuruCommunityApplication.class, args);
    }
}
