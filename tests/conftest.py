"""
Pytest configuration and fixtures for testing.
"""
import pytest
from uuid import uuid4
from fastapi.testclient import TestClient
from app.main import app
from app.services import GroupService
from app.models import Group, Member, Expense, GroupCreate, MemberCreate, ExpenseCreate
from app.api.dependencies import get_group_service


@pytest.fixture
def test_client():
    """Create a test client with a fresh GroupService for each test."""
    # Create a fresh service for each test
    test_service = GroupService()

    # Override the dependency
    app.dependency_overrides[get_group_service] = lambda: test_service

    with TestClient(app) as client:
        yield client

    # Clear the override after the test
    app.dependency_overrides.clear()


@pytest.fixture
def group_service():
    """Create a fresh GroupService for unit testing."""
    return GroupService()


@pytest.fixture
def sample_group():
    """Create a sample group with members for testing."""
    group = Group(name="Test Group")
    group.members = [
        Member(name="Alice"),
        Member(name="Bob"),
        Member(name="Charlie")
    ]
    return group


@pytest.fixture
def sample_group_with_expenses(sample_group):
    """Create a sample group with members and expenses for testing."""
    alice, bob, charlie = sample_group.members

    # Alice pays $60 for dinner (shared by all three)
    expense1 = Expense(
        description="Dinner",
        amount=60.0,
        payer_id=alice.id,
        participant_ids=[alice.id, bob.id, charlie.id]
    )

    # Bob pays $30 for taxi (shared by Bob and Charlie)
    expense2 = Expense(
        description="Taxi",
        amount=30.0,
        payer_id=bob.id,
        participant_ids=[bob.id, charlie.id]
    )

    sample_group.expenses = [expense1, expense2]
    return sample_group
