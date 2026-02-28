package com.expensesplitter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a single settlement transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {

    private UUID fromMemberId;
    private String fromMemberName;
    private UUID toMemberId;
    private String toMemberName;
    private BigDecimal amount;
}
