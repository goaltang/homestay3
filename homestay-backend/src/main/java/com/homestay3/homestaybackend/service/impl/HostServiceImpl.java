package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.HostDTO;
import com.homestay3.homestaybackend.dto.HostStatisticsDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.ReviewDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.FileService;
import com.homestay3.homestaybackend.service.HostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HostServiceImpl implements HostService {

    private static final Logger logger = LoggerFactory.getLogger(HostServiceImpl.class);
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Override
    public HostDTO getHostInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));

        HostDTO hostDTO = HostDTO.fromUser(user);
        
        // 解析languages字段
        if (user.getLanguages() != null && !user.getLanguages().isEmpty()) {
            try {
                List<String> languages = objectMapper.readValue(user.getLanguages(), new TypeReference<List<String>>() {});
                hostDTO.setLanguages(languages);
            } catch (JsonProcessingException e) {
                logger.error("解析语言字段失败", e);
                hostDTO.setLanguages(new ArrayList<>());
            }
        } else {
            hostDTO.setLanguages(new ArrayList<>());
        }
        
        // 解析companions字段
        if (user.getCompanions() != null && !user.getCompanions().isEmpty()) {
            try {
                List<Map<String, String>> companions = objectMapper.readValue(user.getCompanions(), 
                        new TypeReference<List<Map<String, String>>>() {});
                hostDTO.setCompanions(companions);
            } catch (JsonProcessingException e) {
                logger.error("解析接待伙伴字段失败", e);
                hostDTO.setCompanions(new ArrayList<>());
            }
        } else {
            hostDTO.setCompanions(new ArrayList<>());
        }
        
        // 填充统计数据
        HostStatisticsDTO statistics = getHostStatistics(username);
        hostDTO.setHomestayCount(statistics.getHomestayCount());
        hostDTO.setOrderCount(statistics.getOrderCount());
        hostDTO.setReviewCount(statistics.getReviewCount());
        hostDTO.setRating(statistics.getRating());
        
        return hostDTO;
    }

    @Override
    @Transactional
    public HostDTO updateHostInfo(HostDTO hostDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        // 更新基本信息
        if (hostDTO.getNickname() != null) {
            user.setNickname(hostDTO.getNickname());
        }
        if (hostDTO.getEmail() != null) {
            user.setEmail(hostDTO.getEmail());
        }
        if (hostDTO.getPhone() != null) {
            user.setPhone(hostDTO.getPhone());
        }
        if (hostDTO.getRealName() != null) {
            user.setRealName(hostDTO.getRealName());
        }
        if (hostDTO.getIdCard() != null) {
            user.setIdCard(hostDTO.getIdCard());
        }
        
        // 更新房东专属信息
        if (hostDTO.getOccupation() != null) {
            user.setOccupation(hostDTO.getOccupation());
        }
        if (hostDTO.getIntroduction() != null) {
            user.setIntroduction(hostDTO.getIntroduction());
        }
        if (hostDTO.getHostResponseRate() != null) {
            user.setHostResponseRate(hostDTO.getHostResponseRate());
        }
        if (hostDTO.getHostResponseTime() != null) {
            user.setHostResponseTime(hostDTO.getHostResponseTime());
        }
        
        // 序列化languages字段
        if (hostDTO.getLanguages() != null) {
            try {
                String languagesJson = objectMapper.writeValueAsString(hostDTO.getLanguages());
                user.setLanguages(languagesJson);
            } catch (JsonProcessingException e) {
                logger.error("序列化语言字段失败", e);
            }
        }
        
        // 序列化companions字段
        if (hostDTO.getCompanions() != null) {
            try {
                String companionsJson = objectMapper.writeValueAsString(hostDTO.getCompanions());
                user.setCompanions(companionsJson);
            } catch (JsonProcessingException e) {
                logger.error("序列化接待伙伴字段失败", e);
            }
        }
        
        // 如果是新房东，设置hostSince字段
        if (user.getHostSince() == null) {
            user.setHostSince(LocalDateTime.now());
        }
        
        user = userRepository.save(user);
        return getHostInfo(username); // 返回完整信息
    }

    @Override
    public String uploadAvatar(MultipartFile file, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        // 使用FileService上传文件
        String fileUrl = fileService.uploadFile(file);
        
        // 更新用户头像
        user.setAvatar(fileUrl);
        userRepository.save(user);
        
        return fileUrl;
    }

    @Override
    public HostStatisticsDTO getHostStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        // 获取房源数量
        Long homestayCount = homestayRepository.countByOwnerId(user.getId());
        
        // 获取订单总数
        Long orderCount = orderRepository.countByHostId(user.getId());
        
        // 获取待处理订单数
        Long pendingOrders = orderRepository.countByHostIdAndStatus(user.getId(), "PENDING");
        
        // 获取已完成订单数
        Long completedOrders = orderRepository.countByHostIdAndStatus(user.getId(), "COMPLETED");
        
        // 获取已取消订单数
        Long cancelledOrders = orderRepository.countByHostIdAndStatus(user.getId(), "CANCELLED");
        
        // 获取评价总数
        Long reviewCount = reviewRepository.countByHostId(user.getId());
        
        // 获取平均评分
        Double avgRating = reviewRepository.getAverageRatingByHostId(user.getId());
        
        // 获取总收入
        Double totalEarnings = orderRepository.getTotalEarningsByHostId(user.getId());
        
        return HostStatisticsDTO.builder()
                .homestayCount(homestayCount.intValue())
                .orderCount(orderCount.intValue())
                .reviewCount(reviewCount.intValue())
                .rating(avgRating)
                .totalEarnings(totalEarnings)
                .pendingOrders(pendingOrders.intValue())
                .completedOrders(completedOrders.intValue())
                .cancelledOrders(cancelledOrders.intValue())
                .build();
    }

    @Override
    public Page<?> getHostHomestays(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return homestayRepository.findByOwnerId(user.getId(), pageable)
                .map(homestay -> convertToHomestayDTO(homestay));
    }

    @Override
    public Page<?> getHostOrders(String username, int page, int size, String status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (status != null && !status.isEmpty()) {
            return orderRepository.findByHostIdAndStatus(user.getId(), status, pageable)
                    .map(order -> convertToOrderDTO(order));
        } else {
            return orderRepository.findByHostId(user.getId(), pageable)
                    .map(order -> convertToOrderDTO(order));
        }
    }

    @Override
    public Page<?> getHostReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findByHostId(user.getId(), pageable)
                .map(review -> convertToReviewDTO(review));
    }

    private HomestayDTO convertToHomestayDTO(Homestay homestay) {
        return HomestayDTO.builder()
                .id(homestay.getId())
                .title(homestay.getTitle())
                .description(homestay.getDescription())
                .price(homestay.getPrice().toString())
                .address(homestay.getAddress())
                .type(homestay.getType())
                .status(homestay.getStatus())
                .build();
    }
    
    private OrderDTO convertToOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(order.getHomestay().getId())
                .homestayTitle(order.getHomestay().getTitle())
                .guestId(order.getGuest().getId())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .guestCount(order.getGuestCount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createTime(order.getCreatedAt())
                .build();
    }
    
    private ReviewDTO convertToReviewDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .homestayId(review.getHomestay().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .createTime(review.getCreateTime())
                .build();
    }
    
    @Override
    public HostDTO getHomestayHostInfo(Long homestayId) {
        // 根据房源ID查询房源信息
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        // 获取房源所有者ID
        Long ownerId = homestay.getOwner().getId();
        
        // 根据房东ID查询用户信息
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("房东不存在，ID: " + ownerId));
        
        // 创建DTO对象并返回
        HostDTO hostDTO = HostDTO.fromUser(owner);
        
        // 解析languages字段
        if (owner.getLanguages() != null && !owner.getLanguages().isEmpty()) {
            try {
                List<String> languages = objectMapper.readValue(owner.getLanguages(), new TypeReference<List<String>>() {});
                hostDTO.setLanguages(languages);
            } catch (JsonProcessingException e) {
                logger.error("解析语言字段失败", e);
                hostDTO.setLanguages(new ArrayList<>());
            }
        } else {
            hostDTO.setLanguages(new ArrayList<>());
        }
        
        // 解析companions字段
        if (owner.getCompanions() != null && !owner.getCompanions().isEmpty()) {
            try {
                List<Map<String, String>> companions = objectMapper.readValue(owner.getCompanions(), 
                        new TypeReference<List<Map<String, String>>>() {});
                hostDTO.setCompanions(companions);
            } catch (JsonProcessingException e) {
                logger.error("解析接待伙伴字段失败", e);
                hostDTO.setCompanions(new ArrayList<>());
            }
        } else {
            hostDTO.setCompanions(new ArrayList<>());
        }
        
        // 填充统计数据
        Long homestayCount = homestayRepository.countByOwnerId(ownerId);
        Long orderCount = orderRepository.countByHostId(ownerId);
        Long reviewCount = reviewRepository.countByHostId(ownerId);
        Double avgRating = reviewRepository.getAverageRatingByHostId(ownerId);
        
        hostDTO.setHomestayCount(homestayCount != null ? homestayCount.intValue() : 0);
        hostDTO.setOrderCount(orderCount != null ? orderCount.intValue() : 0);
        hostDTO.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);
        hostDTO.setRating(avgRating != null ? avgRating : 0.0);
        
        // 返回结果
        return hostDTO;
    }
} 