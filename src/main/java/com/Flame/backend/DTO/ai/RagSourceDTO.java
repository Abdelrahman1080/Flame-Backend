package com.Flame.backend.DTO.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSourceDTO {
    private String type;
    private Long id;
    private String title;
    private String reason;
}
