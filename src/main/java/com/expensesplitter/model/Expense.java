package com.expensesplitter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents an expense within a group.
 *
 * An expense has a payer (the person who paid) and participants
 * (the people who benefited from the expense and should share the cost).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    private UUID id;
    private String description;
    private BigDecimal amount;
    private UUID payerId;
    private List<UUID> participantIds;
    private LocalDateTime createdAt;

    /**
     * Creates a new expense with auto-generated ID and timestamp.
     *
     * @param description    description of the expense
     * @param amount         amount of the expense
     * @param payerId        ID of the member who paid
     * @param participantIds IDs of members who participated in the expense
     */
    public Expense(String description, BigDecimal amount, UUID payerId, List<UUID> participantIds) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.amount = amount;
        this.payerId = payerId;
        this.participantIds = participantIds;
        this.createdAt = LocalDateTime.now();
    }
}
