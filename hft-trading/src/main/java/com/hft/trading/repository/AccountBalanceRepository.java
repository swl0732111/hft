package com.hft.trading.repository;

import com.hft.common.domain.AccountBalance;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBalanceRepository extends CrudRepository<AccountBalance, String> {
    List<AccountBalance> findByAccountId(String accountId);

    Optional<AccountBalance> findByAccountIdAndAsset(String accountId, String asset);
}
