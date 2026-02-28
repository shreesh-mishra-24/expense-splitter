package com.expensesplitter.service;

import com.expensesplitter.dto.BalanceResponse;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for calculating balances within a group.
 *
 * Calculates how much each member has paid, how much they owe,
 * and their net balance (positive = others owe them, negative = they owe others).
 */
@Service
public class BalanceCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculates the balance for each member in the group.
     *
     * @param group the group to calculate balances for
     * @return list of balance responses for each member
     */
    public List<BalanceResponse> calculateBalances(Group group) {
        if (group.getMembers().isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Member> memberMap = new HashMap<>();
        Map<UUID, BigDecimal> totalPaid = new HashMap<>();
        Map<UUID, BigDecimal> totalOwed = new HashMap<>();

        // Initialize maps
        for (Member member : group.getMembers()) {
            memberMap.put(member.getId(), member);
            totalPaid.put(member.getId(), BigDecimal.ZERO);
            totalOwed.put(member.getId(), BigDecimal.ZERO);
        }

        // Process each expense
        for (Expense expense : group.getExpenses()) {
            processExpense(expense, totalPaid, totalOwed);
        }

        // Build balance responses
        List<BalanceResponse> balances = new ArrayList<>();
        for (Member member : group.getMembers()) {
            UUID memberId = member.getId();
            BigDecimal paid = totalPaid.get(memberId).setScale(SCALE, ROUNDING_MODE);
            BigDecimal owed = totalOwed.get(memberId).setScale(SCALE, ROUNDING_MODE);
            BigDecimal net = paid.subtract(owed).setScale(SCALE, ROUNDING_MODE);

            balances.add(new BalanceResponse(
                    memberId,
                    member.getName(),
                    paid,
                    owed,
                    net
            ));
        }

        return balances;
    }

    /**
     * Gets net balances as a map for debt simplification.
     *
     * @param group the group to calculate net balances for
     * @return map of member ID to net balance
     */
    public Map<UUID, BigDecimal> getNetBalances(Group group) {
        Map<UUID, BigDecimal> netBalances = new HashMap<>();

        for (BalanceResponse balance : calculateBalances(group)) {
            netBalances.put(balance.getMemberId(), balance.getNetBalance());
        }

        return netBalances;
    }

    /**
     * Processes a single expense and updates the running totals.
     *
     * @param expense   the expense to process
     * @param totalPaid map tracking total paid by each member
     * @param totalOwed map tracking total owed by each member
     */
    private void processExpense(Expense expense,
                                Map<UUID, BigDecimal> totalPaid,
                                Map<UUID, BigDecimal> totalOwed) {
        // Credit the payer
        UUID payerId = expense.getPayerId();
        if (totalPaid.containsKey(payerId)) {
            totalPaid.put(payerId, totalPaid.get(payerId).add(expense.getAmount()));
        }

        // Calculate share per participant
        List<UUID> participantIds = expense.getParticipantIds();
        if (participantIds.isEmpty()) {
            return;
        }

        BigDecimal sharePerPerson = expense.getAmount()
                .divide(BigDecimal.valueOf(participantIds.size()), SCALE + 4, ROUNDING_MODE);

        // Debit each participant
        for (UUID participantId : participantIds) {
            if (totalOwed.containsKey(participantId)) {
                totalOwed.put(participantId, totalOwed.get(participantId).add(sharePerPerson));
            }
        }
    }
}
