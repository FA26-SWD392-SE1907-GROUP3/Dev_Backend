package com.example.swd392_se1907_aives.modules.identity.internal.service;
import com.example.swd392_se1907_aives.modules.identity.api.Account;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class IdentityDirectoryService implements IdentityDirectory {
    private final UserRepository users;
    @Override public Account current() {
        Object principal=SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Account account ? account : view((User)principal);
    }
    @Override public Optional<Account> findById(UUID id) { return users.findById(id).map(this::view); }
    @Override public Optional<Account> findByEmailIgnoreCase(String email) { return users.findByEmailIgnoreCase(email).map(this::view); }
    @Override public List<Account> findAllById(Iterable<UUID> ids) { return users.findAllById(ids).stream().map(this::view).toList(); }
    private Account view(User user) { return new Account(user.getId(),user.getFullName(),user.getEmail(),user.getRole(),user.getIsActive(),user.getCreatedAt()); }
}
