package com.hft.wallet.service.deposit;

import com.hft.wallet.domain.CustodialWallet;
import com.hft.wallet.domain.WalletConnection;
import com.hft.wallet.repository.CustodialWalletRepository;
import com.hft.wallet.repository.WalletConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DepositListenerTest {

    @Mock
    private CustodialWalletRepository custodialWalletRepository;
    @Mock
    private WalletConnectionRepository walletConnectionRepository;
    @Mock
    private DepositService depositService;

    private CustodialDepositListener custodialListener;
    private NonCustodialDepositListener nonCustodialListener;

    private final String HOT_WALLET_ADDRESS = "0xHotWallet";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        custodialListener = new CustodialDepositListener(custodialWalletRepository, depositService);

        nonCustodialListener = new NonCustodialDepositListener(walletConnectionRepository, depositService);

        // Inject hot wallet address via reflection
        java.lang.reflect.Field field = NonCustodialDepositListener.class.getDeclaredField("platformHotWalletAddress");
        field.setAccessible(true);
        field.set(nonCustodialListener, HOT_WALLET_ADDRESS);
    }

    @Test
    void custodialListener_ShouldDetectDeposit() {
        String txHash = "0x123";
        String from = "0xExternalUser";
        String to = "0xCustodialAddress";
        BigDecimal amount = new BigDecimal("100");
        String asset = "USDT";
        String chain = "ETH";

        CustodialWallet wallet = CustodialWallet.builder()
                .accountId("user1")
                .walletAddress(to)
                .build();

        when(custodialWalletRepository.findByWalletAddress(to)).thenReturn(Optional.of(wallet));

        custodialListener.processTransaction(txHash, from, to, amount, asset, chain);

        verify(depositService).processDeposit("user1", amount, asset, txHash, "CUSTODIAL");
    }

    @Test
    void nonCustodialListener_ShouldDetectDeposit() {
        String txHash = "0x456";
        String from = "0xUserWeb3Wallet";
        String to = HOT_WALLET_ADDRESS;
        BigDecimal amount = new BigDecimal("50");
        String asset = "USDC";
        String chain = "ETH";

        WalletConnection connection = WalletConnection.builder()
                .accountId("user2")
                .walletAddress(from)
                .chain(chain)
                .build();

        when(walletConnectionRepository.findByWalletAddressAndChain(from, chain)).thenReturn(Optional.of(connection));

        nonCustodialListener.processTransaction(txHash, from, to, amount, asset, chain);

        verify(depositService).processDeposit("user2", amount, asset, txHash, "NON_CUSTODIAL");
    }

    @Test
    void nonCustodialListener_ShouldIgnoreUnknownSender() {
        String txHash = "0x789";
        String from = "0xUnknownUser";
        String to = HOT_WALLET_ADDRESS;

        when(walletConnectionRepository.findByWalletAddressAndChain(anyString(), anyString()))
                .thenReturn(Optional.empty());

        nonCustodialListener.processTransaction(txHash, from, to, BigDecimal.TEN, "ETH", "ETH");

        verify(depositService, never()).processDeposit(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void nonCustodialListener_ShouldIgnoreOtherTransactions() {
        String to = "0xSomeOtherAddress";

        nonCustodialListener.processTransaction("0x000", "0xSender", to, BigDecimal.TEN, "ETH", "ETH");

        verify(walletConnectionRepository, never()).findByWalletAddressAndChain(anyString(), anyString());
    }
}
