package edu.itc.shop.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class MeController {
    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("name", auth.getName(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
