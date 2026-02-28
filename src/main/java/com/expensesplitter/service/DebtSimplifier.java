package com.expensesplitter.service;

import com.expensesplitter.dto.SettlementPlanResponse;
import com.expensesplitter.dto.SettlementResponse;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service for simplifying debts within a group.
 *
 * Uses a greedy algorithm to minimize the number of transactions needed
 * to settle all debts within the group.
 *
 * Algorithm:
 * 1. Calculate net balance for each person
 * 2. Separate into creditors (positive balance) and debtors (negative balance)
 * 3. Greedily match the largest debtor with the largest creditor
 * 4. Continue until all debts are settled
 */
@Service
public class DebtSimplifier {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal THRESHOLD = new BigDecimal("0.01");

    private final BalanceCalculator balanceCalculator;

    public DebtSimplifier(BalanceCalculator balanceCalculator) {
        this.balanceCalculator = balanceCalculator;
    }

    /**
     * Calculates the minimum number of transactions needed to settle all debts.
     *
     * @param group the group to simplify debts for
     * @return settlement plan with optimized list of settlements
     */
    public SettlementPlanResponse simplifyDebts(Group group) {
        if (group.getMembers().isEmpty() || group.getExpenses().isEmpty()) {
            return new SettlementPlanResponse(
                    group.getId(),
                    group.getName(),
                    Collections.emptyList(),
                    0
            );
        }

        Map<UUID, Member> memberMap = new HashMap<>();
        for (Member member : group.getMembers()) {
            memberMap.put(member.getId(), member);
        }

        Map<UUID, BigDecimal> netBalances = balanceCalculator.getNetBalances(group);

        // Separate creditors and debtors
        List<MemberBalance> creditors = new ArrayList<>();
        List<MemberBalance> debtors = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal balance = entry.getValue();
            if (balance.compareTo(THRESHOLD) > 0) {
                creditors.add(new MemberBalance(entry.getKey(), balance));
            } else if (balance.compareTo(THRESHOLD.negate()) < 0) {
                debtors.add(new MemberBalance(entry.getKey(), balance.abs()));
            }
        }

        // Generate settlements
        List<SettlementResponse> settlements = generateSettlements(creditors, debtors, memberMap);

        return new SettlementPlanResponse(
                group.getId(),
                group.getName(),
                settlements,
                settlements.size()
        );
    }

    /**
     * Generates optimized settlement transactions using a greedy approach.
     *
     * @param creditors list of creditors with amounts owed to them
     * @param debtors   list of debtors with amounts they owe
     * @param memberMap map of member IDs to Member objects
     * @return list of settlement transactions
     */
    private List<SettlementResponse> generateSettlements(
            List<MemberBalance> creditors,
            List<MemberBalance> debtors,
            Map<UUID, Member> memberMap) {

        List<SettlementResponse> settlements = new ArrayList<>();

        // Create mutable copies
        Map<UUID, BigDecimal> creditorAmounts = new HashMap<>();
        Map<UUID, BigDecimal> debtorAmounts = new HashMap<>();

        for (MemberBalance creditor : creditors) {
            creditorAmounts.put(creditor.memberId, creditor.amount);
        }
        for (MemberBalance debtor : debtors) {
            debtorAmounts.put(debtor.memberId, debtor.amount);
        }

        while (!creditorAmounts.isEmpty() && !debtorAmounts.isEmpty()) {
            // Find max creditor and max debtor
            UUID maxCreditorId = findMaxEntry(creditorAmounts);
            UUID maxDebtorId = findMaxEntry(debtorAmounts);

            BigDecimal creditAmount = creditorAmounts.get(maxCreditorId);
            BigDecimal debtAmount = debtorAmounts.get(maxDebtorId);

            // Settlement amount is the minimum
            BigDecimal settlementAmount = creditAmount.min(debtAmount)
                    .setScale(SCALE, ROUNDING_MODE);

            if (settlementAmount.compareTo(THRESHOLD) > 0) {
                Member fromMember = memberMap.get(maxDebtorId);
                Member toMember = memberMap.get(maxCreditorId);

                settlements.add(new SettlementResponse(
                        maxDebtorId,
                        fromMember.getName(),
                        maxCreditorId,
                        toMember.getName(),
                        settlementAmount
                ));
            }

            // Update remaining amounts
            BigDecimal newCreditAmount = creditAmount.subtract(settlementAmount);
            BigDecimal newDebtAmount = debtAmount.subtract(settlementAmount);

            if (newCreditAmount.compareTo(THRESHOLD) <= 0) {
                creditorAmounts.remove(maxCreditorId);
            } else {
                creditorAmounts.put(maxCreditorId, newCreditAmount);
            }

            if (newDebtAmount.compareTo(THRESHOLD) <= 0) {
                debtorAmounts.remove(maxDebtorId);
            } else {
                debtorAmounts.put(maxDebtorId, newDebtAmount);
            }
        }

        return settlements;
    }

    /**
     * Finds the entry with the maximum value in the map.
     *
     * @param map the map to search
     * @return the key with the maximum value
     */
    private UUID findMaxEntry(Map<UUID, BigDecimal> map) {
        return map.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    /**
     * Helper class to hold member ID and their balance amount.
     */
    private static class MemberBalance {
        final UUID memberId;
        final BigDecimal amount;

        MemberBalance(UUID memberId, BigDecimal amount) {
            this.memberId = memberId;
            this.amount = amount;
        }
    }
}
