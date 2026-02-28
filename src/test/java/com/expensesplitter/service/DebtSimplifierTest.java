package com.expensesplitter.service;

import com.expensesplitter.dto.SettlementPlanResponse;
import com.expensesplitter.dto.SettlementResponse;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebtSimplifier service.
 */
class DebtSimplifierTest {

    private DebtSimplifier simplifier;

    @BeforeEach
    void setUp() {
        BalanceCalculator calculator = new BalanceCalculator();
        simplifier = new DebtSimplifier(calculator);
    }

    @Test
    @DisplayName("Empty group returns empty settlements")
    void emptyGroupReturnsEmptySettlements() {
        Group group = new Group("Empty Group");

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        assertEquals(group.getId(), plan.getGroupId());
        assertEquals(group.getName(), plan.getGroupName());
        assertTrue(plan.getSettlements().isEmpty());
        assertEquals(0, plan.getTotalTransactions());
    }

    @Test
    @DisplayName("Group with no expenses returns no settlements")
    void groupWithNoExpensesReturnsNoSettlements() {
        Group group = new Group("Test Group");
        group.addMember(new Member("Alice"));
        group.addMember(new Member("Bob"));

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        assertTrue(plan.getSettlements().isEmpty());
        assertEquals(0, plan.getTotalTransactions());
    }

    @Test
    @DisplayName("Simple two-person settlement")
    void simpleTwoPersonSettlement() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        group.addMember(alice);
        group.addMember(bob);

        // Alice pays $100 for both
        Expense expense = new Expense(
                "Dinner",
                new BigDecimal("100.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId())
        );
        group.addExpense(expense);

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        assertEquals(1, plan.getTotalTransactions());
        SettlementResponse settlement = plan.getSettlements().get(0);

        assertEquals(bob.getId(), settlement.getFromMemberId());
        assertEquals("Bob", settlement.getFromMemberName());
        assertEquals(alice.getId(), settlement.getToMemberId());
        assertEquals("Alice", settlement.getToMemberName());
        assertEquals(new BigDecimal("50.00"), settlement.getAmount());
    }

    @Test
    @DisplayName("Circular debt simplification - problem statement example")
    void circularDebtSimplification() {
        // A -> paid -> B - $40
        // B -> paid -> C - $40
        // C -> paid -> A - $10
        // Should simplify to: C pays A $30

        Group group = new Group("Test Group");
        Member a = new Member("A");
        Member b = new Member("B");
        Member c = new Member("C");
        group.addMember(a);
        group.addMember(b);
        group.addMember(c);

        // A pays $40 for B (only B participates)
        group.addExpense(new Expense(
                "A pays for B",
                new BigDecimal("40.00"),
                a.getId(),
                List.of(b.getId())
        ));

        // B pays $40 for C (only C participates)
        group.addExpense(new Expense(
                "B pays for C",
                new BigDecimal("40.00"),
                b.getId(),
                List.of(c.getId())
        ));

        // C pays $10 for A (only A participates)
        group.addExpense(new Expense(
                "C pays for A",
                new BigDecimal("10.00"),
                c.getId(),
                List.of(a.getId())
        ));

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        // Net balances:
        // A: paid 40, owes 10 = net +30
        // B: paid 40, owes 40 = net 0
        // C: paid 10, owes 40 = net -30

        // Should be single transaction: C pays A $30
        assertEquals(1, plan.getTotalTransactions());
        SettlementResponse settlement = plan.getSettlements().get(0);

        assertEquals(c.getId(), settlement.getFromMemberId());
        assertEquals("C", settlement.getFromMemberName());
        assertEquals(a.getId(), settlement.getToMemberId());
        assertEquals("A", settlement.getToMemberName());
        assertEquals(new BigDecimal("30.00"), settlement.getAmount());
    }

    @Test
    @DisplayName("Multiple settlements needed")
    void multipleSettlementsNeeded() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        Member charlie = new Member("Charlie");
        group.addMember(alice);
        group.addMember(bob);
        group.addMember(charlie);

        // Alice pays $90 for all three
        Expense expense = new Expense(
                "Dinner",
                new BigDecimal("90.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId(), charlie.getId())
        );
        group.addExpense(expense);

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        // Alice: paid 90, owes 30, net +60
        // Bob: paid 0, owes 30, net -30
        // Charlie: paid 0, owes 30, net -30

        // Should have 2 transactions (Bob -> Alice and Charlie -> Alice)
        assertEquals(2, plan.getTotalTransactions());

        BigDecimal totalToAlice = plan.getSettlements().stream()
                .filter(s -> s.getToMemberId().equals(alice.getId()))
                .map(SettlementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("60.00"), totalToAlice);
    }

    @Test
    @DisplayName("Balanced group needs no settlements")
    void balancedGroupNoSettlements() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        group.addMember(alice);
        group.addMember(bob);

        // Alice pays $50, both participate
        group.addExpense(new Expense(
                "Lunch",
                new BigDecimal("50.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId())
        ));

        // Bob pays $50, both participate
        group.addExpense(new Expense(
                "Dinner",
                new BigDecimal("50.00"),
                bob.getId(),
                List.of(alice.getId(), bob.getId())
        ));

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        // Both have net balance of 0
        assertEquals(0, plan.getTotalTransactions());
        assertTrue(plan.getSettlements().isEmpty());
    }

    @Test
    @DisplayName("Greedy optimization reduces transactions")
    void greedyOptimizationReducesTransactions() {
        Group group = new Group("Test Group");
        Member[] members = new Member[4];
        for (int i = 0; i < 4; i++) {
            members[i] = new Member("Person" + i);
            group.addMember(members[i]);
        }

        // Person0 pays $120 for all 4
        group.addExpense(new Expense(
                "Big dinner",
                new BigDecimal("120.00"),
                members[0].getId(),
                List.of(members[0].getId(), members[1].getId(),
                        members[2].getId(), members[3].getId())
        ));

        // Person1 pays $40 for Person1 and Person2
        group.addExpense(new Expense(
                "Taxi",
                new BigDecimal("40.00"),
                members[1].getId(),
                List.of(members[1].getId(), members[2].getId())
        ));

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        // Net balances:
        // P0: paid 120, owes 30 = +90
        // P1: paid 40, owes 30+20=50 = -10
        // P2: paid 0, owes 30+20=50 = -50
        // P3: paid 0, owes 30 = -30

        // Should be minimal transactions
        assertTrue(plan.getTotalTransactions() <= 3);

        // Verify total amount transferred equals what P0 should receive
        BigDecimal totalTransferred = plan.getSettlements().stream()
                .map(SettlementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("90.00"), totalTransferred);
    }

    @Test
    @DisplayName("Settlement amounts are properly rounded")
    void settlementAmountsAreRounded() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        Member charlie = new Member("Charlie");
        group.addMember(alice);
        group.addMember(bob);
        group.addMember(charlie);

        // $100 split 3 ways = 33.33... each
        Expense expense = new Expense(
                "Dinner",
                new BigDecimal("100.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId(), charlie.getId())
        );
        group.addExpense(expense);

        SettlementPlanResponse plan = simplifier.simplifyDebts(group);

        // Check all amounts are rounded to 2 decimal places
        for (SettlementResponse settlement : plan.getSettlements()) {
            assertEquals(2, settlement.getAmount().scale());
        }
    }
}
