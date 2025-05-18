package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.PasswordChangeRequest;
import com.homestay3.homestaybackend.dto.ProfileUpdateRequest;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.homestay3.homestaybackend.dto.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    /**
     * 获取所有用户
     */
    List<UserDTO> getAllUsers();
    
    /**
     * 根据ID获取用户
     */
    UserDTO getUserById(Long id);
    
    /**
     * 根据用户名获取用户
     */
    UserDTO getUserByUsername(String username);
    
    /**
     * 更新用户信息
     */
    UserDTO updateUser(Long id, UserDTO userDTO);
    
    /**
     * 删除用户
     */
    void deleteUser(Long id);
    
    /**
     * 上传头像
     */
    String uploadAvatar(MultipartFile file, String username);
    
    /**
     * 更新用户角色
     */
    UserDTO updateUserRole(Long id, String role);
    
    /**
     * 启用/禁用用户
     */
    UserDTO toggleUserStatus(Long id, boolean enabled);
    
    /**
     * 将User实体转换为UserDTO
     */
    UserDTO convertToDTO(User user);
    
    /**
     * 更新用户头像
     */
    void updateAvatar(String username, String avatarUrl);
    
    /**
     * 修改密码
     */
    void changePassword(PasswordChangeRequest request, String username);

    @Transactional
    AuthResponse updateProfile(ProfileUpdateRequest request, String currentUsername);
    
    /**
     * 管理员获取用户列表（分页和筛选）
     */
    Page<UserDTO> getAdminUsers(Pageable pageable, String username, String email, String role);
    
    /**
     * 管理员创建用户
     */
    UserDTO createUser(UserDTO userDTO);
    
    /**
     * 管理员更新用户状态
     */
    void updateUserStatus(Long id, boolean enabled);
    
    /**
     * 管理员重置用户密码
     */
    String resetUserPassword(Long id);
}