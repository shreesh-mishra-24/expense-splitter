"""
REST API routes for the expense splitter application.

This module defines all the API endpoints for managing groups, members,
expenses, and calculating settlements.
"""
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, status
from app.models import (
    Group, GroupCreate,
    Member, MemberCreate,
    Expense, ExpenseCreate,
    Balance, SettlementPlan
)
from app.services import GroupService
from app.api.dependencies import get_group_service

router = APIRouter()


# =============================================================================
# Group endpoints
# =============================================================================

@router.post(
    "/groups",
    response_model=Group,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new group",
    description="Creates a new expense-sharing group.",
    tags=["Groups"]
)
def create_group(
    group_data: GroupCreate,
    service: GroupService = Depends(get_group_service)
) -> Group:
    """Create a new group for tracking shared expenses."""
    return service.create_group(group_data)


@router.get(
    "/groups",
    response_model=list[Group],
    summary="Get all groups",
    description="Retrieves all expense-sharing groups.",
    tags=["Groups"]
)
def get_all_groups(
    service: GroupService = Depends(get_group_service)
) -> list[Group]:
    """Get all groups."""
    return service.get_all_groups()


@router.get(
    "/groups/{group_id}",
    response_model=Group,
    summary="Get a group by ID",
    description="Retrieves a specific group with all its members and expenses.",
    tags=["Groups"]
)
def get_group(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> Group:
    """Get a specific group by its ID."""
    group = service.get_group(group_id)
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return group


@router.delete(
    "/groups/{group_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a group",
    description="Deletes a group and all associated data.",
    tags=["Groups"]
)
def delete_group(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> None:
    """Delete a group and all its data."""
    if not service.delete_group(group_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )


# =============================================================================
# Member endpoints
# =============================================================================

@router.post(
    "/groups/{group_id}/members",
    response_model=Member,
    status_code=status.HTTP_201_CREATED,
    summary="Add a member to a group",
    description="Adds a new member to an existing group.",
    tags=["Members"]
)
def add_member(
    group_id: UUID,
    member_data: MemberCreate,
    service: GroupService = Depends(get_group_service)
) -> Member:
    """Add a new member to a group."""
    member = service.add_member(group_id, member_data)
    if not member:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return member


@router.get(
    "/groups/{group_id}/members",
    response_model=list[Member],
    summary="Get all members in a group",
    description="Retrieves all members of a specific group.",
    tags=["Members"]
)
def get_members(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> list[Member]:
    """Get all members of a group."""
    group = service.get_group(group_id)
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return group.members


@router.get(
    "/groups/{group_id}/members/{member_id}",
    response_model=Member,
    summary="Get a member by ID",
    description="Retrieves a specific member from a group.",
    tags=["Members"]
)
def get_member(
    group_id: UUID,
    member_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> Member:
    """Get a specific member from a group."""
    member = service.get_member(group_id, member_id)
    if not member:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Member with ID {member_id} not found in group {group_id}"
        )
    return member


@router.delete(
    "/groups/{group_id}/members/{member_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Remove a member from a group",
    description="Removes a member from a group. Will fail if the member has any expenses.",
    tags=["Members"]
)
def remove_member(
    group_id: UUID,
    member_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> None:
    """Remove a member from a group."""
    if not service.remove_member(group_id, member_id):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot remove member {member_id}. Either the member doesn't exist or has associated expenses."
        )


# =============================================================================
# Expense endpoints
# =============================================================================

@router.post(
    "/groups/{group_id}/expenses",
    response_model=Expense,
    status_code=status.HTTP_201_CREATED,
    summary="Add an expense to a group",
    description="Records a new expense within the group. The payer and all participants must be existing group members.",
    tags=["Expenses"]
)
def add_expense(
    group_id: UUID,
    expense_data: ExpenseCreate,
    service: GroupService = Depends(get_group_service)
) -> Expense:
    """Add a new expense to a group."""
    expense = service.add_expense(group_id, expense_data)
    if not expense:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid expense data. Ensure the group exists and all member IDs (payer and participants) are valid."
        )
    return expense


@router.get(
    "/groups/{group_id}/expenses",
    response_model=list[Expense],
    summary="Get all expenses in a group",
    description="Retrieves all expenses recorded in a group.",
    tags=["Expenses"]
)
def get_expenses(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> list[Expense]:
    """Get all expenses in a group."""
    group = service.get_group(group_id)
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return group.expenses


@router.get(
    "/groups/{group_id}/expenses/{expense_id}",
    response_model=Expense,
    summary="Get an expense by ID",
    description="Retrieves a specific expense from a group.",
    tags=["Expenses"]
)
def get_expense(
    group_id: UUID,
    expense_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> Expense:
    """Get a specific expense from a group."""
    expense = service.get_expense(group_id, expense_id)
    if not expense:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Expense with ID {expense_id} not found in group {group_id}"
        )
    return expense


@router.delete(
    "/groups/{group_id}/expenses/{expense_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete an expense",
    description="Removes an expense from a group.",
    tags=["Expenses"]
)
def delete_expense(
    group_id: UUID,
    expense_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> None:
    """Delete an expense from a group."""
    if not service.delete_expense(group_id, expense_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Expense with ID {expense_id} not found in group {group_id}"
        )


# =============================================================================
# Balance and Settlement endpoints
# =============================================================================

@router.get(
    "/groups/{group_id}/balances",
    response_model=list[Balance],
    summary="Get balances for all members",
    description="Calculates and returns the balance for each member, showing total paid, total owed, and net balance.",
    tags=["Balances & Settlements"]
)
def get_balances(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> list[Balance]:
    """Get the balance for each member in a group."""
    balances = service.get_balances(group_id)
    if balances is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return balances


@router.get(
    "/groups/{group_id}/settlements",
    response_model=SettlementPlan,
    summary="Get optimized settlement plan",
    description="""
    Calculates the minimum number of transactions needed to settle all debts within the group.

    The algorithm simplifies debts so that instead of multiple individual reimbursements,
    only the minimum necessary transactions are returned.

    Example:
    - A paid B $40
    - B paid C $40
    - C paid A $10

    Instead of three separate repayments, the simplified result is:
    - C pays A $30 (single transaction settles all balances)
    """,
    tags=["Balances & Settlements"]
)
def get_settlements(
    group_id: UUID,
    service: GroupService = Depends(get_group_service)
) -> SettlementPlan:
    """Get the optimized settlement plan for a group."""
    settlements = service.get_settlements(group_id)
    if settlements is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Group with ID {group_id} not found"
        )
    return settlements
