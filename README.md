# Expense Splitter API

A REST API built with Java and Spring Boot for managing shared expenses among groups of people, calculating balances, and generating optimized settlement plans.

## Features

- **Group Management**: Create and manage expense-sharing groups
- **Member Management**: Add and remove members from groups
- **Expense Tracking**: Record expenses with details about who paid and who participated
- **Balance Calculation**: Calculate each member's balance within the group
- **Debt Simplification**: Generate optimized settlement plans with minimum transactions

### Debt Simplification Example

Given the following transactions:
- A pays B $40
- B pays C $40
- C pays A $10

Instead of three separate repayments, the API simplifies this to:
- **C pays A $30** (single transaction settles all balances)

## Technology Stack

- **Java 17**
- **Spring Boot 3.2**
- **Maven** for build management
- **JUnit 5** for testing
- **SpringDoc OpenAPI** for API documentation
- **Docker** for containerization
- **Lombok** for reducing boilerplate

## Project Structure

```
expense-splitter/
├── src/
│   ├── main/
│   │   ├── java/com/expensesplitter/
│   │   │   ├── ExpenseSplitterApplication.java  # Main entry point
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java           # Swagger configuration
│   │   │   ├── controller/
│   │   │   │   ├── GroupController.java         # REST API endpoints
│   │   │   │   └── HealthController.java        # Health check endpoints
│   │   │   ├── dto/                             # Data Transfer Objects
│   │   │   │   ├── GroupCreateRequest.java
│   │   │   │   ├── MemberCreateRequest.java
│   │   │   │   ├── ExpenseCreateRequest.java
│   │   │   │   ├── BalanceResponse.java
│   │   │   │   ├── SettlementResponse.java
│   │   │   │   └── SettlementPlanResponse.java
│   │   │   ├── exception/                       # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── InvalidOperationException.java
│   │   │   ├── model/                           # Domain models
│   │   │   │   ├── Group.java
│   │   │   │   ├── Member.java
│   │   │   │   └── Expense.java
│   │   │   ├── repository/
│   │   │   │   └── GroupRepository.java         # In-memory storage
│   │   │   └── service/                         # Business logic
│   │   │       ├── GroupService.java
│   │   │       ├── BalanceCalculator.java
│   │   │       └── DebtSimplifier.java
│   │   └── resources/
│   │       └── application.yml                  # Configuration
│   └── test/java/com/expensesplitter/
│       ├── ExpenseSplitterApplicationTest.java
│       ├── service/                             # Unit tests
│       │   ├── BalanceCalculatorTest.java
│       │   ├── DebtSimplifierTest.java
│       │   └── GroupServiceTest.java
│       └── integration/                         # Integration tests
│           └── GroupControllerIntegrationTest.java
├── pom.xml
├── Dockerfile
├── Dockerfile.test
├── docker-compose.yml
└── README.md
```

## Quick Start

### Option 1: Using Docker (Recommended)

```bash
# Build and run the container
docker-compose up --build

# The API will be available at http://localhost:8080
```

### Option 2: Local Development

Prerequisites:
- Java 17 or higher
- Maven 3.8+

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/expense-splitter-1.0.0.jar

# The API will be available at http://localhost:8080
```

## Running Tests

### Using Docker

```bash
# Run tests in a container
docker-compose --profile test run test
```

### Locally

```bash
# Run all tests
mvn test

# Run tests with verbose output
mvn test -Dtest=BalanceCalculatorTest

# Run specific test class
mvn test -Dtest=DebtSimplifierTest

# Run integration tests only
mvn test -Dtest=GroupControllerIntegrationTest
```

## API Documentation

Once the application is running, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## API Endpoints

### Groups

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups` | Create a new group |
| GET | `/api/v1/groups` | Get all groups |
| GET | `/api/v1/groups/{groupId}` | Get a specific group |
| DELETE | `/api/v1/groups/{groupId}` | Delete a group |

### Members

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups/{groupId}/members` | Add a member to a group |
| GET | `/api/v1/groups/{groupId}/members` | Get all members in a group |
| GET | `/api/v1/groups/{groupId}/members/{memberId}` | Get a specific member |
| DELETE | `/api/v1/groups/{groupId}/members/{memberId}` | Remove a member |

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups/{groupId}/expenses` | Add an expense |
| GET | `/api/v1/groups/{groupId}/expenses` | Get all expenses in a group |
| GET | `/api/v1/groups/{groupId}/expenses/{expenseId}` | Get a specific expense |
| DELETE | `/api/v1/groups/{groupId}/expenses/{expenseId}` | Delete an expense |

### Balances & Settlements

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/groups/{groupId}/balances` | Get balances for all members |
| GET | `/api/v1/groups/{groupId}/settlements` | Get optimized settlement plan |

## Usage Example

### 1. Create a Group

```bash
curl -X POST http://localhost:8080/api/v1/groups \
  -H "Content-Type: application/json" \
  -d '{"name": "Bangkok Trip"}'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Bangkok Trip",
  "members": [],
  "expenses": [],
  "createdAt": "2024-01-15T10:00:00"
}
```

### 2. Add Members

```bash
# Add Alice
curl -X POST http://localhost:8080/api/v1/groups/{groupId}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice"}'

# Add Bob
curl -X POST http://localhost:8080/api/v1/groups/{groupId}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob"}'

# Add Charlie
curl -X POST http://localhost:8080/api/v1/groups/{groupId}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Charlie"}'
```

### 3. Add Expenses

```bash
# Alice pays $60 for dinner (everyone participates)
curl -X POST http://localhost:8080/api/v1/groups/{groupId}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner at Thai restaurant",
    "amount": 60.00,
    "payerId": "{aliceId}",
    "participantIds": ["{aliceId}", "{bobId}", "{charlieId}"]
  }'

# Bob pays $30 for taxi (Bob and Charlie only)
curl -X POST http://localhost:8080/api/v1/groups/{groupId}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Taxi to hotel",
    "amount": 30.00,
    "payerId": "{bobId}",
    "participantIds": ["{bobId}", "{charlieId}"]
  }'
```

### 4. Check Balances

```bash
curl http://localhost:8080/api/v1/groups/{groupId}/balances
```

Response:
```json
[
  {
    "memberId": "...",
    "memberName": "Alice",
    "totalPaid": 60.00,
    "totalOwed": 20.00,
    "netBalance": 40.00
  },
  {
    "memberId": "...",
    "memberName": "Bob",
    "totalPaid": 30.00,
    "totalOwed": 35.00,
    "netBalance": -5.00
  },
  {
    "memberId": "...",
    "memberName": "Charlie",
    "totalPaid": 0.00,
    "totalOwed": 35.00,
    "netBalance": -35.00
  }
]
```

### 5. Get Optimized Settlements

```bash
curl http://localhost:8080/api/v1/groups/{groupId}/settlements
```

Response:
```json
{
  "groupId": "...",
  "groupName": "Bangkok Trip",
  "settlements": [
    {
      "fromMemberId": "...",
      "fromMemberName": "Charlie",
      "toMemberId": "...",
      "toMemberName": "Alice",
      "amount": 35.00
    },
    {
      "fromMemberId": "...",
      "fromMemberName": "Bob",
      "toMemberId": "...",
      "toMemberName": "Alice",
      "amount": 5.00
    }
  ],
  "totalTransactions": 2
}
```

## Design Decisions

### Architecture

- **Layered Architecture**: Clear separation between Controller, Service, and Repository layers
- **Service Layer Pattern**: Business logic is separated into dedicated services (`BalanceCalculator`, `DebtSimplifier`, `GroupService`)
- **Dependency Injection**: Spring's DI for clean, testable code
- **DTO Pattern**: Separate request/response objects from domain models

### Debt Simplification Algorithm

The debt simplification uses a **greedy algorithm**:

1. Calculate net balance for each person (total paid - total owed)
2. Separate members into creditors (positive balance) and debtors (negative balance)
3. Match the largest debtor with the largest creditor
4. Transfer the minimum of what the debtor owes and what the creditor is owed
5. Repeat until all debts are settled

This approach minimizes the number of transactions while ensuring all balances are settled.

### Data Storage

Currently uses in-memory storage (`ConcurrentHashMap`) for simplicity. The `GroupRepository` can be easily replaced with a JPA repository for database persistence by:
1. Adding `@Entity` annotations to domain models
2. Creating a JPA interface extending `JpaRepository`
3. Injecting the JPA repository

## Health Check

```bash
curl http://localhost:8080/health
```

Response:
```json
{"status": "healthy"}
```

## Configuration

The application can be configured via `application.yml` or environment variables:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Server port |
| `spring.application.name` | expense-splitter | Application name |

## License

MIT License
