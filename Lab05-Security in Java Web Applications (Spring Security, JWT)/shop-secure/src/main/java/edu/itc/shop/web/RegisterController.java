package edu.itc.shop.web;

import edu.itc.shop.domain.Role;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/register")
public class RegisterController {
    private final AccountService accounts;

    public RegisterController(AccountService accounts) { this.accounts = accounts; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest req) {
        var acc = accounts.register(req.username(), req.password(), Set.of(Role.CUSTOMER));
        return Map.of("username", acc.getUsername());
    }
}
