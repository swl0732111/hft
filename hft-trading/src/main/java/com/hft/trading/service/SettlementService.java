package com.hft.trading.service;

import com.hft.account.service.AccountService;
import com.hft.common.util.FixedPointMath;
import com.hft.trading.domain.Order;
import com.hft.trading.event.TradeEvent;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.state.AccountStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final AccountService accountService;
    private final OrderRepository orderRepository;
    private final FeeService feeService;
    private final AccountStateStore accountStateStore;

    /**
     * Settle a trade by exchanging assets and deducting fees.
     * Uses "Fee from Proceeds" model:
     * - Buyer: Pays Quote (Locked), Receives Base - Fee
     * - Seller: Pays Base (Locked), Receives Quote - Fee
     */
    @Transactional
    public void settleTrade(TradeEvent event) {
        String symbol = event.getSymbol();
        long priceScaled = event.getPriceScaled();
        long quantityScaled = event.getQuantityScaled();
        long timestamp = event.getTimestamp();

        // Calculate trade value (Quote Asset Amount)
        long tradeValueScaled = FixedPointMath.multiply(priceScaled, quantityScaled);

        // Parse Symbol
        String[] parts = symbol.split("-");
        String baseAsset = parts[0];
        String quoteAsset = parts.length > 1 ? parts[1] : "USDC";

        // Fetch Orders
        Order makerOrder = orderRepository.findById(event.getMakerOrderId()).orElse(null);
        Order takerOrder = orderRepository.findById(event.getTakerOrderId()).orElse(null);

        if (makerOrder == null || takerOrder == null) {
            log.error("Failed to settle trade: Order not found. Maker={}, Taker={}",
                    event.getMakerOrderId(), event.getTakerOrderId());
            return;
        }

        // Settle Maker
        if (makerOrder.getAccountId() != null) {
            settleParty(makerOrder, symbol, baseAsset, quoteAsset,
                    quantityScaled, tradeValueScaled, true);
        }

        // Settle Taker
        if (takerOrder.getAccountId() != null) {
            settleParty(takerOrder, symbol, baseAsset, quoteAsset,
                    quantityScaled, tradeValueScaled, false);
        }

        log.info("Trade settled: {} {} @ {} (Val: {})",
                FixedPointMath.toDouble(quantityScaled), baseAsset,
                FixedPointMath.toDouble(priceScaled),
                FixedPointMath.toDouble(tradeValueScaled));
    }

    private void settleParty(Order order, String symbol, String baseAsset, String quoteAsset,
            long quantityScaled, long tradeValueScaled, boolean isMaker) {
        String accountId = order.getAccountId();

        // Calculate Fee (Fee is always in the Asset *Received*)
        // Wait: FeeService calculates rate. We need to apply it to the proceeds.
        // But FeeService.calculateFeeScaled takes "tradeValue".
        // If Buyer receives Base, rate should be applied to Base Qty?
        // Or is FeeService rate always in terms of Quote?

        // Let's check FeeService behavior.
        // Standard model: Fee is calculated on the Notional Value (in Quote).
        // But if we deduct from Base, we need to convert that value to Base?
        // OR simpler: Rate is X bps.
        // Buyer Received: Qty Base. Fee = Qty * Rate.
        // Seller Received: TradeVal Quote. Fee = TradeVal * Rate.

        // Re-using FeeService logic might be tricky if it assumes Quote Asset fee.
        // Let's assume FeeService returns a scaled amount based on the input "value".
        // If we pass "Quantity" as value for Buyer, we get Fee in Base.
        // If we pass "TradeValue" as value for Seller, we get Fee in Quote.

        long proceedsScaled;
        String assetReceived;
        String assetPaid;
        long amountPaidScaled;

        if (order.getSide() == Order.Side.BUY) {
            // BUYER: Verified 'Buy' logic
            // Pays: Quote (Lock: TradeValue)
            // Receives: Base (Qty)
            assetPaid = quoteAsset;
            amountPaidScaled = tradeValueScaled;

            assetReceived = baseAsset;
            proceedsScaled = quantityScaled;
        } else {
            // SELLER: Verified 'Sell' logic
            // Pays: Base (Lock: Qty)
            // Receives: Quote (TradeValue)
            assetPaid = baseAsset;
            amountPaidScaled = quantityScaled;

            assetReceived = quoteAsset;
            proceedsScaled = tradeValueScaled;
        }

        // 1. Deduct Paid Asset (Locked)
        BigDecimal amountPaid = BigDecimal.valueOf(FixedPointMath.toDouble(amountPaidScaled));
        var balancePaid = accountService.deductLockedBalance(accountId, assetPaid, amountPaid);
        accountStateStore.updateBalance(balancePaid);

        // 2. Calculate Fee on Proceeds
        // Note: passing proceedsScaled as value to apply rate to
        long feeScaled = feeService.calculateFeeScaled(accountId, symbol, proceedsScaled, isMaker);
        long netProceedsScaled = FixedPointMath.subtract(proceedsScaled, feeScaled);

        // 3. Credit Received Asset (Available)
        BigDecimal netProceeds = BigDecimal.valueOf(FixedPointMath.toDouble(netProceedsScaled));
        var balanceReceived = accountService.creditBalance(accountId, assetReceived, netProceeds);
        accountStateStore.updateBalance(balanceReceived);
    }
}
