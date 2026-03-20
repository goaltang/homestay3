package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Announcement;
import com.homestay3.homestaybackend.repository.AnnouncementRepository;
import com.homestay3.homestaybackend.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Override
    public Announcement createAnnouncement(Announcement announcement, Long publisherId, String publisherName) {
        announcement.setPublisherId(publisherId);
        announcement.setPublisherName(publisherName);
        return announcementRepository.save(announcement);
    }

    @Override
    public Announcement updateAnnouncement(Long id, Announcement announcement) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setCategory(announcement.getCategory());
        existing.setPriority(announcement.getPriority());
        existing.setStartTime(announcement.getStartTime());
        existing.setEndTime(announcement.getEndTime());

        return announcementRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        existing.setIsDeleted(true);
        announcementRepository.save(existing);
    }

    @Override
    public Page<Announcement> getAllAnnouncements(Pageable pageable) {
        return announcementRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<Announcement> getAnnouncementsByStatus(String status, Pageable pageable) {
        return announcementRepository.findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Override
    public Announcement publishAnnouncement(Long id, Long publisherId, String publisherName) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        existing.setStatus("PUBLISHED");
        existing.setPublisherId(publisherId);
        existing.setPublisherName(publisherName);
        existing.setPublishedAt(LocalDateTime.now());

        return announcementRepository.save(existing);
    }

    @Override
    public Announcement offlineAnnouncement(Long id) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        existing.setStatus("OFFLINE");

        return announcementRepository.save(existing);
    }

    @Override
    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("公告不存在"));
    }

    @Override
    public Page<Announcement> getPublishedAnnouncements(String category, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        if (category != null && !category.isEmpty()) {
            return announcementRepository.findActiveByCategory(category, now, pageable);
        }
        return announcementRepository.findActiveAnnouncements(now, pageable);
    }

    @Override
    public Announcement getPublishedById(Long id) {
        return announcementRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new RuntimeException("公告不存在或已下线"));
    }
}
