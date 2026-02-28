# Expense Splitter API

A REST API for managing shared expenses among groups of people, calculating balances, and generating optimized settlement plans.

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

## Project Structure

```
expense-splitter/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI application entry point
│   ├── api/
│   │   ├── __init__.py
│   │   ├── dependencies.py  # Dependency injection
│   │   └── routes.py        # REST API endpoints
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py       # Pydantic models for validation
│   └── services/
│       ├── __init__.py
│       ├── balance_calculator.py  # Balance calculation logic
│       ├── debt_simplifier.py     # Debt simplification algorithm
│       └── group_service.py       # Group/member/expense management
├── tests/
│   ├── __init__.py
│   ├── conftest.py                 # Test fixtures
│   ├── test_api_integration.py     # API integration tests
│   ├── test_balance_calculator.py  # Balance calculator unit tests
│   ├── test_debt_simplifier.py     # Debt simplifier unit tests
│   └── test_group_service.py       # Group service unit tests
├── Dockerfile
├── Dockerfile.test
├── docker-compose.yml
├── requirements.txt
└── README.md
```

## Quick Start

### Option 1: Using Docker (Recommended)

```bash
# Build and run the container
docker-compose up --build

# The API will be available at http://localhost:8000
```

### Option 2: Local Development

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the application
uvicorn app.main:app --reload

# The API will be available at http://localhost:8000
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
pytest -v

# Run with coverage
pytest -v --cov=app --cov-report=html

# Run specific test file
pytest tests/test_debt_simplifier.py -v
```

## API Documentation

Once the application is running, visit:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## API Endpoints

### Groups

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups` | Create a new group |
| GET | `/api/v1/groups` | Get all groups |
| GET | `/api/v1/groups/{group_id}` | Get a specific group |
| DELETE | `/api/v1/groups/{group_id}` | Delete a group |

### Members

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups/{group_id}/members` | Add a member to a group |
| GET | `/api/v1/groups/{group_id}/members` | Get all members in a group |
| GET | `/api/v1/groups/{group_id}/members/{member_id}` | Get a specific member |
| DELETE | `/api/v1/groups/{group_id}/members/{member_id}` | Remove a member |

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups/{group_id}/expenses` | Add an expense |
| GET | `/api/v1/groups/{group_id}/expenses` | Get all expenses in a group |
| GET | `/api/v1/groups/{group_id}/expenses/{expense_id}` | Get a specific expense |
| DELETE | `/api/v1/groups/{group_id}/expenses/{expense_id}` | Delete an expense |

### Balances & Settlements

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/groups/{group_id}/balances` | Get balances for all members |
| GET | `/api/v1/groups/{group_id}/settlements` | Get optimized settlement plan |

## Usage Example

### 1. Create a Group

```bash
curl -X POST http://localhost:8000/api/v1/groups \
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
  "created_at": "2024-01-15T10:00:00"
}
```

### 2. Add Members

```bash
# Add Alice
curl -X POST http://localhost:8000/api/v1/groups/{group_id}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice"}'

# Add Bob
curl -X POST http://localhost:8000/api/v1/groups/{group_id}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob"}'

# Add Charlie
curl -X POST http://localhost:8000/api/v1/groups/{group_id}/members \
  -H "Content-Type: application/json" \
  -d '{"name": "Charlie"}'
```

### 3. Add Expenses

```bash
# Alice pays $60 for dinner (everyone participates)
curl -X POST http://localhost:8000/api/v1/groups/{group_id}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner at Thai restaurant",
    "amount": 60.0,
    "payer_id": "{alice_id}",
    "participant_ids": ["{alice_id}", "{bob_id}", "{charlie_id}"]
  }'

# Bob pays $30 for taxi (Bob and Charlie only)
curl -X POST http://localhost:8000/api/v1/groups/{group_id}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Taxi to hotel",
    "amount": 30.0,
    "payer_id": "{bob_id}",
    "participant_ids": ["{bob_id}", "{charlie_id}"]
  }'
```

### 4. Check Balances

```bash
curl http://localhost:8000/api/v1/groups/{group_id}/balances
```

Response:
```json
[
  {
    "member_id": "...",
    "member_name": "Alice",
    "total_paid": 60.0,
    "total_owed": 20.0,
    "net_balance": 40.0
  },
  {
    "member_id": "...",
    "member_name": "Bob",
    "total_paid": 30.0,
    "total_owed": 35.0,
    "net_balance": -5.0
  },
  {
    "member_id": "...",
    "member_name": "Charlie",
    "total_paid": 0.0,
    "total_owed": 35.0,
    "net_balance": -35.0
  }
]
```

### 5. Get Optimized Settlements

```bash
curl http://localhost:8000/api/v1/groups/{group_id}/settlements
```

Response:
```json
{
  "group_id": "...",
  "group_name": "Bangkok Trip",
  "settlements": [
    {
      "from_member_id": "...",
      "from_member_name": "Charlie",
      "to_member_id": "...",
      "to_member_name": "Alice",
      "amount": 35.0
    },
    {
      "from_member_id": "...",
      "from_member_name": "Bob",
      "to_member_id": "...",
      "to_member_name": "Alice",
      "amount": 5.0
    }
  ],
  "total_transactions": 2
}
```

## Design Decisions

### Architecture

- **Service Layer Pattern**: Business logic is separated into dedicated services (`BalanceCalculator`, `DebtSimplifier`, `GroupService`)
- **Dependency Injection**: FastAPI's dependency injection is used for clean, testable code
- **Pydantic Models**: Strong typing and validation using Pydantic v2

### Debt Simplification Algorithm

The debt simplification uses a **greedy algorithm**:

1. Calculate net balance for each person (total paid - total owed)
2. Separate members into creditors (positive balance) and debtors (negative balance)
3. Match the largest debtor with the largest creditor
4. Transfer the minimum of what the debtor owes and what the creditor is owed
5. Repeat until all debts are settled

This approach minimizes the number of transactions while ensuring all balances are settled.

### Data Storage

Currently uses in-memory storage for simplicity. The `GroupService` can be easily extended to use a database by:
1. Creating a repository interface
2. Implementing database-specific repositories
3. Injecting the repository into the service

## Technology Stack

- **Python 3.11+**
- **FastAPI**: Modern, fast web framework
- **Pydantic**: Data validation and serialization
- **Uvicorn**: ASGI server
- **pytest**: Testing framework
- **Docker**: Containerization

## Health Check

```bash
curl http://localhost:8000/health
```

Response:
```json
{"status": "healthy"}
```

## License

MIT License
