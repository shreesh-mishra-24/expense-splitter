package com.expensesplitter.service;

import com.expensesplitter.dto.BalanceResponse;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BalanceCalculator service.
 */
class BalanceCalculatorTest {

    private BalanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BalanceCalculator();
    }

    @Test
    @DisplayName("Empty group returns empty balances")
    void emptyGroupReturnsEmptyBalances() {
        Group group = new Group("Empty Group");

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        assertTrue(balances.isEmpty());
    }

    @Test
    @DisplayName("Group with no expenses has zero balances")
    void groupWithNoExpensesHasZeroBalances() {
        Group group = new Group("Test Group");
        group.addMember(new Member("Alice"));
        group.addMember(new Member("Bob"));

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        assertEquals(2, balances.size());
        for (BalanceResponse balance : balances) {
            assertEquals(BigDecimal.ZERO.setScale(2), balance.getTotalPaid());
            assertEquals(BigDecimal.ZERO.setScale(2), balance.getTotalOwed());
            assertEquals(BigDecimal.ZERO.setScale(2), balance.getNetBalance());
        }
    }

    @Test
    @DisplayName("Single expense split equally between two members")
    void singleExpenseEqualSplit() {
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

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        BalanceResponse aliceBalance = findBalance(balances, alice.getId());
        BalanceResponse bobBalance = findBalance(balances, bob.getId());

        // Alice paid 100, owes 50, net +50
        assertEquals(new BigDecimal("100.00"), aliceBalance.getTotalPaid());
        assertEquals(new BigDecimal("50.00"), aliceBalance.getTotalOwed());
        assertEquals(new BigDecimal("50.00"), aliceBalance.getNetBalance());

        // Bob paid 0, owes 50, net -50
        assertEquals(new BigDecimal("0.00"), bobBalance.getTotalPaid());
        assertEquals(new BigDecimal("50.00"), bobBalance.getTotalOwed());
        assertEquals(new BigDecimal("-50.00"), bobBalance.getNetBalance());
    }

    @Test
    @DisplayName("Multiple expenses with different payers and participants")
    void multipleExpenses() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        Member charlie = new Member("Charlie");
        group.addMember(alice);
        group.addMember(bob);
        group.addMember(charlie);

        // Alice pays $60 for dinner (all three participate)
        Expense expense1 = new Expense(
                "Dinner",
                new BigDecimal("60.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId(), charlie.getId())
        );

        // Bob pays $30 for taxi (only Bob and Charlie)
        Expense expense2 = new Expense(
                "Taxi",
                new BigDecimal("30.00"),
                bob.getId(),
                List.of(bob.getId(), charlie.getId())
        );

        group.addExpense(expense1);
        group.addExpense(expense2);

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        BalanceResponse aliceBalance = findBalance(balances, alice.getId());
        BalanceResponse bobBalance = findBalance(balances, bob.getId());
        BalanceResponse charlieBalance = findBalance(balances, charlie.getId());

        // Alice: paid 60, owes 20 (60/3), net +40
        assertEquals(new BigDecimal("60.00"), aliceBalance.getTotalPaid());
        assertEquals(new BigDecimal("20.00"), aliceBalance.getTotalOwed());
        assertEquals(new BigDecimal("40.00"), aliceBalance.getNetBalance());

        // Bob: paid 30, owes 20 (60/3) + 15 (30/2) = 35, net -5
        assertEquals(new BigDecimal("30.00"), bobBalance.getTotalPaid());
        assertEquals(new BigDecimal("35.00"), bobBalance.getTotalOwed());
        assertEquals(new BigDecimal("-5.00"), bobBalance.getNetBalance());

        // Charlie: paid 0, owes 20 + 15 = 35, net -35
        assertEquals(new BigDecimal("0.00"), charlieBalance.getTotalPaid());
        assertEquals(new BigDecimal("35.00"), charlieBalance.getTotalOwed());
        assertEquals(new BigDecimal("-35.00"), charlieBalance.getNetBalance());
    }

    @Test
    @DisplayName("Expense where payer is not a participant")
    void payerNotParticipant() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        group.addMember(alice);
        group.addMember(bob);

        // Alice pays $50 for Bob only (Alice does not participate)
        Expense expense = new Expense(
                "Gift for Bob",
                new BigDecimal("50.00"),
                alice.getId(),
                List.of(bob.getId())
        );
        group.addExpense(expense);

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        BalanceResponse aliceBalance = findBalance(balances, alice.getId());
        BalanceResponse bobBalance = findBalance(balances, bob.getId());

        // Alice: paid 50, owes 0, net +50
        assertEquals(new BigDecimal("50.00"), aliceBalance.getTotalPaid());
        assertEquals(new BigDecimal("0.00"), aliceBalance.getTotalOwed());
        assertEquals(new BigDecimal("50.00"), aliceBalance.getNetBalance());

        // Bob: paid 0, owes 50, net -50
        assertEquals(new BigDecimal("0.00"), bobBalance.getTotalPaid());
        assertEquals(new BigDecimal("50.00"), bobBalance.getTotalOwed());
        assertEquals(new BigDecimal("-50.00"), bobBalance.getNetBalance());
    }

    @Test
    @DisplayName("Sum of all net balances should be zero (conservation)")
    void sumOfBalancesIsZero() {
        Group group = new Group("Test Group");
        Member[] members = new Member[5];
        for (int i = 0; i < 5; i++) {
            members[i] = new Member("Person" + i);
            group.addMember(members[i]);
        }

        // Create various expenses
        group.addExpense(new Expense(
                "Expense 1",
                new BigDecimal("100.00"),
                members[0].getId(),
                List.of(members[0].getId(), members[1].getId(), members[2].getId(),
                        members[3].getId(), members[4].getId())
        ));

        group.addExpense(new Expense(
                "Expense 2",
                new BigDecimal("75.00"),
                members[2].getId(),
                List.of(members[0].getId(), members[2].getId(), members[4].getId())
        ));

        group.addExpense(new Expense(
                "Expense 3",
                new BigDecimal("50.00"),
                members[4].getId(),
                List.of(members[1].getId(), members[3].getId())
        ));

        List<BalanceResponse> balances = calculator.calculateBalances(group);

        BigDecimal totalNet = balances.stream()
                .map(BalanceResponse::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small floating point error
        assertTrue(totalNet.abs().compareTo(new BigDecimal("0.01")) < 0);
    }

    @Test
    @DisplayName("Get net balances returns map correctly")
    void getNetBalances() {
        Group group = new Group("Test Group");
        Member alice = new Member("Alice");
        Member bob = new Member("Bob");
        group.addMember(alice);
        group.addMember(bob);

        Expense expense = new Expense(
                "Dinner",
                new BigDecimal("100.00"),
                alice.getId(),
                List.of(alice.getId(), bob.getId())
        );
        group.addExpense(expense);

        Map<UUID, BigDecimal> netBalances = calculator.getNetBalances(group);

        assertEquals(new BigDecimal("50.00"), netBalances.get(alice.getId()));
        assertEquals(new BigDecimal("-50.00"), netBalances.get(bob.getId()));
    }

    private BalanceResponse findBalance(List<BalanceResponse> balances, UUID memberId) {
        return balances.stream()
                .filter(b -> b.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow();
    }
}
