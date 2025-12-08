package com.hft.account.repository;

import com.hft.common.domain.AccountBalance;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBalanceRepository extends CrudRepository<AccountBalance, String> {
        List<AccountBalance> findByAccountId(String accountId);

        Optional<AccountBalance> findByAccountIdAndAssetAndType(String accountId, String asset,
                        com.hft.common.domain.AccountType type);

        @org.springframework.data.jdbc.repository.query.Query("SELECT SUM(available_balance) FROM account_balances WHERE asset = :asset")
        java.math.BigDecimal sumAvailableBalanceByAsset(
                        @org.springframework.data.repository.query.Param("asset") String asset);

        @org.springframework.data.jdbc.repository.query.Query("SELECT SUM(locked_balance) FROM account_balances WHERE asset = :asset")
        java.math.BigDecimal sumLockedBalanceByAsset(
                        @org.springframework.data.repository.query.Param("asset") String asset);
}
