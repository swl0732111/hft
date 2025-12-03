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

    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED
    }
}
