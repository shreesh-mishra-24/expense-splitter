package com.expensesplitter.service;

import com.expensesplitter.dto.*;
import com.expensesplitter.exception.InvalidOperationException;
import com.expensesplitter.exception.ResourceNotFoundException;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import com.expensesplitter.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing groups, members, and expenses.
 *
 * This is the main service that coordinates between the repository
 * and the calculation services.
 */
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final BalanceCalculator balanceCalculator;
    private final DebtSimplifier debtSimplifier;

    public GroupService(GroupRepository groupRepository,
                        BalanceCalculator balanceCalculator,
                        DebtSimplifier debtSimplifier) {
        this.groupRepository = groupRepository;
        this.balanceCalculator = balanceCalculator;
        this.debtSimplifier = debtSimplifier;
    }

    // =========================================================================
    // Group Operations
    // =========================================================================

    /**
     * Creates a new group.
     *
     * @param request the group creation request
     * @return the created group
     */
    public Group createGroup(GroupCreateRequest request) {
        Group group = new Group(request.getName());
        return groupRepository.save(group);
    }

    /**
     * Gets a group by ID.
     *
     * @param groupId the group ID
     * @return the group
     * @throws ResourceNotFoundException if group not found
     */
    public Group getGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId.toString()));
    }

    /**
     * Gets all groups.
     *
     * @return list of all groups
     */
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    /**
     * Deletes a group.
     *
     * @param groupId the group ID to delete
     * @throws ResourceNotFoundException if group not found
     */
    public void deleteGroup(UUID groupId) {
        if (!groupRepository.deleteById(groupId)) {
            throw new ResourceNotFoundException("Group", groupId.toString());
        }
    }

    // =========================================================================
    // Member Operations
    // =========================================================================

    /**
     * Adds a member to a group.
     *
     * @param groupId the group ID
     * @param request the member creation request
     * @return the created member
     * @throws ResourceNotFoundException if group not found
     */
    public Member addMember(UUID groupId, MemberCreateRequest request) {
        Group group = getGroup(groupId);
        Member member = new Member(request.getName());
        group.addMember(member);
        groupRepository.save(group);
        return member;
    }

    /**
     * Gets all members in a group.
     *
     * @param groupId the group ID
     * @return list of members
     * @throws ResourceNotFoundException if group not found
     */
    public List<Member> getMembers(UUID groupId) {
        return getGroup(groupId).getMembers();
    }

    /**
     * Gets a specific member from a group.
     *
     * @param groupId  the group ID
     * @param memberId the member ID
     * @return the member
     * @throws ResourceNotFoundException if group or member not found
     */
    public Member getMember(UUID groupId, UUID memberId) {
        Group group = getGroup(groupId);
        return group.findMemberById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId.toString()));
    }

    /**
     * Removes a member from a group.
     *
     * @param groupId  the group ID
     * @param memberId the member ID to remove
     * @throws ResourceNotFoundException  if group or member not found
     * @throws InvalidOperationException if member has expenses
     */
    public void removeMember(UUID groupId, UUID memberId) {
        Group group = getGroup(groupId);

        // Check if member exists
        if (!group.hasMember(memberId)) {
            throw new ResourceNotFoundException("Member", memberId.toString());
        }

        // Check if member is involved in any expenses
        if (group.isMemberInvolvedInExpenses(memberId)) {
            throw new InvalidOperationException(
                    "Cannot remove member with ID " + memberId + " because they are involved in expenses"
            );
        }

        group.removeMember(memberId);
        groupRepository.save(group);
    }

    // =========================================================================
    // Expense Operations
    // =========================================================================

    /**
     * Adds an expense to a group.
     *
     * @param groupId the group ID
     * @param request the expense creation request
     * @return the created expense
     * @throws ResourceNotFoundException  if group not found
     * @throws InvalidOperationException if payer or participants are invalid
     */
    public Expense addExpense(UUID groupId, ExpenseCreateRequest request) {
        Group group = getGroup(groupId);

        // Validate payer exists
        if (!group.hasMember(request.getPayerId())) {
            throw new InvalidOperationException(
                    "Payer with ID " + request.getPayerId() + " is not a member of the group"
            );
        }

        // Validate all participants exist
        for (UUID participantId : request.getParticipantIds()) {
            if (!group.hasMember(participantId)) {
                throw new InvalidOperationException(
                        "Participant with ID " + participantId + " is not a member of the group"
                );
            }
        }

        Expense expense = new Expense(
                request.getDescription(),
                request.getAmount(),
                request.getPayerId(),
                request.getParticipantIds()
        );

        group.addExpense(expense);
        groupRepository.save(group);
        return expense;
    }

    /**
     * Gets all expenses in a group.
     *
     * @param groupId the group ID
     * @return list of expenses
     * @throws ResourceNotFoundException if group not found
     */
    public List<Expense> getExpenses(UUID groupId) {
        return getGroup(groupId).getExpenses();
    }

    /**
     * Gets a specific expense from a group.
     *
     * @param groupId   the group ID
     * @param expenseId the expense ID
     * @return the expense
     * @throws ResourceNotFoundException if group or expense not found
     */
    public Expense getExpense(UUID groupId, UUID expenseId) {
        Group group = getGroup(groupId);
        return group.findExpenseById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId.toString()));
    }

    /**
     * Deletes an expense from a group.
     *
     * @param groupId   the group ID
     * @param expenseId the expense ID to delete
     * @throws ResourceNotFoundException if group or expense not found
     */
    public void deleteExpense(UUID groupId, UUID expenseId) {
        Group group = getGroup(groupId);

        if (!group.removeExpense(expenseId)) {
            throw new ResourceNotFoundException("Expense", expenseId.toString());
        }

        groupRepository.save(group);
    }

    // =========================================================================
    // Balance and Settlement Operations
    // =========================================================================

    /**
     * Gets balances for all members in a group.
     *
     * @param groupId the group ID
     * @return list of balance responses
     * @throws ResourceNotFoundException if group not found
     */
    public List<BalanceResponse> getBalances(UUID groupId) {
        Group group = getGroup(groupId);
        return balanceCalculator.calculateBalances(group);
    }

    /**
     * Gets the optimized settlement plan for a group.
     *
     * @param groupId the group ID
     * @return settlement plan response
     * @throws ResourceNotFoundException if group not found
     */
    public SettlementPlanResponse getSettlements(UUID groupId) {
        Group group = getGroup(groupId);
        return debtSimplifier.simplifyDebts(group);
    }
}
