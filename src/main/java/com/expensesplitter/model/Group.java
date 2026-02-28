package com.expensesplitter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an expense-sharing group.
 *
 * A group contains members and their shared expenses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    private UUID id;
    private String name;
    private List<Member> members;
    private List<Expense> expenses;
    private LocalDateTime createdAt;

    /**
     * Creates a new group with the given name.
     * Members and expenses lists are initialized as empty.
     *
     * @param name the name of the group
     */
    public Group(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.members = new ArrayList<>();
        this.expenses = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Adds a member to the group.
     *
     * @param member the member to add
     */
    public void addMember(Member member) {
        this.members.add(member);
    }

    /**
     * Adds an expense to the group.
     *
     * @param expense the expense to add
     */
    public void addExpense(Expense expense) {
        this.expenses.add(expense);
    }

    /**
     * Finds a member by their ID.
     *
     * @param memberId the ID of the member to find
     * @return Optional containing the member if found
     */
    public Optional<Member> findMemberById(UUID memberId) {
        return members.stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst();
    }

    /**
     * Finds an expense by its ID.
     *
     * @param expenseId the ID of the expense to find
     * @return Optional containing the expense if found
     */
    public Optional<Expense> findExpenseById(UUID expenseId) {
        return expenses.stream()
                .filter(e -> e.getId().equals(expenseId))
                .findFirst();
    }

    /**
     * Checks if a member ID exists in this group.
     *
     * @param memberId the member ID to check
     * @return true if the member exists in this group
     */
    public boolean hasMember(UUID memberId) {
        return members.stream().anyMatch(m -> m.getId().equals(memberId));
    }

    /**
     * Removes a member from the group.
     *
     * @param memberId the ID of the member to remove
     * @return true if the member was removed
     */
    public boolean removeMember(UUID memberId) {
        return members.removeIf(m -> m.getId().equals(memberId));
    }

    /**
     * Removes an expense from the group.
     *
     * @param expenseId the ID of the expense to remove
     * @return true if the expense was removed
     */
    public boolean removeExpense(UUID expenseId) {
        return expenses.removeIf(e -> e.getId().equals(expenseId));
    }

    /**
     * Checks if a member is involved in any expense (as payer or participant).
     *
     * @param memberId the member ID to check
     * @return true if the member is involved in any expense
     */
    public boolean isMemberInvolvedInExpenses(UUID memberId) {
        return expenses.stream()
                .anyMatch(e -> e.getPayerId().equals(memberId)
                        || e.getParticipantIds().contains(memberId));
    }
}
