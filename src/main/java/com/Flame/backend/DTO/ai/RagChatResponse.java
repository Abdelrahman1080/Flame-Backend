package com.Flame.backend.DTO.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatResponse {
    private String answer;
    private List<RagSourceDTO> sources;
    private String conversationId;
}
