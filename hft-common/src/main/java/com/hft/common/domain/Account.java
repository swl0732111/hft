package com.hft.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("accounts")
public class Account {
    @Id
    private String id;
    private String userId;
    private String username;
    private String email;
    private long createdAt;
    private AccountStatus status;

  // User tier and volume tracking
  private UserTier currentTier;
  private long volume30dScaled; // 30-day trading volume (scaled)
  private long lastTierUpdate; // Timestamp of last tier calculation
  private Long tierLockedUntil; // Optional manual tier lock (for promotions)

    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }

  /** Check if tier is manually locked (e.g., promotional tier). */
  public boolean isTierLocked() {
    return tierLockedUntil != null && tierLockedUntil > System.currentTimeMillis();
  }

  /** Get current tier, defaulting to VIP0 if null. */
  public UserTier getCurrentTierOrDefault() {
    return currentTier != null ? currentTier : UserTier.VIP0;
  }
}
