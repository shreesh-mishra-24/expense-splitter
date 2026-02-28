"""
Unit tests for the DebtSimplifier service.
"""
import pytest
from app.services.debt_simplifier import DebtSimplifier
from app.services.balance_calculator import BalanceCalculator
from app.models import Group, Member, Expense


class TestDebtSimplifier:
    """Tests for DebtSimplifier."""

    def setup_method(self):
        """Set up test fixtures."""
        self.simplifier = DebtSimplifier()

    def test_empty_group_returns_empty_settlements(self):
        """Test that an empty group returns no settlements."""
        group = Group(name="Empty Group")
        plan = self.simplifier.simplify_debts(group)

        assert plan.group_id == group.id
        assert plan.group_name == group.name
        assert plan.settlements == []
        assert plan.total_transactions == 0

    def test_group_with_no_expenses(self):
        """Test that a group with no expenses returns no settlements."""
        group = Group(name="Test Group")
        group.members = [Member(name="Alice"), Member(name="Bob")]

        plan = self.simplifier.simplify_debts(group)

        assert plan.settlements == []
        assert plan.total_transactions == 0

    def test_simple_two_person_settlement(self):
        """Test settlement between two people."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        group.members = [alice, bob]

        # Alice pays $100 for both
        expense = Expense(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )
        group.expenses = [expense]

        plan = self.simplifier.simplify_debts(group)

        assert plan.total_transactions == 1
        settlement = plan.settlements[0]
        assert settlement.from_member_id == bob.id
        assert settlement.to_member_id == alice.id
        assert settlement.amount == 50.0

    def test_circular_debt_simplification(self):
        """
        Test the example from the problem statement:
        A -> paid -> B - $40
        B -> paid -> C - $40
        C -> paid -> A - $10

        Should simplify to: C pays A $30
        """
        group = Group(name="Test Group")
        a = Member(name="A")
        b = Member(name="B")
        c = Member(name="C")
        group.members = [a, b, c]

        # A pays $40 for B (A pays, only B participates)
        expense1 = Expense(
            description="A pays for B",
            amount=40.0,
            payer_id=a.id,
            participant_ids=[b.id]
        )

        # B pays $40 for C (B pays, only C participates)
        expense2 = Expense(
            description="B pays for C",
            amount=40.0,
            payer_id=b.id,
            participant_ids=[c.id]
        )

        # C pays $10 for A (C pays, only A participates)
        expense3 = Expense(
            description="C pays for A",
            amount=10.0,
            payer_id=c.id,
            participant_ids=[a.id]
        )

        group.expenses = [expense1, expense2, expense3]

        plan = self.simplifier.simplify_debts(group)

        # Net balances:
        # A: paid 40, owes 10 = net +30
        # B: paid 40, owes 40 = net 0
        # C: paid 10, owes 40 = net -30

        # Should be single transaction: C pays A $30
        assert plan.total_transactions == 1
        settlement = plan.settlements[0]
        assert settlement.from_member_id == c.id
        assert settlement.from_member_name == "C"
        assert settlement.to_member_id == a.id
        assert settlement.to_member_name == "A"
        assert settlement.amount == 30.0

    def test_multiple_settlements_needed(self):
        """Test when multiple settlements are needed."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        charlie = Member(name="Charlie")
        group.members = [alice, bob, charlie]

        # Alice pays $90 for all three
        expense = Expense(
            description="Dinner",
            amount=90.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id, charlie.id]
        )
        group.expenses = [expense]

        plan = self.simplifier.simplify_debts(group)

        # Alice: paid 90, owes 30, net +60
        # Bob: paid 0, owes 30, net -30
        # Charlie: paid 0, owes 30, net -30

        # Should have 2 transactions (Bob -> Alice and Charlie -> Alice)
        assert plan.total_transactions == 2

        total_to_alice = sum(
            s.amount for s in plan.settlements
            if s.to_member_id == alice.id
        )
        assert total_to_alice == 60.0

    def test_balanced_group_no_settlements(self):
        """Test that a balanced group needs no settlements."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        group.members = [alice, bob]

        # Alice pays $50, both participate
        expense1 = Expense(
            description="Lunch",
            amount=50.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id]
        )

        # Bob pays $50, both participate
        expense2 = Expense(
            description="Dinner",
            amount=50.0,
            payer_id=bob.id,
            participant_ids=[alice.id, bob.id]
        )

        group.expenses = [expense1, expense2]

        plan = self.simplifier.simplify_debts(group)

        # Both have net balance of 0
        assert plan.total_transactions == 0
        assert plan.settlements == []

    def test_greedy_optimization_reduces_transactions(self):
        """Test that the greedy algorithm minimizes transactions."""
        group = Group(name="Test Group")
        members = [Member(name=f"Person{i}") for i in range(4)]
        group.members = members

        # Person0 pays $120 for all 4
        expense1 = Expense(
            description="Big dinner",
            amount=120.0,
            payer_id=members[0].id,
            participant_ids=[m.id for m in members]
        )

        # Person1 pays $40 for Person1 and Person2
        expense2 = Expense(
            description="Taxi",
            amount=40.0,
            payer_id=members[1].id,
            participant_ids=[members[1].id, members[2].id]
        )

        group.expenses = [expense1, expense2]

        plan = self.simplifier.simplify_debts(group)

        # Net balances:
        # P0: paid 120, owes 30 = +90
        # P1: paid 40, owes 30+20=50 = -10
        # P2: paid 0, owes 30+20=50 = -50
        # P3: paid 0, owes 30 = -30

        # Without optimization: could be many transactions
        # With optimization: should be minimal
        assert plan.total_transactions <= 3

        # Verify total amount transferred equals the total owed
        total_transferred = sum(s.amount for s in plan.settlements)
        assert abs(total_transferred - 90.0) < 0.01  # P0 should receive 90

    def test_settlement_amounts_are_rounded(self):
        """Test that settlement amounts are properly rounded."""
        group = Group(name="Test Group")
        alice = Member(name="Alice")
        bob = Member(name="Bob")
        charlie = Member(name="Charlie")
        group.members = [alice, bob, charlie]

        # $100 split 3 ways = 33.33... each
        expense = Expense(
            description="Dinner",
            amount=100.0,
            payer_id=alice.id,
            participant_ids=[alice.id, bob.id, charlie.id]
        )
        group.expenses = [expense]

        plan = self.simplifier.simplify_debts(group)

        # Check all amounts are rounded to 2 decimal places
        for settlement in plan.settlements:
            assert settlement.amount == round(settlement.amount, 2)
