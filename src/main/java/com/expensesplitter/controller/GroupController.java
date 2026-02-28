package com.expensesplitter.controller;

import com.expensesplitter.dto.*;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import com.expensesplitter.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing expense-sharing groups.
 */
@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Operations for managing expense-sharing groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // =========================================================================
    // Group Endpoints
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create a new group", description = "Creates a new expense-sharing group")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<Group> createGroup(@Valid @RequestBody GroupCreateRequest request) {
        Group group = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    @Operation(summary = "Get all groups", description = "Retrieves all expense-sharing groups")
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get a group by ID", description = "Retrieves a specific group with all its members and expenses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group found"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<Group> getGroup(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group", description = "Deletes a group and all associated data")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Group deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Member Endpoints
    // =========================================================================

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Add a member to a group", description = "Adds a new member to an existing group")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Members", description = "Operations for managing group members")
    public ResponseEntity<Member> addMember(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Valid @RequestBody MemberCreateRequest request) {
        Member member = groupService.addMember(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Get all members in a group", description = "Retrieves all members of a specific group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members retrieved"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Members")
    public ResponseEntity<List<Member>> getMembers(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getMembers(groupId));
    }

    @GetMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Get a member by ID", description = "Retrieves a specific member from a group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
    })
    @Tag(name = "Members")
    public ResponseEntity<Member> getMember(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Parameter(description = "Member ID") @PathVariable UUID memberId) {
        return ResponseEntity.ok(groupService.getMember(groupId, memberId));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Remove a member from a group",
            description = "Removes a member from a group. Will fail if the member has any expenses.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "400", description = "Member has expenses and cannot be removed"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
    })
    @Tag(name = "Members")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Parameter(description = "Member ID") @PathVariable UUID memberId) {
        groupService.removeMember(groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Expense Endpoints
    // =========================================================================

    @PostMapping("/{groupId}/expenses")
    @Operation(summary = "Add an expense to a group",
            description = "Records a new expense. The payer and all participants must be existing group members.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Expense added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or invalid member IDs"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Expenses", description = "Operations for managing expenses")
    public ResponseEntity<Expense> addExpense(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Valid @RequestBody ExpenseCreateRequest request) {
        Expense expense = groupService.addExpense(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping("/{groupId}/expenses")
    @Operation(summary = "Get all expenses in a group", description = "Retrieves all expenses recorded in a group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expenses retrieved"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Expenses")
    public ResponseEntity<List<Expense>> getExpenses(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getExpenses(groupId));
    }

    @GetMapping("/{groupId}/expenses/{expenseId}")
    @Operation(summary = "Get an expense by ID", description = "Retrieves a specific expense from a group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense found"),
            @ApiResponse(responseCode = "404", description = "Group or expense not found")
    })
    @Tag(name = "Expenses")
    public ResponseEntity<Expense> getExpense(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Parameter(description = "Expense ID") @PathVariable UUID expenseId) {
        return ResponseEntity.ok(groupService.getExpense(groupId, expenseId));
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @Operation(summary = "Delete an expense", description = "Removes an expense from a group")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Expense deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Group or expense not found")
    })
    @Tag(name = "Expenses")
    public ResponseEntity<Void> deleteExpense(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @Parameter(description = "Expense ID") @PathVariable UUID expenseId) {
        groupService.deleteExpense(groupId, expenseId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Balance and Settlement Endpoints
    // =========================================================================

    @GetMapping("/{groupId}/balances")
    @Operation(summary = "Get balances for all members",
            description = "Calculates and returns the balance for each member, showing total paid, total owed, and net balance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balances calculated"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Balances & Settlements", description = "Operations for calculating balances and settlements")
    public ResponseEntity<List<BalanceResponse>> getBalances(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getBalances(groupId));
    }

    @GetMapping("/{groupId}/settlements")
    @Operation(summary = "Get optimized settlement plan",
            description = """
                    Calculates the minimum number of transactions needed to settle all debts within the group.

                    Example:
                    - A paid B $40
                    - B paid C $40
                    - C paid A $10

                    Instead of three separate repayments, the simplified result is:
                    - C pays A $30 (single transaction settles all balances)
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement plan generated"),
            @ApiResponse(responseCode = "404", description = "Group not found")
    })
    @Tag(name = "Balances & Settlements")
    public ResponseEntity<SettlementPlanResponse> getSettlements(
            @Parameter(description = "Group ID") @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getSettlements(groupId));
    }
}
