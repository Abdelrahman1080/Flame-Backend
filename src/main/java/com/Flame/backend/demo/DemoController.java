package com.Flame.backend.demo;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
public class DemoController {
    @GetMapping("/get")
    public String test() {
        return "test";
    }

    @GetMapping("/whoami")
    public String whoami(Authentication authentication) {
        if (authentication == null) return "not authenticated";
        String name = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        return String.format("principal=%s, authorities=[%s]", name, authorities);
    }
}
