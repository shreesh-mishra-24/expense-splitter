package com.expensesplitter.integration;

import com.expensesplitter.dto.*;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Member;
import com.expensesplitter.repository.GroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the REST API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        groupRepository.clear();
    }

    @Nested
    @DisplayName("Group Endpoints")
    class GroupEndpoints {

        @Test
        @DisplayName("POST /api/v1/groups - Create group")
        void createGroup() throws Exception {
            GroupCreateRequest request = new GroupCreateRequest("Bangkok Trip");

            mockMvc.perform(post("/api/v1/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Bangkok Trip"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.members").isEmpty())
                    .andExpect(jsonPath("$.expenses").isEmpty());
        }

        @Test
        @DisplayName("POST /api/v1/groups - Empty name fails validation")
        void createGroupEmptyNameFails() throws Exception {
            GroupCreateRequest request = new GroupCreateRequest("");

            mockMvc.perform(post("/api/v1/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/v1/groups - Get all groups")
        void getAllGroups() throws Exception {
            // Create two groups
            createTestGroup("Group 1");
            createTestGroup("Group 2");

            mockMvc.perform(get("/api/v1/groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id} - Get group by ID")
        void getGroupById() throws Exception {
            String groupId = createTestGroup("Test Group");

            mockMvc.perform(get("/api/v1/groups/" + groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(groupId))
                    .andExpect(jsonPath("$.name").value("Test Group"));
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id} - Nonexistent group returns 404")
        void getNonexistentGroupReturns404() throws Exception {
            mockMvc.perform(get("/api/v1/groups/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /api/v1/groups/{id} - Delete group")
        void deleteGroup() throws Exception {
            String groupId = createTestGroup("Test Group");

            mockMvc.perform(delete("/api/v1/groups/" + groupId))
                    .andExpect(status().isNoContent());

            // Verify deleted
            mockMvc.perform(get("/api/v1/groups/" + groupId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Member Endpoints")
    class MemberEndpoints {

        private String groupId;

        @BeforeEach
        void setUp() throws Exception {
            groupId = createTestGroup("Test Group");
        }

        @Test
        @DisplayName("POST /api/v1/groups/{id}/members - Add member")
        void addMember() throws Exception {
            MemberCreateRequest request = new MemberCreateRequest("Alice");

            mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id}/members - Get all members")
        void getMembers() throws Exception {
            createTestMember(groupId, "Alice");
            createTestMember(groupId, "Bob");

            mockMvc.perform(get("/api/v1/groups/" + groupId + "/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("GET /api/v1/groups/{gid}/members/{mid} - Get member by ID")
        void getMemberById() throws Exception {
            String memberId = createTestMember(groupId, "Alice");

            mockMvc.perform(get("/api/v1/groups/" + groupId + "/members/" + memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice"));
        }

        @Test
        @DisplayName("DELETE /api/v1/groups/{gid}/members/{mid} - Remove member")
        void removeMember() throws Exception {
            String memberId = createTestMember(groupId, "Alice");

            mockMvc.perform(delete("/api/v1/groups/" + groupId + "/members/" + memberId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Expense Endpoints")
    class ExpenseEndpoints {

        private String groupId;
        private String aliceId;
        private String bobId;

        @BeforeEach
        void setUp() throws Exception {
            groupId = createTestGroup("Test Group");
            aliceId = createTestMember(groupId, "Alice");
            bobId = createTestMember(groupId, "Bob");
        }

        @Test
        @DisplayName("POST /api/v1/groups/{id}/expenses - Add expense")
        void addExpense() throws Exception {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    UUID.fromString(aliceId),
                    List.of(UUID.fromString(aliceId), UUID.fromString(bobId))
            );

            mockMvc.perform(post("/api/v1/groups/" + groupId + "/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description").value("Dinner"))
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.payerId").value(aliceId));
        }

        @Test
        @DisplayName("POST /api/v1/groups/{id}/expenses - Invalid payer returns 400")
        void addExpenseInvalidPayerReturns400() throws Exception {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("100.00"),
                    UUID.randomUUID(), // Invalid
                    List.of(UUID.fromString(aliceId))
            );

            mockMvc.perform(post("/api/v1/groups/" + groupId + "/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/groups/{id}/expenses - Negative amount fails validation")
        void addExpenseNegativeAmountFails() throws Exception {
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                    "Dinner",
                    new BigDecimal("-50.00"),
                    UUID.fromString(aliceId),
                    List.of(UUID.fromString(aliceId))
            );

            mockMvc.perform(post("/api/v1/groups/" + groupId + "/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id}/expenses - Get all expenses")
        void getExpenses() throws Exception {
            createTestExpense(groupId, aliceId, "Dinner", "100.00");
            createTestExpense(groupId, bobId, "Taxi", "50.00");

            mockMvc.perform(get("/api/v1/groups/" + groupId + "/expenses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("DELETE /api/v1/groups/{gid}/expenses/{eid} - Delete expense")
        void deleteExpense() throws Exception {
            String expenseId = createTestExpense(groupId, aliceId, "Dinner", "100.00");

            mockMvc.perform(delete("/api/v1/groups/" + groupId + "/expenses/" + expenseId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Balance and Settlement Endpoints")
    class BalanceAndSettlementEndpoints {

        private String groupId;
        private String aliceId;
        private String bobId;

        @BeforeEach
        void setUp() throws Exception {
            groupId = createTestGroup("Test Group");
            aliceId = createTestMember(groupId, "Alice");
            bobId = createTestMember(groupId, "Bob");
            createTestExpense(groupId, aliceId, "Dinner", "100.00");
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id}/balances - Get balances")
        void getBalances() throws Exception {
            mockMvc.perform(get("/api/v1/groups/" + groupId + "/balances"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[?(@.memberId == '" + aliceId + "')].netBalance").value(50.00))
                    .andExpect(jsonPath("$[?(@.memberId == '" + bobId + "')].netBalance").value(-50.00));
        }

        @Test
        @DisplayName("GET /api/v1/groups/{id}/settlements - Get settlements")
        void getSettlements() throws Exception {
            mockMvc.perform(get("/api/v1/groups/" + groupId + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupId").value(groupId))
                    .andExpect(jsonPath("$.totalTransactions").value(1))
                    .andExpect(jsonPath("$.settlements[0].fromMemberId").value(bobId))
                    .andExpect(jsonPath("$.settlements[0].toMemberId").value(aliceId))
                    .andExpect(jsonPath("$.settlements[0].amount").value(50.00));
        }
    }

    @Nested
    @DisplayName("Circular Debt Simplification - Problem Statement Example")
    class CircularDebtSimplification {

        @Test
        @DisplayName("A->B $40, B->C $40, C->A $10 simplifies to C->A $30")
        void circularDebtExample() throws Exception {
            // Create group and members
            String groupId = createTestGroup("Circular Debt Test");
            String aId = createTestMember(groupId, "A");
            String bId = createTestMember(groupId, "B");
            String cId = createTestMember(groupId, "C");

            // A pays $40 for B (only B participates)
            createTestExpenseWithParticipants(groupId, aId, "A pays for B", "40.00",
                    List.of(UUID.fromString(bId)));

            // B pays $40 for C (only C participates)
            createTestExpenseWithParticipants(groupId, bId, "B pays for C", "40.00",
                    List.of(UUID.fromString(cId)));

            // C pays $10 for A (only A participates)
            createTestExpenseWithParticipants(groupId, cId, "C pays for A", "10.00",
                    List.of(UUID.fromString(aId)));

            // Verify simplified settlement
            mockMvc.perform(get("/api/v1/groups/" + groupId + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTransactions").value(1))
                    .andExpect(jsonPath("$.settlements[0].fromMemberName").value("C"))
                    .andExpect(jsonPath("$.settlements[0].toMemberName").value("A"))
                    .andExpect(jsonPath("$.settlements[0].amount").value(30.00));
        }
    }

    @Nested
    @DisplayName("Health Endpoints")
    class HealthEndpoints {

        @Test
        @DisplayName("GET / - Root endpoint")
        void rootEndpoint() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Expense Splitter API"));
        }

        @Test
        @DisplayName("GET /health - Health check")
        void healthEndpoint() throws Exception {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("healthy"));
        }
    }

    // Helper methods

    private String createTestGroup(String name) throws Exception {
        GroupCreateRequest request = new GroupCreateRequest(name);

        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Group group = objectMapper.readValue(
                result.getResponse().getContentAsString(), Group.class);
        return group.getId().toString();
    }

    private String createTestMember(String groupId, String name) throws Exception {
        MemberCreateRequest request = new MemberCreateRequest(name);

        MvcResult result = mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Member member = objectMapper.readValue(
                result.getResponse().getContentAsString(), Member.class);
        return member.getId().toString();
    }

    private String createTestExpense(String groupId, String payerId,
                                     String description, String amount) throws Exception {
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                description,
                new BigDecimal(amount),
                UUID.fromString(payerId),
                List.of(UUID.fromString(payerId))
        );

        // Get all members and add them as participants
        MvcResult membersResult = mockMvc.perform(get("/api/v1/groups/" + groupId + "/members"))
                .andReturn();
        Member[] members = objectMapper.readValue(
                membersResult.getResponse().getContentAsString(), Member[].class);

        List<UUID> participantIds = java.util.Arrays.stream(members)
                .map(Member::getId)
                .toList();

        request = new ExpenseCreateRequest(
                description,
                new BigDecimal(amount),
                UUID.fromString(payerId),
                participantIds
        );

        MvcResult result = mockMvc.perform(post("/api/v1/groups/" + groupId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Expense expense = objectMapper.readValue(
                result.getResponse().getContentAsString(), Expense.class);
        return expense.getId().toString();
    }

    private void createTestExpenseWithParticipants(String groupId, String payerId,
                                                   String description, String amount,
                                                   List<UUID> participantIds) throws Exception {
        ExpenseCreateRequest request = new ExpenseCreateRequest(
                description,
                new BigDecimal(amount),
                UUID.fromString(payerId),
                participantIds
        );

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
