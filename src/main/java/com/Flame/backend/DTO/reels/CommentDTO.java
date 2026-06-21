package com.Flame.backend.DTO.reels;

import com.Flame.backend.DTO.customer.CustomerLiteDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long id;

    private String content;

    private CustomerLiteDTO user;

}
