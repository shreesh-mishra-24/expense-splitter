package com.expensesplitter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing the optimized settlement plan for a group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPlanResponse {

    private UUID groupId;
    private String groupName;
    private List<SettlementResponse> settlements;
    private int totalTransactions;
}
