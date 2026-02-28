package com.expensesplitter.service;

import com.expensesplitter.dto.*;
import com.expensesplitter.exception.InvalidOperationException;
import com.expensesplitter.exception.ResourceNotFoundException;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import com.expensesplitter.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GroupService.
 */
class GroupServiceTest {

    private GroupService service;
    private GroupRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GroupRepository();
        BalanceCalculator balanceCalculator = new BalanceCalculator();
        DebtSimplifier debtSimplifier = new DebtSimplifier(balanceCalculator);
        service = new GroupService(repository, balanceCalculator, debtSimplifier);
    }

    @Nested
    @DisplayName("Group Operations")
    class GroupOperations {

        @Test
        @DisplayName("Create group successfully")
        void createGroup() {
            GroupCreateRequest request = new GroupCreateRequest("Bangkok Trip");

            Group group = service.createGroup(request);

            assertNotNull(group.getId());
            assertEquals("Bangkok Trip", group.getName());
            assertTrue(group.getMembers().isEmpty());
            assertTrue(group.getExpenses().isEmpty());
        }

        @Test
        @DisplayName("Get group by ID")
        void getGroup() {
            Group created = service.createGroup(new GroupCreateRequest("Test Group"));

            Group retrieved = service.getGroup(created.getId());

            assertEquals(created.getId(), retrieved.getId());
            assertEquals(created.getName(), retrieved.getName());
        }

        @Test
        @DisplayName("Get nonexistent group throws exception")
        void getNonexistentGroupThrowsException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getGroup(randomId));
        }

        @Test
        @DisplayName("Get all groups")
        void getAllGroups() {
            service.createGroup(new GroupCreateRequest("Group 1"));
            service.createGroup(new GroupCreateRequest("Group 2"));

            List<Group> groups = service.getAllGroups();

            assertEquals(2, groups.size());
        }

        @Test
        @DisplayName("Delete group successfully")
        void deleteGroup() {
            Group group = service.createGroup(new GroupCreateRequest("Test Group"));

            service.deleteGroup(group.getId());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getGroup(group.getId()));
        }

        @Test
        @DisplayName("Delete nonexistent group throws exception")
        void deleteNonexistentGroupThrowsException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteGroup(randomId));
        }
    }

    @Nested
    @DisplayName("Member Operations")
    class MemberOperations {

        private Group group;

        @BeforeEach
        void setUp() {
            group = service.createGroup(new GroupCreateRequest("Test Group"));
        }

        @Test
        @DisplayName("Add member successfully")
        void addMember() {
            MemberCreateRequest request = new MemberCreateRequest("Alice");

            Member member = service.addMember(group.getId(), request);

            assertNotNull(member.getId());
            assertEquals("Alice", member.getName());

            // Verify member is in group
            Group updated = service.getGroup(group.getId());
            assertEquals(1, updated.getMembers().size());
        }

        @Test
        @DisplayName("Add member to nonexistent group throws exception")
        void addMemberToNonexistentGroupThrowsException() {
            UUID randomId = UUID.randomUUID();
            MemberCreateRequest request = new MemberCreateRequest("Alice");

            assertThrows(ResourceNotFoundException.class,
                    () -> service.addMember(randomId, request));
        }

        @Test
        @DisplayName("Get member successfully")
        void getMember() {
            Member created = service.addMember(group.getId(),
                    new MemberCreateRequest("Alice"));

            Member retrieved = service.getMember(group.getId(), created.getId());

            assertEquals(created.getId(), retrieved.getId());
            assertEquals("Alice", retrieved.getName());
        }

        @Test
        @DisplayName("Get nonexistent member throws exception")
        void getNonexistentMemberThrowsException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getMember(group.getId(), randomId));
        }

        @Test
        @DisplayName("Remove member without expenses")
        void removeMemberWithoutExpenses() {
            Member member = service.addMember(group.getId(),
                    new MemberCreateRequest("Alice"));

            service.removeMember(group.getId(), member.getId());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getMember(group.getId(), member.getId()));
        }

        @Test
        @DisplayName("Remove member with expenses fails")
        void removeMemberWithExpensesFails() {
            Member alice = service.addMember(group.getId(),
                    new MemberCreateRequest("Alice"));
            Member bob = service.addMember(group.getId(),
                    new MemberCreateRequest("Bob"));

            // Add expense with Alice as payer
            service.addExpense(group.getId(), new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("50.00"),
                    alice.getId(),
                    List.of(alice.getId(), bob.getId())
            ));

            // Should fail to remove Alice
            assertThrows(InvalidOperationException.class,
                    () -> service.removeMember(group.getId(), alice.getId()));

            // Alice should still exist
            assertNotNull(service.getMember(group.getId(), alice.getId()));
        }
    }

    @Nested
    @DisplayName("Expense Operations")
    class ExpenseOperations {

        private Group group;
        private Member alice;
        private Member bob;

        @BeforeEach
        void setUp() {
            group = service.createGroup(new GroupCreateRequest("Test Group"));
            alice = service.addMember(group.getId(), new MemberCreateRequest("Alice"));
            bob = service.addMember(group.getId(), new MemberCreateRequest("Bob"));
        }

        @Test
        @DisplayName("Add expense successfully")
        void addExpense() {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    alice.getId(),
                    List.of(alice.getId(), bob.getId())
            );

            Expense expense = service.addExpense(group.getId(), request);

            assertNotNull(expense.getId());
            assertEquals("Dinner", expense.getDescription());
            assertEquals(new BigDecimal("100.00"), expense.getAmount());
            assertEquals(alice.getId(), expense.getPayerId());
        }

        @Test
        @DisplayName("Add expense with invalid payer fails")
        void addExpenseWithInvalidPayerFails() {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    UUID.randomUUID(), // Invalid payer
                    List.of(alice.getId())
            );

            assertThrows(InvalidOperationException.class,
                    () -> service.addExpense(group.getId(), request));
        }

        @Test
        @DisplayName("Add expense with invalid participant fails")
        void addExpenseWithInvalidParticipantFails() {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    alice.getId(),
                    List.of(alice.getId(), UUID.randomUUID()) // Invalid participant
            );

            assertThrows(InvalidOperationException.class,
                    () -> service.addExpense(group.getId(), request));
        }

        @Test
        @DisplayName("Get expense successfully")
        void getExpense() {
            Expense created = service.addExpense(group.getId(), new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    alice.getId(),
                    List.of(alice.getId())
            ));

            Expense retrieved = service.getExpense(group.getId(), created.getId());

            assertEquals(created.getId(), retrieved.getId());
        }

        @Test
        @DisplayName("Delete expense successfully")
        void deleteExpense() {
            Expense expense = service.addExpense(group.getId(), new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    alice.getId(),
                    List.of(alice.getId())
            ));

            service.deleteExpense(group.getId(), expense.getId());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getExpense(group.getId(), expense.getId()));
        }
    }

    @Nested
    @DisplayName("Balance and Settlement Operations")
    class BalanceAndSettlementOperations {

        private Group group;
        private Member alice;
        private Member bob;

        @BeforeEach
        void setUp() {
            group = service.createGroup(new GroupCreateRequest("Test Group"));
            alice = service.addMember(group.getId(), new MemberCreateRequest("Alice"));
            bob = service.addMember(group.getId(), new MemberCreateRequest("Bob"));

            service.addExpense(group.getId(), new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    alice.getId(),
                    List.of(alice.getId(), bob.getId())
            ));
        }

        @Test
        @DisplayName("Get balances successfully")
        void getBalances() {
            List<BalanceResponse> balances = service.getBalances(group.getId());

            assertEquals(2, balances.size());

            BalanceResponse aliceBalance = balances.stream()
                    .filter(b -> b.getMemberId().equals(alice.getId()))
                    .findFirst()
                    .orElseThrow();

            assertEquals(new BigDecimal("50.00"), aliceBalance.getNetBalance());
        }

        @Test
        @DisplayName("Get balances for nonexistent group throws exception")
        void getBalancesForNonexistentGroupThrowsException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getBalances(randomId));
        }

        @Test
        @DisplayName("Get settlements successfully")
        void getSettlements() {
            SettlementPlanResponse plan = service.getSettlements(group.getId());

            assertEquals(group.getId(), plan.getGroupId());
            assertEquals(1, plan.getTotalTransactions());
            assertEquals(bob.getId(), plan.getSettlements().get(0).getFromMemberId());
            assertEquals(alice.getId(), plan.getSettlements().get(0).getToMemberId());
            assertEquals(new BigDecimal("50.00"), plan.getSettlements().get(0).getAmount());
        }

        @Test
        @DisplayName("Get settlements for nonexistent group throws exception")
        void getSettlementsForNonexistentGroupThrowsException() {
            UUID randomId = UUID.randomUUID();

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getSettlements(randomId));
        }
    }
}
