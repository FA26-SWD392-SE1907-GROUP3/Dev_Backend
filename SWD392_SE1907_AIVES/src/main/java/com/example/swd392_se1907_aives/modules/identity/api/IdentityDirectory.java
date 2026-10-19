package com.example.swd392_se1907_aives.modules.identity.api;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only account directory. Account creation and credentials remain inside Identity. */
public interface IdentityDirectory {
    Account current();
    Optional<Account> findById(UUID id);
    Optional<Account> findByEmailIgnoreCase(String email);
    List<Account> findAllById(Iterable<UUID> ids);
}
