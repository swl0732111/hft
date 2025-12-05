package com.hft.dashboard.websocket;

import com.hft.dashboard.dto.DashboardOverviewDTO;
import com.hft.dashboard.dto.TierInfoDTO;
import com.hft.dashboard.service.DashboardService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Service for broadcasting real-time dashboard updates via WebSocket. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardWebSocketService {

  private final SimpMessagingTemplate messagingTemplate;
  private final DashboardService dashboardService;

  // Track active subscriptions
  private final Map<String, Long> activeSubscriptions = new ConcurrentHashMap<>();

  /** Subscribe to dashboard updates for an account. */
  public void subscribe(String accountId) {
    activeSubscriptions.put(accountId, System.currentTimeMillis());
    log.info("Account {} subscribed to dashboard updates", accountId);
  }

  /** Unsubscribe from dashboard updates. */
  public void unsubscribe(String accountId) {
    activeSubscriptions.remove(accountId);
    log.info("Account {} unsubscribed from dashboard updates", accountId);
  }

  /** Broadcast dashboard overview update. */
  public void broadcastOverviewUpdate(String accountId) {
    try {
      DashboardOverviewDTO overview = dashboardService.getOverview(accountId);
      messagingTemplate.convertAndSend("/topic/dashboard/" + accountId + "/overview", overview);
      log.debug("Broadcasted overview update for account: {}", accountId);
    } catch (Exception e) {
      log.error("Failed to broadcast overview update for account: {}", accountId, e);
    }
  }

  /** Broadcast tier info update. */
  public void broadcastTierUpdate(String accountId) {
    try {
      TierInfoDTO tierInfo = dashboardService.getTierInfo(accountId);
      messagingTemplate.convertAndSend("/topic/dashboard/" + accountId + "/tier", tierInfo);
      log.debug("Broadcasted tier update for account: {}", accountId);
    } catch (Exception e) {
      log.error("Failed to broadcast tier update for account: {}", accountId, e);
    }
  }

  /** Broadcast trade notification. */
  //    public void broadcastTradeNotification(String accountId, String symbol, double volume) {
  //        try {
  //            Map<String, Object> notification = Map.of(
  //                    "type", "TRADE",
  //                    "symbol", symbol,
  //                    "volume", volume,
  //                    "timestamp", System.currentTimeMillis());
  //
  //            messagingTemplate.convertAndSend(
  //                    "/topic/dashboard/" + accountId + "/notifications",
  //                    notification);
  //            log.debug("Broadcasted trade notification for account: {}", accountId);
  //        } catch (Exception e) {
  //            log.error("Failed to broadcast trade notification for account: {}", accountId, e);
  //        }
  //    }

  /** Scheduled task to broadcast updates for all active subscriptions. Runs every 5 seconds. */
  @Scheduled(fixedRate = 5000)
  public void broadcastScheduledUpdates() {
    if (activeSubscriptions.isEmpty()) {
      return;
    }

    log.debug(
        "Broadcasting scheduled updates for {} active subscriptions", activeSubscriptions.size());

    activeSubscriptions
        .keySet()
        .forEach(
            accountId -> {
              try {
                broadcastOverviewUpdate(accountId);
                broadcastTierUpdate(accountId);
              } catch (Exception e) {
                log.error("Failed to broadcast scheduled update for account: {}", accountId, e);
              }
            });
  }

  /** Cleanup stale subscriptions (inactive for > 5 minutes). */
  @Scheduled(fixedRate = 60000)
  public void cleanupStaleSubscriptions() {
    long now = System.currentTimeMillis();
    long staleThreshold = 5 * 60 * 1000; // 5 minutes

    activeSubscriptions
        .entrySet()
        .removeIf(
            entry -> {
              boolean isStale = (now - entry.getValue()) > staleThreshold;
              if (isStale) {
                log.info("Removing stale subscription for account: {}", entry.getKey());
              }
              return isStale;
            });
  }
}
