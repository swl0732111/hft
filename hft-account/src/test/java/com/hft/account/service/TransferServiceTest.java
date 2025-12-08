package com.hft.account.service;

import com.hft.common.domain.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    @Mock
    private AccountService accountService;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transferService = new TransferService(accountService);
    }

    @Test
    void transfer_ShouldSucceed_WhenBalanceIsSufficient() {
        String accountId = "user1";
        String asset = "USDT";
        BigDecimal amount = new BigDecimal("100");

        transferService.transfer(accountId, asset, AccountType.SPOT, AccountType.FUTURES, amount);

        verify(accountService).debitAvailableBalance(accountId, asset, AccountType.SPOT, amount);
        verify(accountService).creditBalance(accountId, asset, AccountType.FUTURES, amount);
    }

    @Test
    void transfer_ShouldFail_WhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> transferService.transfer("user1", "USDT", AccountType.SPOT,
                AccountType.FUTURES, new BigDecimal("-10")));
    }

    @Test
    void transfer_ShouldFail_WhenTypesAreSame() {
        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer("user1", "USDT", AccountType.SPOT, AccountType.SPOT, BigDecimal.TEN));
    }
}
