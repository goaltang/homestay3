package com.homestay3.homestaybackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/locations", "/api/v1/locations"})
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
    allowedHeaders = "*", 
    methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
        RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH
    },
    exposedHeaders = {
        "Content-Type", "Content-Length", "Authorization",
        "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", 
        "Access-Control-Allow-Methods"
    },
    allowCredentials = "true",
    maxAge = 3600
)
public class LocationController {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    
    /**
     * 获取省份列表
     */
    @GetMapping("/provinces")
    public ResponseEntity<Map<String, Object>> getProvinces() {
        logger.info("获取省份列表");
        Map<String, Object> response = new HashMap<>();
        
        // 模拟数据 - 实际应用中应从数据库获取
        List<Map<String, Object>> provinces = Arrays.asList(
            createLocation("110000", "北京市"),
            createLocation("120000", "天津市"),
            createLocation("130000", "河北省"),
            createLocation("140000", "山西省"),
            createLocation("150000", "内蒙古自治区"),
            createLocation("210000", "辽宁省"),
            createLocation("220000", "吉林省"),
            createLocation("230000", "黑龙江省"),
            createLocation("310000", "上海市"),
            createLocation("320000", "江苏省"),
            createLocation("330000", "浙江省"),
            createLocation("340000", "安徽省"),
            createLocation("350000", "福建省"),
            createLocation("360000", "江西省"),
            createLocation("370000", "山东省"),
            createLocation("410000", "河南省"),
            createLocation("420000", "湖北省"),
            createLocation("430000", "湖南省"),
            createLocation("440000", "广东省"),
            createLocation("450000", "广西壮族自治区"),
            createLocation("460000", "海南省"),
            createLocation("500000", "重庆市"),
            createLocation("510000", "四川省"),
            createLocation("520000", "贵州省"),
            createLocation("530000", "云南省"),
            createLocation("540000", "西藏自治区"),
            createLocation("610000", "陕西省"),
            createLocation("620000", "甘肃省"),
            createLocation("630000", "青海省"),
            createLocation("640000", "宁夏回族自治区"),
            createLocation("650000", "新疆维吾尔自治区"),
            createLocation("710000", "台湾省"),
            createLocation("810000", "香港特别行政区"),
            createLocation("820000", "澳门特别行政区")
        );
        
        response.put("success", true);
        response.put("data", provinces);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取城市列表
     */
    @GetMapping("/cities")
    public ResponseEntity<Map<String, Object>> getCities(@RequestParam String provinceCode) {
        logger.info("获取城市列表，省份代码: {}", provinceCode);
        Map<String, Object> response = new HashMap<>();
        
        // 根据省份代码返回相应的城市（这里简化为示例数据）
        List<Map<String, Object>> cities;
        
        if ("440000".equals(provinceCode)) { // 广东省
            cities = Arrays.asList(
                createLocation("440100", "广州市"),
                createLocation("440300", "深圳市"),
                createLocation("440400", "珠海市"),
                createLocation("440600", "佛山市"),
                createLocation("441900", "东莞市"),
                createLocation("442000", "中山市")
            );
        } else if ("110000".equals(provinceCode)) { // 北京市
            cities = Arrays.asList(
                createLocation("110100", "北京市")
            );
        } else if ("310000".equals(provinceCode)) { // 上海市
            cities = Arrays.asList(
                createLocation("310100", "上海市")
            );
        } else {
            // 默认返回一些示例城市
            cities = Arrays.asList(
                createLocation("000100", "示例市1"),
                createLocation("000200", "示例市2"),
                createLocation("000300", "示例市3")
            );
        }
        
        response.put("success", true);
        response.put("data", cities);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取区县列表
     */
    @GetMapping("/districts")
    public ResponseEntity<Map<String, Object>> getDistricts(@RequestParam String cityCode) {
        logger.info("获取区县列表，城市代码: {}", cityCode);
        Map<String, Object> response = new HashMap<>();
        
        // 根据城市代码返回相应的区县（这里简化为示例数据）
        List<Map<String, Object>> districts;
        
        if ("440100".equals(cityCode)) { // 广州市
            districts = Arrays.asList(
                createLocation("440103", "荔湾区"),
                createLocation("440104", "越秀区"),
                createLocation("440105", "海珠区"),
                createLocation("440106", "天河区"),
                createLocation("440111", "白云区"),
                createLocation("440112", "黄埔区")
            );
        } else if ("440300".equals(cityCode)) { // 深圳市
            districts = Arrays.asList(
                createLocation("440303", "罗湖区"),
                createLocation("440304", "福田区"),
                createLocation("440305", "南山区"),
                createLocation("440306", "宝安区"),
                createLocation("440307", "龙岗区"),
                createLocation("440308", "盐田区")
            );
        } else {
            // 默认返回一些示例区县
            districts = Arrays.asList(
                createLocation("000101", "示例区1"),
                createLocation("000102", "示例区2"),
                createLocation("000103", "示例区3")
            );
        }
        
        response.put("success", true);
        response.put("data", districts);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建位置对象
     */
    private Map<String, Object> createLocation(String code, String name) {
        Map<String, Object> location = new HashMap<>();
        location.put("code", code);
        location.put("name", name);
        return location;
    }
} 