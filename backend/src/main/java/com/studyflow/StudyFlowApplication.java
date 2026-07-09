package com.studyflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan(value = "com.studyflow", markerInterface = BaseMapper.class)
@SpringBootApplication
public class StudyFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyFlowApplication.class, args);
    }
}
