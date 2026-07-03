package com.studyflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.studyflow")
@SpringBootApplication
public class StudyFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyFlowApplication.class, args);
    }
}
