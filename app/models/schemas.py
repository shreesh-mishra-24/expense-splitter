"""
Pydantic models for request/response validation and serialization.
"""
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from uuid import UUID, uuid4
from datetime import datetime


class MemberCreate(BaseModel):
    """Schema for creating a new member."""
    name: str = Field(..., min_length=1, max_length=100, description="Name of the member")


class Member(BaseModel):
    """Schema representing a group member."""
    id: UUID = Field(default_factory=uuid4, description="Unique identifier for the member")
    name: str = Field(..., min_length=1, max_length=100, description="Name of the member")

    class Config:
        json_schema_extra = {
            "example": {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "name": "Alice"
            }
        }


class ExpenseCreate(BaseModel):
    """Schema for creating a new expense."""
    description: str = Field(..., min_length=1, max_length=255, description="Description of the expense")
    amount: float = Field(..., gt=0, description="Amount of the expense (must be positive)")
    payer_id: UUID = Field(..., description="ID of the member who paid")
    participant_ids: list[UUID] = Field(
        ...,
        min_length=1,
        description="List of member IDs who participated in the expense"
    )

    @field_validator('amount')
    @classmethod
    def round_amount(cls, v: float) -> float:
        """Round amount to 2 decimal places."""
        return round(v, 2)


class Expense(BaseModel):
    """Schema representing an expense."""
    id: UUID = Field(default_factory=uuid4, description="Unique identifier for the expense")
    description: str = Field(..., description="Description of the expense")
    amount: float = Field(..., description="Amount of the expense")
    payer_id: UUID = Field(..., description="ID of the member who paid")
    participant_ids: list[UUID] = Field(..., description="List of participant member IDs")
    created_at: datetime = Field(default_factory=datetime.utcnow, description="Timestamp of creation")

    class Config:
        json_schema_extra = {
            "example": {
                "id": "123e4567-e89b-12d3-a456-426614174001",
                "description": "Dinner at Thai restaurant",
                "amount": 120.50,
                "payer_id": "123e4567-e89b-12d3-a456-426614174000",
                "participant_ids": [
                    "123e4567-e89b-12d3-a456-426614174000",
                    "123e4567-e89b-12d3-a456-426614174002"
                ],
                "created_at": "2024-01-15T10:30:00"
            }
        }


class GroupCreate(BaseModel):
    """Schema for creating a new group."""
    name: str = Field(..., min_length=1, max_length=100, description="Name of the group")


class Group(BaseModel):
    """Schema representing a group."""
    id: UUID = Field(default_factory=uuid4, description="Unique identifier for the group")
    name: str = Field(..., description="Name of the group")
    members: list[Member] = Field(default_factory=list, description="List of group members")
    expenses: list[Expense] = Field(default_factory=list, description="List of expenses")
    created_at: datetime = Field(default_factory=datetime.utcnow, description="Timestamp of creation")

    class Config:
        json_schema_extra = {
            "example": {
                "id": "123e4567-e89b-12d3-a456-426614174003",
                "name": "Bangkok Trip",
                "members": [],
                "expenses": [],
                "created_at": "2024-01-10T08:00:00"
            }
        }


class Balance(BaseModel):
    """Schema representing a member's balance within a group."""
    member_id: UUID = Field(..., description="ID of the member")
    member_name: str = Field(..., description="Name of the member")
    total_paid: float = Field(..., description="Total amount paid by the member")
    total_owed: float = Field(..., description="Total amount owed by the member")
    net_balance: float = Field(
        ...,
        description="Net balance (positive = others owe you, negative = you owe others)"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "member_id": "123e4567-e89b-12d3-a456-426614174000",
                "member_name": "Alice",
                "total_paid": 150.00,
                "total_owed": 75.00,
                "net_balance": 75.00
            }
        }


class Settlement(BaseModel):
    """Schema representing a single settlement transaction."""
    from_member_id: UUID = Field(..., description="ID of the member who should pay")
    from_member_name: str = Field(..., description="Name of the member who should pay")
    to_member_id: UUID = Field(..., description="ID of the member who should receive")
    to_member_name: str = Field(..., description="Name of the member who should receive")
    amount: float = Field(..., description="Amount to be transferred")

    class Config:
        json_schema_extra = {
            "example": {
                "from_member_id": "123e4567-e89b-12d3-a456-426614174002",
                "from_member_name": "Charlie",
                "to_member_id": "123e4567-e89b-12d3-a456-426614174000",
                "to_member_name": "Alice",
                "amount": 30.00
            }
        }


class SettlementPlan(BaseModel):
    """Schema representing the optimized settlement plan for a group."""
    group_id: UUID = Field(..., description="ID of the group")
    group_name: str = Field(..., description="Name of the group")
    settlements: list[Settlement] = Field(
        default_factory=list,
        description="List of optimized settlement transactions"
    )
    total_transactions: int = Field(..., description="Total number of transactions needed")

    class Config:
        json_schema_extra = {
            "example": {
                "group_id": "123e4567-e89b-12d3-a456-426614174003",
                "group_name": "Bangkok Trip",
                "settlements": [],
                "total_transactions": 0
            }
        }
