package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    
    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        
        // 获取房源信息
        Homestay homestay = homestayRepository.findById(orderDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        
        // 检查日期是否有效
        if (orderDTO.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }
        if (orderDTO.getCheckOutDate().isBefore(orderDTO.getCheckInDate())) {
            throw new IllegalArgumentException("退房日期不能早于入住日期");
        }
        
        // 计算住宿天数
        long nights = ChronoUnit.DAYS.between(orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }
        
        // 检查是否有重叠的预订
        boolean hasOverlap = orderRepository.existsOverlappingBooking(
                homestay.getId(), orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (hasOverlap) {
            throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
        }
        
        // 计算总价
        BigDecimal totalAmount = homestay.getPrice().multiply(BigDecimal.valueOf(nights));
        
        // 创建订单
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .homestay(homestay)
                .guest(currentUser)
                .guestPhone(orderDTO.getGuestPhone())
                .checkInDate(orderDTO.getCheckInDate())
                .checkOutDate(orderDTO.getCheckOutDate())
                .nights((int) nights)
                .guestCount(orderDTO.getGuestCount())
                .price(homestay.getPrice())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING.name())
                .remark(orderDTO.getRemark())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查权限
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new IllegalArgumentException("您无权访问此订单");
        }
        
        return convertToDTO(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查权限
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new IllegalArgumentException("您无权访问此订单");
        }
        
        return convertToDTO(order);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(Map<String, String> params, Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // 创建查询条件
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 当前用户作为客人
            predicates.add(cb.equal(root.get("guest"), currentUser));
            
            // 按状态筛选
            if (params.containsKey("status")) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOwnerOrders(String ownerUsername, Map<String, String> params, Pageable pageable) {
        // 获取房东用户
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 创建查询条件
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 关联房源表，筛选出指定房东的订单
            Join<Order, Homestay> homestayJoin = root.join("homestay");
            predicates.add(cb.equal(homestayJoin.get("owner"), owner));
            
            // 按状态筛选
            if (params.containsKey("status")) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }
            
            // 按房源ID筛选
            if (params.containsKey("homestayId")) {
                predicates.add(cb.equal(homestayJoin.get("id"), 
                        Long.parseLong(params.get("homestayId"))));
            }
            
            // 按日期范围筛选
            if (params.containsKey("startDate") && params.containsKey("endDate")) {
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                LocalDate endDate = LocalDate.parse(params.get("endDate"));
                
                predicates.add(cb.or(
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("checkInDate"), startDate),
                                cb.lessThanOrEqualTo(root.get("checkInDate"), endDate)
                        ),
                        cb.and(
                                cb.greaterThanOrEqualTo(root.get("checkOutDate"), startDate),
                                cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate)
                        )
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getPendingOrderCount(String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        return orderRepository.countPendingOrdersByOwner(owner);
    }
    
    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查当前用户是否为房东
        User currentUser = getCurrentUser();
        if (!isOrderOwner(order, currentUser)) {
            throw new IllegalArgumentException("您无权更新此订单状态");
        }
        
        // 验证状态转换的合法性
        validateStatusTransition(order.getStatus(), status);
        
        // 如果是接受订单，设置状态为已确认
        if (status.equals(OrderStatus.CONFIRMED.name())) {
            // 再次检查是否有重叠的预订
            boolean hasOverlap = orderRepository.existsOverlappingBooking(
                    order.getHomestay().getId(), order.getCheckInDate(), order.getCheckOutDate());
            if (hasOverlap) {
                throw new IllegalArgumentException("所选日期已被其他订单预订，无法确认此订单");
            }
        }
        
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        return convertToDTO(updatedOrder);
    }
    
    @Override
    @Transactional
    public OrderDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查是否有权取消订单（房客或房东）
        User currentUser = getCurrentUser();
        if (!isOrderAccessible(order, currentUser)) {
            throw new IllegalArgumentException("您无权取消此订单");
        }
        
        // 只有待确认或已确认的订单可以取消
        if (!order.getStatus().equals(OrderStatus.PENDING.name()) && 
            !order.getStatus().equals(OrderStatus.CONFIRMED.name())) {
            throw new IllegalArgumentException("该订单当前状态无法取消");
        }
        
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setUpdatedAt(LocalDateTime.now());
        Order cancelledOrder = orderRepository.save(order);
        
        return convertToDTO(cancelledOrder);
    }
    
    @Override
    @Transactional
    public OrderDTO payOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查是否为订单的客人
        User currentUser = getCurrentUser();
        if (!order.getGuest().equals(currentUser)) {
            throw new IllegalArgumentException("您无权支付此订单");
        }
        
        // 只有已确认的订单可以支付
        if (!order.getStatus().equals(OrderStatus.CONFIRMED.name())) {
            throw new IllegalArgumentException("该订单当前状态无法支付");
        }
        
        order.setStatus(OrderStatus.PAID.name());
        order.setUpdatedAt(LocalDateTime.now());
        Order paidOrder = orderRepository.save(order);
        
        return convertToDTO(paidOrder);
    }
    
    @Override
    public OrderDTO createOrderPreview(OrderDTO orderDTO) {
        // 获取房源信息
        Homestay homestay = homestayRepository.findById(orderDTO.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        
        // 获取当前用户信息
        User currentUser = getCurrentUser();
        
        // 检查日期是否有效
        if (orderDTO.getCheckInDate() == null || orderDTO.getCheckOutDate() == null) {
            throw new IllegalArgumentException("入住和退房日期不能为空");
        }
        
        if (orderDTO.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("入住日期不能早于今天");
        }
        
        if (orderDTO.getCheckOutDate().isBefore(orderDTO.getCheckInDate())) {
            throw new IllegalArgumentException("退房日期不能早于入住日期");
        }
        
        // 计算住宿天数
        long nights = ChronoUnit.DAYS.between(orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }
        
        // 检查是否有重叠的预订
        boolean hasOverlap = orderRepository.existsOverlappingBooking(
                homestay.getId(), orderDTO.getCheckInDate(), orderDTO.getCheckOutDate());
        if (hasOverlap) {
            throw new IllegalArgumentException("所选日期已被预订，请选择其他日期");
        }
        
        // 计算价格信息
        BigDecimal pricePerNight = homestay.getPrice();
        BigDecimal baseAmount = pricePerNight.multiply(BigDecimal.valueOf(nights));
        
        // 假设清洁费为每晚价格的10%
        BigDecimal cleaningFee = pricePerNight.multiply(BigDecimal.valueOf(0.1));
        
        // 假设服务费为总价的15%
        BigDecimal serviceFee = baseAmount.multiply(BigDecimal.valueOf(0.15));
        
        // 计算总价
        BigDecimal totalAmount = baseAmount.add(cleaningFee).add(serviceFee);
        
        // 创建预览订单DTO对象
        OrderDTO previewOrderDTO = OrderDTO.builder()
                .homestayId(homestay.getId())
                .homestayTitle(homestay.getTitle())
                .guestId(currentUser.getId())
                .guestName(currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername())
                .guestPhone(orderDTO.getGuestPhone())
                .checkInDate(orderDTO.getCheckInDate())
                .checkOutDate(orderDTO.getCheckOutDate())
                .nights((int) nights)
                .guestCount(orderDTO.getGuestCount())
                .price(pricePerNight)
                .totalAmount(totalAmount)
                .remark(orderDTO.getRemark())
                .build();
        
        // 添加额外信息，用于前端显示
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("baseAmount", baseAmount);
        additionalInfo.put("cleaningFee", cleaningFee);
        additionalInfo.put("serviceFee", serviceFee);
        additionalInfo.put("pricePerNight", pricePerNight);
        additionalInfo.put("imageUrl", homestay.getCoverImage());
        additionalInfo.put("address", homestay.getAddress());
        additionalInfo.put("province", homestay.getProvince());
        additionalInfo.put("city", homestay.getCity());
        additionalInfo.put("district", homestay.getDistrict());
        
        // 这里我们不能直接在OrderDTO添加additionalInfo字段，
        // 因为它没有定义这个字段，我们需要修改前端代码来处理这些额外信息
        // 或者扩展OrderDTO类。这里为了简单，我们返回基本信息。
        
        return previewOrderDTO;
    }
    
    @Override
    public OrderDTO rejectOrder(Long id, String reason) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 检查是否为房东
        if (!isOrderOwner(order, currentUser)) {
            throw new IllegalArgumentException("无权操作此订单");
        }
        
        // 检查订单状态是否为待确认
        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("只能拒绝待确认状态的订单");
        }
        
        // 更新订单状态
        order.setStatus(OrderStatus.REJECTED.name());
        order.setRemark((order.getRemark() != null ? order.getRemark() + "\n" : "") + "拒绝原因: " + reason);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        return convertToDTO(updatedOrder);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAdminOrders(Pageable pageable, Map<String, String> params) {
        // 创建查询条件
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 按订单号筛选
            if (params.containsKey("orderNumber") && !params.get("orderNumber").isEmpty()) {
                predicates.add(cb.like(root.get("orderNumber"), "%" + params.get("orderNumber") + "%"));
            }
            
            // 按状态筛选
            if (params.containsKey("status") && !params.get("status").isEmpty()) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }
            
            // 按日期范围筛选
            if (params.containsKey("startDate") && !params.get("startDate").isEmpty()) {
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), startDate));
            }
            
            if (params.containsKey("endDate") && !params.get("endDate").isEmpty()) {
                LocalDate endDate = LocalDate.parse(params.get("endDate"));
                predicates.add(cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return orderRepository.findAll(spec, pageable).map(this::convertToDTO);
    }
    
    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + id));
        
        // 检查订单状态，只允许删除已完成或已取消的订单
        if (!Arrays.asList("COMPLETED", "CANCELLED", "REJECTED").contains(order.getStatus())) {
            throw new IllegalStateException("只能删除已完成、已取消或已拒绝的订单");
        }
        
        orderRepository.delete(order);
    }
    
    @Override
    public byte[] exportOrders(Map<String, String> params) {
        // 获取符合条件的所有订单
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 按订单号筛选
            if (params.containsKey("orderNumber") && !params.get("orderNumber").isEmpty()) {
                predicates.add(cb.like(root.get("orderNumber"), "%" + params.get("orderNumber") + "%"));
            }
            
            // 按状态筛选
            if (params.containsKey("status") && !params.get("status").isEmpty()) {
                predicates.add(cb.equal(root.get("status"), params.get("status")));
            }
            
            // 按日期范围筛选
            if (params.containsKey("startDate") && !params.get("startDate").isEmpty()) {
                LocalDate startDate = LocalDate.parse(params.get("startDate"));
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInDate"), startDate));
            }
            
            if (params.containsKey("endDate") && !params.get("endDate").isEmpty()) {
                LocalDate endDate = LocalDate.parse(params.get("endDate"));
                predicates.add(cb.lessThanOrEqualTo(root.get("checkOutDate"), endDate));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        List<Order> orders = orderRepository.findAll(spec);
        
        // 创建导出数据（使用简单的CSV格式）
        try {
            StringBuilder csv = new StringBuilder();
            // 添加CSV头
            csv.append("订单编号,房源名称,入住日期,退房日期,晚数,房客姓名,房客电话,总金额,状态,创建时间\n");
            
            // 添加订单数据
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Order order : orders) {
                csv.append(order.getOrderNumber()).append(",");
                csv.append(order.getHomestay().getTitle()).append(",");
                csv.append(order.getCheckInDate().format(dateFormatter)).append(",");
                csv.append(order.getCheckOutDate().format(dateFormatter)).append(",");
                csv.append(order.getNights()).append(",");
                csv.append(order.getGuest().getFullName()).append(",");
                csv.append(order.getGuestPhone()).append(",");
                csv.append(order.getTotalAmount()).append(",");
                csv.append(order.getStatus()).append(",");
                csv.append(order.getCreatedAt().format(dateTimeFormatter)).append("\n");
            }
            
            return csv.toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("导出订单失败", e);
        }
    }
    
    // 工具方法：获取当前登录用户
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("未找到当前登录用户"));
    }
    
    // 工具方法：生成订单号
    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDate.now().format(formatter);
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "HS" + datePart + randomPart;
    }
    
    // 工具方法：验证状态转换的合法性
    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(OrderStatus.PENDING.name())) {
            // 待确认订单可以转为已确认、已拒绝或已取消
            if (!newStatus.equals(OrderStatus.CONFIRMED.name()) && 
                !newStatus.equals(OrderStatus.REJECTED.name()) && 
                !newStatus.equals(OrderStatus.CANCELLED.name())) {
                throw new IllegalArgumentException("待确认订单只能转为已确认、已拒绝或已取消状态");
            }
        } else if (currentStatus.equals(OrderStatus.CONFIRMED.name())) {
            // 已确认订单可以转为已支付或已取消
            if (!newStatus.equals(OrderStatus.PAID.name()) && 
                !newStatus.equals(OrderStatus.CANCELLED.name())) {
                throw new IllegalArgumentException("已确认订单只能转为已支付或已取消状态");
            }
        } else if (currentStatus.equals(OrderStatus.PAID.name())) {
            // 已支付订单可以转为已入住或已取消
            if (!newStatus.equals(OrderStatus.CHECKED_IN.name()) && 
                !newStatus.equals(OrderStatus.CANCELLED.name())) {
                throw new IllegalArgumentException("已支付订单只能转为已入住或已取消状态");
            }
        } else if (currentStatus.equals(OrderStatus.CHECKED_IN.name())) {
            // 已入住订单只能转为已完成
            if (!newStatus.equals(OrderStatus.COMPLETED.name())) {
                throw new IllegalArgumentException("已入住订单只能转为已完成状态");
            }
        } else if (currentStatus.equals(OrderStatus.COMPLETED.name()) || 
                   currentStatus.equals(OrderStatus.CANCELLED.name()) ||
                   currentStatus.equals(OrderStatus.REJECTED.name())) {
            // 已完成、已取消、已拒绝订单不能更改状态
            throw new IllegalArgumentException("此订单状态不可更改");
        }
    }
    
    // 工具方法：检查用户是否为订单的房东
    private boolean isOrderOwner(Order order, User user) {
        return order.getHomestay().getOwner().equals(user);
    }
    
    // 工具方法：检查用户是否有权访问订单（房客或房东）
    private boolean isOrderAccessible(Order order, User user) {
        return order.getGuest().equals(user) || order.getHomestay().getOwner().equals(user);
    }
    
    // 工具方法：将 Order 实体转换为 OrderDTO
    private OrderDTO convertToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(order.getHomestay().getId())
                .homestayTitle(order.getHomestay().getTitle())
                .guestId(order.getGuest().getId())
                .guestName(order.getGuest().getUsername())
                .guestPhone(order.getGuestPhone())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .guestCount(order.getGuestCount())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .remark(order.getRemark())
                .createTime(order.getCreatedAt())
                .updateTime(order.getUpdatedAt())
                .build();
    }
} 