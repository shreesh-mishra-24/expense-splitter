"""
Unit tests for the GroupService.
"""
import pytest
from uuid import uuid4
from app.services.group_service import GroupService
from app.models import GroupCreate, MemberCreate, ExpenseCreate


class TestGroupService:
    """Tests for GroupService."""

    def setup_method(self):
        """Set up test fixtures."""
        self.service = GroupService()

    # Group operations tests
    def test_create_group(self):
        """Test creating a new group."""
        group_data = GroupCreate(name="Bangkok Trip")
        group = self.service.create_group(group_data)

        assert group.name == "Bangkok Trip"
        assert group.id is not None
        assert group.members == []
        assert group.expenses == []

    def test_get_group(self):
        """Test retrieving a group by ID."""
        group_data = GroupCreate(name="Test Group")
        created = self.service.create_group(group_data)

        retrieved = self.service.get_group(created.id)

        assert retrieved is not None
        assert retrieved.id == created.id
        assert retrieved.name == created.name

    def test_get_nonexistent_group_returns_none(self):
        """Test that getting a non-existent group returns None."""
        result = self.service.get_group(uuid4())
        assert result is None

    def test_get_all_groups(self):
        """Test retrieving all groups."""
        self.service.create_group(GroupCreate(name="Group 1"))
        self.service.create_group(GroupCreate(name="Group 2"))

        groups = self.service.get_all_groups()

        assert len(groups) == 2
        names = {g.name for g in groups}
        assert "Group 1" in names
        assert "Group 2" in names

    def test_delete_group(self):
        """Test deleting a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))

        result = self.service.delete_group(group.id)

        assert result is True
        assert self.service.get_group(group.id) is None

    def test_delete_nonexistent_group_returns_false(self):
        """Test that deleting a non-existent group returns False."""
        result = self.service.delete_group(uuid4())
        assert result is False

    # Member operations tests
    def test_add_member(self):
        """Test adding a member to a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        member_data = MemberCreate(name="Alice")

        member = self.service.add_member(group.id, member_data)

        assert member is not None
        assert member.name == "Alice"
        assert member.id is not None

        # Verify member is in group
        updated_group = self.service.get_group(group.id)
        assert len(updated_group.members) == 1
        assert updated_group.members[0].id == member.id

    def test_add_member_to_nonexistent_group(self):
        """Test that adding a member to non-existent group returns None."""
        member_data = MemberCreate(name="Alice")
        result = self.service.add_member(uuid4(), member_data)
        assert result is None

    def test_get_member(self):
        """Test retrieving a member from a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        member = self.service.add_member(group.id, MemberCreate(name="Alice"))

        retrieved = self.service.get_member(group.id, member.id)

        assert retrieved is not None
        assert retrieved.id == member.id
        assert retrieved.name == "Alice"

    def test_get_nonexistent_member(self):
        """Test that getting a non-existent member returns None."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        result = self.service.get_member(group.id, uuid4())
        assert result is None

    def test_remove_member_without_expenses(self):
        """Test removing a member who has no expenses."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        member = self.service.add_member(group.id, MemberCreate(name="Alice"))

        result = self.service.remove_member(group.id, member.id)

        assert result is True
        assert self.service.get_member(group.id, member.id) is None

    def test_remove_member_with_expenses_fails(self):
        """Test that removing a member with expenses fails."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))
        bob = self.service.add_member(group.id, MemberCreate(name="Bob"))

        # Add expense with Alice as payer
        expense_data = ExpenseCreate(
            description="Dinner",
            amount=50.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        self.service.add_expense(group.id, expense_data)

        # Try to remove Alice (should fail)
        result = self.service.remove_member(group.id, alice.id)
        assert result is False

        # Alice should still exist
        assert self.service.get_member(group.id, alice.id) is not None

    # Expense operations tests
    def test_add_expense(self):
        """Test adding an expense to a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))
        bob = self.service.add_member(group.id, MemberCreate(name="Bob"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        expense = self.service.add_expense(group.id, expense_data)

        assert expense is not None
        assert expense.description == "Dinner"
        assert expense.amount == 100.0
        assert expense.payer_id == alice.id
        assert set(expense.participant_ids) == {alice.id, bob.id}

    def test_add_expense_with_invalid_payer_fails(self):
        """Test that adding an expense with invalid payer returns None."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=uuid4(),  # Invalid payer
            participant_ids=[alice.id]
        )
        result = self.service.add_expense(group.id, expense_data)
        assert result is None

    def test_add_expense_with_invalid_participant_fails(self):
        """Test that adding an expense with invalid participant returns None."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, uuid4()]  # Invalid participant
        )
        result = self.service.add_expense(group.id, expense_data)
        assert result is None

    def test_get_expense(self):
        """Test retrieving an expense from a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id]
        )
        created = self.service.add_expense(group.id, expense_data)

        retrieved = self.service.get_expense(group.id, created.id)

        assert retrieved is not None
        assert retrieved.id == created.id

    def test_delete_expense(self):
        """Test deleting an expense from a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id]
        )
        expense = self.service.add_expense(group.id, expense_data)

        result = self.service.delete_expense(group.id, expense.id)

        assert result is True
        assert self.service.get_expense(group.id, expense.id) is None

    # Balance and settlement tests
    def test_get_balances(self):
        """Test getting balances for a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))
        bob = self.service.add_member(group.id, MemberCreate(name="Bob"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        self.service.add_expense(group.id, expense_data)

        balances = self.service.get_balances(group.id)

        assert balances is not None
        assert len(balances) == 2

        alice_balance = next(b for b in balances if b.member_id == alice.id)
        assert alice_balance.net_balance == 50.0

    def test_get_balances_for_nonexistent_group(self):
        """Test that getting balances for non-existent group returns None."""
        result = self.service.get_balances(uuid4())
        assert result is None

    def test_get_settlements(self):
        """Test getting settlements for a group."""
        group = self.service.create_group(GroupCreate(name="Test Group"))
        alice = self.service.add_member(group.id, MemberCreate(name="Alice"))
        bob = self.service.add_member(group.id, MemberCreate(name="Bob"))

        expense_data = ExpenseCreate(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        self.service.add_expense(group.id, expense_data)

        plan = self.service.get_settlements(group.id)

        assert plan is not None
        assert plan.group_id == group.id
        assert plan.total_transactions == 1
        assert plan.settlements[0].from_member_id == bob.id
        assert plan.settlements[0].to_member_id == alice.id
        assert plan.settlements[0].amount == 50.0

    def test_get_settlements_for_nonexistent_group(self):
        """Test that getting settlements for non-existent group returns None."""
        result = self.service.get_settlements(uuid4())
        assert result is None
