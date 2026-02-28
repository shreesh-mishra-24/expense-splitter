package com.expensesplitter.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new expense.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateRequest {

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payer ID is required")
    private UUID payerId;

    @NotNull(message = "Participant IDs are required")
    @NotEmpty(message = "At least one participant is required")
    private List<UUID> participantIds;
}
