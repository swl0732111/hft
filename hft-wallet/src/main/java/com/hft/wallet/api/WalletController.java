package com.hft.wallet.api;

import com.hft.wallet.domain.WalletConnection;
import com.hft.wallet.service.JwtService;
import com.hft.wallet.service.Web3WalletService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for Web3 wallet operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final Web3WalletService walletService;
    private final JwtService jwtService;

    /**
     * Get nonce for wallet signature
     */
    @GetMapping("/nonce/{address}")
    public ResponseEntity<NonceResponse> getNonce(@PathVariable String address) {
        try {
            String nonce = walletService.generateNonce(address);
            return ResponseEntity.ok(new NonceResponse(nonce, address));
        } catch (Exception e) {
            log.error("Error generating nonce", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verify wallet signature
     */
    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifySignature(@RequestBody VerifyRequest request) {
        try {
            boolean isValid = walletService.verifySignature(
                    request.getWalletAddress(),
                    request.getSignature(),
                    request.getNonce());

            if (isValid) {
                String token = jwtService.generateToken(request.getWalletAddress());
                return ResponseEntity.ok(new VerifyResponse(true, "Signature verified successfully", token));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new VerifyResponse(false, "Invalid signature", null));
            }
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new VerifyResponse(false, "Verification failed", null));
        }
    }

    /**
     * Link wallet to account
     */
    @PostMapping("/link")
    public ResponseEntity<WalletConnection> linkWallet(@RequestBody LinkWalletRequest request) {
        try {
            // First verify signature
            boolean isValid = walletService.verifySignature(
                    request.getWalletAddress(),
                    request.getSignature(),
                    request.getNonce());

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Link wallet to account
            WalletConnection connection = walletService.linkWallet(
                    request.getAccountId(),
                    request.getWalletAddress(),
                    request.getChain(),
                    request.getChainId());

            return ResponseEntity.ok(connection);
        } catch (Exception e) {
            log.error("Error linking wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get wallet connections for account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<WalletConnection>> getAccountWallets(@PathVariable String accountId) {
        try {
            List<WalletConnection> wallets = walletService.getAccountWallets(accountId);
            return ResponseEntity.ok(wallets);
        } catch (Exception e) {
            log.error("Error fetching account wallets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Disconnect wallet
     */
    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnectWallet(@RequestBody DisconnectRequest request) {
        try {
            walletService.disconnectWallet(request.getWalletAddress(), request.getChain());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error disconnecting wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NonceResponse {
        private String nonce;
        private String walletAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        private String walletAddress;
        private String signature;
        private String nonce;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyResponse {
        private boolean valid;
        private String message;
        private String token;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkWalletRequest {
        private String accountId;
        private String walletAddress;
        private String signature;
        private String nonce;
        private String chain;
        private Long chainId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisconnectRequest {
        private String walletAddress;
        private String chain;
    }
}
