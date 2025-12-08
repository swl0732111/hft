package com.hft.wallet.service;

import com.hft.wallet.domain.CustodialWallet;
import com.hft.wallet.repository.CustodialWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustodialWalletServiceTest {

    @Mock
    private CustodialWalletRepository custodialWalletRepository;

    private EncryptionService encryptionService;
    private CustodialWalletService custodialWalletService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use real encryption service for testing
        encryptionService = new EncryptionService();
        // Inject secret key via reflection or just rely on default if possible.
        // Since EncryptionService uses @Value, we need to set it manually for unit test
        // if not using Spring context.
        java.lang.reflect.Field secretKeyField = EncryptionService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(encryptionService, "test-secret-key-1234567890123456");

        custodialWalletService = new CustodialWalletService(custodialWalletRepository, encryptionService);
    }

    @Test
    void getDepositAddress_ShouldReturnExistingAddress() {
        String accountId = "user1";
        String chain = "ETH";
        String existingAddress = "0x123";

        CustodialWallet existingWallet = CustodialWallet.builder()
                .walletAddress(existingAddress)
                .build();

        when(custodialWalletRepository.findByAccountIdAndChain(accountId, chain))
                .thenReturn(Optional.of(existingWallet));

        String address = custodialWalletService.getDepositAddress(accountId, chain);

        assertEquals(existingAddress, address);
        verify(custodialWalletRepository, never()).save(any());
    }

    @Test
    void getDepositAddress_ShouldCreateNewWallet() {
        String accountId = "user2";
        String chain = "ETH";

        when(custodialWalletRepository.findByAccountIdAndChain(accountId, chain))
                .thenReturn(Optional.empty());

        when(custodialWalletRepository.save(any(CustodialWallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String address = custodialWalletService.getDepositAddress(accountId, chain);

        assertNotNull(address);
        assertTrue(address.startsWith("0x"));
        verify(custodialWalletRepository).save(any(CustodialWallet.class));
    }

    @Test
    void encryption_ShouldEncryptAndDecryptCorrectly() {
        String originalData = "my-secret-data";
        String encrypted = encryptionService.encrypt(originalData);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotEquals(originalData, encrypted);
        assertEquals(originalData, decrypted);
    }
}
