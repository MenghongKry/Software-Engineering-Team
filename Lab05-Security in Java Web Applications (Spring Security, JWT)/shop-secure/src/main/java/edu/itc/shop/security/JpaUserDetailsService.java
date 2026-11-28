package edu.itc.shop.security;

import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public JpaUserDetailsService(UserAccountRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var acc = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(acc.getUsername())
                .password(acc.getPasswordHash())
                .roles(acc.getRoles().stream().map(Role::name).toArray(String[]::new))
                .disabled(!acc.isEnabled())
                .build();
    }
}
