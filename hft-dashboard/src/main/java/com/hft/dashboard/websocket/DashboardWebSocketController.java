package com.hft.dashboard.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/** WebSocket controller for handling dashboard subscriptions. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardWebSocketController {

  private final DashboardWebSocketService webSocketService;

  /** Handle dashboard subscription. */
  @MessageMapping("/dashboard/{accountId}/subscribe")
  @SendTo("/topic/dashboard/{accountId}/status")
  public String subscribe(@DestinationVariable String accountId) {
    log.info("WebSocket subscription request from account: {}", accountId);
    webSocketService.subscribe(accountId);
    return "Subscribed to dashboard updates";
  }

  /** Handle dashboard unsubscription. */
  @MessageMapping("/dashboard/{accountId}/unsubscribe")
  @SendTo("/topic/dashboard/{accountId}/status")
  public String unsubscribe(@DestinationVariable String accountId) {
    log.info("WebSocket unsubscription request from account: {}", accountId);
    webSocketService.unsubscribe(accountId);
    return "Unsubscribed from dashboard updates";
  }
}
