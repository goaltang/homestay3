package com.homestay3.homestaybackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.homestay3.homestaybackend.service.impl.AmenityServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private AmenityServiceImpl amenityService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("初始化设施数据...");
        amenityService.initDefaultAmenities();
        log.info("设施数据初始化完成");
    }
} 