"""
Unit tests for the BalanceCalculator service.
"""
import pytest
from uuid import uuid4
from app.services.balance_calculator import BalanceCalculator
from app.models import Group, Member, Expense


class TestBalanceCalculator:
    """Tests for BalanceCalculator."""

    def setup_method(self):
        """Set up test fixtures."""
        self.calculator = BalanceCalculator()

    def test_empty_group_returns_empty_balances(self):
        """Test that an empty group returns no balances."""
        group = Group(name="Empty Group")
        balances = self.calculator.calculate_balances(group)
        assert balances == []

    def test_group_with_no_expenses(self):
        """Test that a group with members but no expenses has zero balances."""
        group = Group(name="Test Group")
        group.members = [Member(name="Alice"), Member(name="Bob")]

        balances = self.calculator.calculate_balances(group)

        assert len(balances) == 2
        for balance in balances:
            assert balance.total_paid == 0.0
            assert balance.total_owed == 0.0
            assert balance.net_balance == 0.0

    def test_single_expense_equal_split(self):
        """Test balance calculation for a single expense split equally."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        group.members = [alice, bob]

        # Alice pays $100 split between both
        expense = Expense(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        group.expenses = [expense]

        balances = self.calculator.calculate_balances(group)

        alice_balance = next(b for b in balances if b.member_id == alice.id)
        bob_balance = next(b for b in balances if b.member_id == bob.id)

        # Alice paid 100, owes 50, net +50
        assert alice_balance.total_paid == 100.0
        assert alice_balance.total_owed == 50.0
        assert alice_balance.net_balance == 50.0

        # Bob paid 0, owes 50, net -50
        assert bob_balance.total_paid == 0.0
        assert bob_balance.total_owed == 50.0
        assert bob_balance.net_balance == -50.0

    def test_multiple_expenses(self):
        """Test balance calculation with multiple expenses."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        charlie = Member(name="Charlie")
        group.members = [alice, bob, charlie]

        # Alice pays $60 for dinner (all three participate)
        expense1 = Expense(
            description="Dinner",
            amount=60.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id, charlie.id]
        )

        # Bob pays $30 for taxi (only Bob and Charlie)
        expense2 = Expense(
            description="Taxi",
            amount=30.0,
            payer_id=bob.id,
            participant_ids=[bob.id, charlie.id]
        )

        group.expenses = [expense1, expense2]

        balances = self.calculator.calculate_balances(group)

        alice_balance = next(b for b in balances if b.member_id == alice.id)
        bob_balance = next(b for b in balances if b.member_id == bob.id)
        charlie_balance = next(b for b in balances if b.member_id == charlie.id)

        # Alice: paid 60, owes 20 (60/3), net +40
        assert alice_balance.total_paid == 60.0
        assert alice_balance.total_owed == 20.0
        assert alice_balance.net_balance == 40.0

        # Bob: paid 30, owes 20 (60/3) + 15 (30/2) = 35, net -5
        assert bob_balance.total_paid == 30.0
        assert bob_balance.total_owed == 35.0
        assert bob_balance.net_balance == -5.0

        # Charlie: paid 0, owes 20 + 15 = 35, net -35
        assert charlie_balance.total_paid == 0.0
        assert charlie_balance.total_owed == 35.0
        assert charlie_balance.net_balance == -35.0

    def test_expense_with_unequal_participants(self):
        """Test expense where payer is not a participant."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        group.members = [alice, bob]

        # Alice pays $50 for Bob only
        expense = Expense(
            description="Gift for Bob",
            amount=50.0,
            payer_id=alice.id,
            participant_ids=[bob.id]
        )
        group.expenses = [expense]

        balances = self.calculator.calculate_balances(group)

        alice_balance = next(b for b in balances if b.member_id == alice.id)
        bob_balance = next(b for b in balances if b.member_id == bob.id)

        # Alice: paid 50, owes 0, net +50
        assert alice_balance.total_paid == 50.0
        assert alice_balance.total_owed == 0.0
        assert alice_balance.net_balance == 50.0

        # Bob: paid 0, owes 50, net -50
        assert bob_balance.total_paid == 0.0
        assert bob_balance.total_owed == 50.0
        assert bob_balance.net_balance == -50.0

    def test_sum_of_balances_is_zero(self):
        """Test that the sum of all net balances is zero (conservation)."""
        group = Group(name="Test Group")
        members = [Member(name=f"Person{i}") for i in range(5)]
        group.members = members

        # Create various expenses
        expenses = [
            Expense(
                description="Expense 1",
                amount=100.0,
                payer_id=members[0].id,
                participant_ids=[m.id for m in members]
            ),
            Expense(
                description="Expense 2",
                amount=75.0,
                payer_id=members[2].id,
                participant_ids=[members[0].id, members[2].id, members[4].id]
            ),
            Expense(
                description="Expense 3",
                amount=50.0,
                payer_id=members[4].id,
                participant_ids=[members[1].id, members[3].id]
            ),
        ]
        group.expenses = expenses

        balances = self.calculator.calculate_balances(group)
        total_net = sum(b.net_balance for b in balances)

        # Allow small floating point error
        assert abs(total_net) < 0.01

    def test_get_net_balances(self):
        """Test the get_net_balances helper method."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        group.members = [alice, bob]

        expense = Expense(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        group.expenses = [expense]

        net_balances = self.calculator.get_net_balances(group)

        assert alice.id in net_balances
        assert bob.id in net_balances
        assert net_balances[alice.id] == 50.0
        assert net_balances[bob.id] == -50.0
