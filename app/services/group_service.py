"""
Service for managing groups, members, and expenses.

This service acts as the main interface for group operations,
coordinating between the data storage and business logic services.
"""
from uuid import UUID
from typing import Optional
from app.models import (
    Group, GroupCreate,
    Member, MemberCreate,
    Expense, ExpenseCreate,
    Balance, SettlementPlan
)
from app.services.balance_calculator import BalanceCalculator
from app.services.debt_simplifier import DebtSimplifier


class GroupService:
    """
    Manages groups, members, and expenses.

    Uses in-memory storage for simplicity. Can be extended to use
    a database by implementing a repository pattern.
    """

    def __init__(self):
        """Initialize the service with empty storage and helper services."""
        self._groups: dict[UUID, Group] = {}
        self._balance_calculator = BalanceCalculator()
        self._debt_simplifier = DebtSimplifier(self._balance_calculator)

    # Group operations
    def create_group(self, group_data: GroupCreate) -> Group:
        """
        Create a new group.

        Args:
            group_data: Data for creating the group

        Returns:
            The created Group object
        """
        group = Group(name=group_data.name)
        self._groups[group.id] = group
        return group

    def get_group(self, group_id: UUID) -> Optional[Group]:
        """
        Get a group by ID.

        Args:
            group_id: The ID of the group to retrieve

        Returns:
            The Group object if found, None otherwise
        """
        return self._groups.get(group_id)

    def get_all_groups(self) -> list[Group]:
        """
        Get all groups.

        Returns:
            List of all Group objects
        """
        return list(self._groups.values())

    def delete_group(self, group_id: UUID) -> bool:
        """
        Delete a group by ID.

        Args:
            group_id: The ID of the group to delete

        Returns:
            True if deleted, False if not found
        """
        if group_id in self._groups:
            del self._groups[group_id]
            return True
        return False

    # Member operations
    def add_member(self, group_id: UUID, member_data: MemberCreate) -> Optional[Member]:
        """
        Add a member to a group.

        Args:
            group_id: The ID of the group
            member_data: Data for creating the member

        Returns:
            The created Member object, or None if group not found
        """
        group = self.get_group(group_id)
        if not group:
            return None

        member = Member(name=member_data.name)
        group.members.append(member)
        return member

    def get_member(self, group_id: UUID, member_id: UUID) -> Optional[Member]:
        """
        Get a member from a group.

        Args:
            group_id: The ID of the group
            member_id: The ID of the member

        Returns:
            The Member object if found, None otherwise
        """
        group = self.get_group(group_id)
        if not group:
            return None

        for member in group.members:
            if member.id == member_id:
                return member
        return None

    def remove_member(self, group_id: UUID, member_id: UUID) -> bool:
        """
        Remove a member from a group.

        Args:
            group_id: The ID of the group
            member_id: The ID of the member to remove

        Returns:
            True if removed, False if not found

        Note: This will fail if the member has any expenses
        """
        group = self.get_group(group_id)
        if not group:
            return False

        # Check if member is involved in any expenses
        for expense in group.expenses:
            if expense.payer_id == member_id or member_id in expense.participant_ids:
                return False  # Cannot remove member with expenses

        for i, member in enumerate(group.members):
            if member.id == member_id:
                group.members.pop(i)
                return True
        return False

    # Expense operations
    def add_expense(self, group_id: UUID, expense_data: ExpenseCreate) -> Optional[Expense]:
        """
        Add an expense to a group.

        Args:
            group_id: The ID of the group
            expense_data: Data for creating the expense

        Returns:
            The created Expense object, or None if validation fails
        """
        group = self.get_group(group_id)
        if not group:
            return None

        # Validate payer exists in group
        member_ids = {member.id for member in group.members}
        if expense_data.payer_id not in member_ids:
            return None

        # Validate all participants exist in group
        for participant_id in expense_data.participant_ids:
            if participant_id not in member_ids:
                return None

        expense = Expense(
            description=expense_data.description,
            amount=expense_data.amount,
            payer_id=expense_data.payer_id,
            participant_ids=expense_data.participant_ids
        )
        group.expenses.append(expense)
        return expense

    def get_expense(self, group_id: UUID, expense_id: UUID) -> Optional[Expense]:
        """
        Get an expense from a group.

        Args:
            group_id: The ID of the group
            expense_id: The ID of the expense

        Returns:
            The Expense object if found, None otherwise
        """
        group = self.get_group(group_id)
        if not group:
            return None

        for expense in group.expenses:
            if expense.id == expense_id:
                return expense
        return None

    def delete_expense(self, group_id: UUID, expense_id: UUID) -> bool:
        """
        Delete an expense from a group.

        Args:
            group_id: The ID of the group
            expense_id: The ID of the expense to delete

        Returns:
            True if deleted, False if not found
        """
        group = self.get_group(group_id)
        if not group:
            return False

        for i, expense in enumerate(group.expenses):
            if expense.id == expense_id:
                group.expenses.pop(i)
                return True
        return False

    # Balance and settlement operations
    def get_balances(self, group_id: UUID) -> Optional[list[Balance]]:
        """
        Get balances for all members in a group.

        Args:
            group_id: The ID of the group

        Returns:
            List of Balance objects, or None if group not found
        """
        group = self.get_group(group_id)
        if not group:
            return None

        return self._balance_calculator.calculate_balances(group)

    def get_settlements(self, group_id: UUID) -> Optional[SettlementPlan]:
        """
        Get optimized settlement plan for a group.

        Args:
            group_id: The ID of the group

        Returns:
            SettlementPlan with minimized transactions, or None if group not found
        """
        group = self.get_group(group_id)
        if not group:
            return None

        return self._debt_simplifier.simplify_debts(group)
