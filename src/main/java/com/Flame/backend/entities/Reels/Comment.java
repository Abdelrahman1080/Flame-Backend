package com.Flame.backend.entities.Reels;

import com.Flame.backend.entities.user.Customer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;


    @ManyToOne
    private Customer user;

    @ManyToOne
    private Reel reel;
}