package com.expensesplitter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a member's balance within a group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private UUID memberId;
    private String memberName;
    private BigDecimal totalPaid;
    private BigDecimal totalOwed;
    private BigDecimal netBalance;
}
