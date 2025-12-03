package com.hft.wallet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ChainIntegrationService {

    private final Web3j web3j;

    public ChainIntegrationService(@Value("${web3.rpc-url:https://eth.public-rpc.com}") String rpcUrl) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        log.info("Connected to Ethereum RPC: {}", rpcUrl);
    }

    /**
     * Get ETH balance for address
     */
    public BigDecimal getBalance(String address) {
        try {
            EthGetBalance ethBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigInteger wei = ethBalance.getBalance();
            return Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
        } catch (Exception e) {
            log.error("Error fetching balance for {}", address, e);
            throw new RuntimeException("Failed to fetch balance", e);
        }
    }

    /**
     * Submit withdrawal transaction (Mock implementation for demo)
     * In a real system, this would sign and send a transaction using a hot wallet.
     */
    public CompletableFuture<String> submitWithdrawal(String toAddress, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing withdrawal of {} ETH to {}", amount, toAddress);

            // Simulate transaction delay
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Return a mock transaction hash
            String txHash = "0x" + Long.toHexString(System.nanoTime()) + Long.toHexString(System.currentTimeMillis());
            log.info("Withdrawal submitted. TxHash: {}", txHash);
            return txHash;
        });
    }
}
