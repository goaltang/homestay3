package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnnouncementService {

    /**
     * 管理员接口
     */

    /**
     * 创建公告
     */
    Announcement createAnnouncement(Announcement announcement, Long publisherId, String publisherName);

    /**
     * 更新公告
     */
    Announcement updateAnnouncement(Long id, Announcement announcement);

    /**
     * 删除公告（软删除）
     */
    void deleteAnnouncement(Long id);

    /**
     * 获取所有公告（分页）
     */
    Page<Announcement> getAllAnnouncements(Pageable pageable);

    /**
     * 按状态获取公告
     */
    Page<Announcement> getAnnouncementsByStatus(String status, Pageable pageable);

    /**
     * 发布公告
     */
    Announcement publishAnnouncement(Long id, Long publisherId, String publisherName);

    /**
     * 下线公告
     */
    Announcement offlineAnnouncement(Long id);

    /**
     * 根据ID获取公告
     */
    Announcement getById(Long id);

    /**
     * 用户端接口
     */

    /**
     * 获取已发布的公告
     */
    Page<Announcement> getPublishedAnnouncements(String category, Pageable pageable);

    /**
     * 用户端根据ID获取已发布公告
     */
    Announcement getPublishedById(Long id);
}
