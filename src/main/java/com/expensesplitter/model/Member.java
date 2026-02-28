package com.expensesplitter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a member in an expense-sharing group.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private UUID id;
    private String name;

    /**
     * Creates a new member with the given name.
     * A unique ID is automatically generated.
     *
     * @param name the name of the member
     */
    public Member(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }
}
