package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import org.springframework.web.multipart.MultipartFile;

public interface HomestayCommandService {

    String uploadHomestayImage(MultipartFile file);

    HomestayDTO createHomestay(HomestayDTO homestayDTO, String ownerUsername);

    HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO);

    void deleteHomestay(Long id);

    HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername);
}
