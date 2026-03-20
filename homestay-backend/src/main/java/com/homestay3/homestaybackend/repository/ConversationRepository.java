package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.Conversation;
import com.homestay3.homestaybackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.host.id = :userId OR c.guest.id = :userId ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId, Pageable pageable);

    Optional<Conversation> findByHomestayIdAndHostIdAndGuestId(Long homestayId, Long hostId, Long guestId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE (c.host.id = :userId AND c.unreadHostCount > 0) OR (c.guest.id = :userId AND c.unreadGuestCount > 0)")
    long countUnreadByUserId(@Param("userId") Long userId);
}
