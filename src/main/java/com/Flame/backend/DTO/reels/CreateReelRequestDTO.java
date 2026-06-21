package com.Flame.backend.DTO.reels;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateReelRequestDTO {

    private MultipartFile video;

    private String caption;

    private String preferences;
}
