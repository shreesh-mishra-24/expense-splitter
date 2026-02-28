"""
Expense Splitter Application

A REST API for managing shared expenses among groups of people,
calculating balances, and generating optimized settlement plans.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api import router

# Create FastAPI application
app = FastAPI(
    title="Expense Splitter API",
    description="""
## Overview

The Expense Splitter API helps groups manage shared expenses and calculate who owes whom.

### Features

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
- C pays A $30 (single transaction settles all balances)

### API Usage

1. Create a group using `POST /api/v1/groups`
2. Add members using `POST /api/v1/groups/{group_id}/members`
3. Record expenses using `POST /api/v1/groups/{group_id}/expenses`
4. View balances using `GET /api/v1/groups/{group_id}/balances`
5. Get settlement plan using `GET /api/v1/groups/{group_id}/settlements`
    """,
    version="1.0.0",
    contact={
        "name": "API Support",
    },
    license_info={
        "name": "MIT",
    },
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API router with version prefix
app.include_router(router, prefix="/api/v1")


@app.get("/", tags=["Health"])
def root():
    """Root endpoint returning API information."""
    return {
        "name": "Expense Splitter API",
        "version": "1.0.0",
        "docs": "/docs",
        "openapi": "/openapi.json"
    }


@app.get("/health", tags=["Health"])
def health_check():
    """Health check endpoint for container orchestration."""
    return {"status": "healthy"}
