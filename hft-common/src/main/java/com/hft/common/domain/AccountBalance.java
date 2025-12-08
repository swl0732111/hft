package com.hft.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("account_balances")
public class AccountBalance {
    @Id
    private String id;
    private String accountId;
    private String asset;
    @Builder.Default
    private AccountType type = AccountType.SPOT;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;
}
