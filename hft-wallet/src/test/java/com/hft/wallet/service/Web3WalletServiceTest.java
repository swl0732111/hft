package com.hft.wallet.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hft.wallet.domain.WalletNonce;
import com.hft.wallet.repository.WalletConnectionRepository;
import com.hft.wallet.repository.WalletNonceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class Web3WalletServiceTest {

    @Mock
    private WalletConnectionRepository walletConnectionRepository;

    @Mock
    private WalletNonceRepository walletNonceRepository;

    private Web3WalletService web3WalletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        web3WalletService = new Web3WalletService(walletConnectionRepository, walletNonceRepository);
    }

    @Test
    void generateNonce_ShouldCreateNewNonce() {
        String walletAddress = "0x1234567890123456789012345678901234567890";

        when(walletNonceRepository.findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(anyString(), anyLong()))
                .thenReturn(Optional.empty());

        String nonce = web3WalletService.generateNonce(walletAddress);

        assertNotNull(nonce);
        verify(walletNonceRepository).save(any(WalletNonce.class));
    }

    @Test
    void verifySignature_ShouldReturnFalse_WhenNonceNotFound() {
        String walletAddress = "0x1234567890123456789012345678901234567890";
        String signature = "0x...";
        String nonce = "test-nonce";

        when(walletNonceRepository.findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(anyString(), anyLong()))
                .thenReturn(Optional.empty());

        boolean result = web3WalletService.verifySignature(walletAddress, signature, nonce);

        assertFalse(result);
    }

    @Test
    void verifySignature_ShouldReturnFalse_WhenNonceMismatch() {
        String walletAddress = "0x1234567890123456789012345678901234567890";
        String signature = "0x...";
        String nonce = "test-nonce";

        WalletNonce storedNonce = WalletNonce.builder()
                .nonce("different-nonce")
                .build();

        when(walletNonceRepository.findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(anyString(), anyLong()))
                .thenReturn(Optional.of(storedNonce));

        boolean result = web3WalletService.verifySignature(walletAddress, signature, nonce);

        assertFalse(result);
    }

  @Test
  void generateNonce_ShouldSetIsNewToTrue() {
    String walletAddress = "0x1234567890123456789012345678901234567890";

    when(walletNonceRepository.findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(
            anyString(), anyLong()))
        .thenReturn(Optional.empty());

    web3WalletService.generateNonce(walletAddress);

    org.mockito.ArgumentCaptor<WalletNonce> captor =
        org.mockito.ArgumentCaptor.forClass(WalletNonce.class);
    verify(walletNonceRepository).save(captor.capture());

    WalletNonce savedNonce = captor.getValue();
    assertTrue(savedNonce.isNew(), "New nonce should have isNew=true");
  }

  @Test
  void walletNonce_ShouldDefaultIsNewToTrue() {
    WalletNonce nonce = WalletNonce.builder().build();
    assertTrue(nonce.isNew());
  }

  @Test
  void walletConnection_ShouldDefaultIsNewToTrue() {
    com.hft.wallet.domain.WalletConnection connection =
        com.hft.wallet.domain.WalletConnection.builder().build();
    assertTrue(connection.isNew());
  }
}
