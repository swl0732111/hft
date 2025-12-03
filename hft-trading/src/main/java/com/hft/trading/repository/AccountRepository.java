package com.hft.trading.repository;

import com.hft.common.domain.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends CrudRepository<Account, String> {
    Optional<Account> findByUserId(String userId);

    Optional<Account> findByUsername(String username);
}
