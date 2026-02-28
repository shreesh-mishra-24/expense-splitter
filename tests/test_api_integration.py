"""
Integration tests for the REST API endpoints.

These tests verify the complete request/response cycle through the API.
"""
import pytest
from fastapi.testclient import TestClient


class TestGroupEndpoints:
    """Integration tests for group endpoints."""

    def test_create_group(self, test_client: TestClient):
        """Test POST /api/v1/groups creates a new group."""
        response = test_client.post(
            "/api/v1/groups",
            json={"name": "Bangkok Trip"}
        )

        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "Bangkok Trip"
        assert "id" in data
        assert data["members"] == []
        assert data["expenses"] == []

    def test_get_all_groups(self, test_client: TestClient):
        """Test GET /api/v1/groups returns all groups."""
        # Create two groups
        test_client.post("/api/v1/groups", json={"name": "Group 1"})
        test_client.post("/api/v1/groups", json={"name": "Group 2"})

        response = test_client.get("/api/v1/groups")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2

    def test_get_group_by_id(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id} returns specific group."""
        create_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = create_response.json()["id"]

        response = test_client.get(f"/api/v1/groups/{group_id}")

        assert response.status_code == 200
        assert response.json()["id"] == group_id

    def test_get_nonexistent_group_returns_404(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id} returns 404 for non-existent group."""
        response = test_client.get(
            "/api/v1/groups/00000000-0000-0000-0000-000000000000"
        )
        assert response.status_code == 404

    def test_delete_group(self, test_client: TestClient):
        """Test DELETE /api/v1/groups/{id} removes a group."""
        create_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = create_response.json()["id"]

        delete_response = test_client.delete(f"/api/v1/groups/{group_id}")

        assert delete_response.status_code == 204

        # Verify group is deleted
        get_response = test_client.get(f"/api/v1/groups/{group_id}")
        assert get_response.status_code == 404


class TestMemberEndpoints:
    """Integration tests for member endpoints."""

    def test_add_member(self, test_client: TestClient):
        """Test POST /api/v1/groups/{id}/members adds a member."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )

        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "Alice"
        assert "id" in data

    def test_get_members(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id}/members returns all members."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        test_client.post(f"/api/v1/groups/{group_id}/members", json={"name": "Alice"})
        test_client.post(f"/api/v1/groups/{group_id}/members", json={"name": "Bob"})

        response = test_client.get(f"/api/v1/groups/{group_id}/members")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2
        names = {m["name"] for m in data}
        assert "Alice" in names
        assert "Bob" in names

    def test_get_member_by_id(self, test_client: TestClient):
        """Test GET /api/v1/groups/{gid}/members/{mid} returns specific member."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        member_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        member_id = member_response.json()["id"]

        response = test_client.get(
            f"/api/v1/groups/{group_id}/members/{member_id}"
        )

        assert response.status_code == 200
        assert response.json()["name"] == "Alice"

    def test_remove_member(self, test_client: TestClient):
        """Test DELETE /api/v1/groups/{gid}/members/{mid} removes member."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        member_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        member_id = member_response.json()["id"]

        delete_response = test_client.delete(
            f"/api/v1/groups/{group_id}/members/{member_id}"
        )

        assert delete_response.status_code == 204


class TestExpenseEndpoints:
    """Integration tests for expense endpoints."""

    def test_add_expense(self, test_client: TestClient):
        """Test POST /api/v1/groups/{id}/expenses adds an expense."""
        # Create group and members
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        bob_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Bob"}
        )
        bob_id = bob_response.json()["id"]

        # Add expense
        response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id, bob_id]
            }
        )

        assert response.status_code == 201
        data = response.json()
        assert data["description"] == "Dinner"
        assert data["amount"] == 100.0
        assert data["payer_id"] == alice_id

    def test_add_expense_with_invalid_member_returns_400(
        self, test_client: TestClient
    ):
        """Test adding expense with invalid member ID returns 400."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": "00000000-0000-0000-0000-000000000000",  # Invalid
                "participant_ids": [alice_id]
            }
        )

        assert response.status_code == 400

    def test_get_expenses(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id}/expenses returns all expenses."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        # Add two expenses
        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id]
            }
        )
        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Taxi",
                "amount": 50.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id]
            }
        )

        response = test_client.get(f"/api/v1/groups/{group_id}/expenses")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2

    def test_delete_expense(self, test_client: TestClient):
        """Test DELETE /api/v1/groups/{gid}/expenses/{eid} removes expense."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        expense_response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id]
            }
        )
        expense_id = expense_response.json()["id"]

        delete_response = test_client.delete(
            f"/api/v1/groups/{group_id}/expenses/{expense_id}"
        )

        assert delete_response.status_code == 204


class TestBalanceAndSettlementEndpoints:
    """Integration tests for balance and settlement endpoints."""

    def test_get_balances(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id}/balances returns balances."""
        # Setup group with members and expenses
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        bob_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Bob"}
        )
        bob_id = bob_response.json()["id"]

        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id, bob_id]
            }
        )

        response = test_client.get(f"/api/v1/groups/{group_id}/balances")

        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2

        alice_balance = next(b for b in data if b["member_id"] == alice_id)
        bob_balance = next(b for b in data if b["member_id"] == bob_id)

        assert alice_balance["net_balance"] == 50.0
        assert bob_balance["net_balance"] == -50.0

    def test_get_settlements(self, test_client: TestClient):
        """Test GET /api/v1/groups/{id}/settlements returns settlement plan."""
        # Setup group with members and expenses
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        bob_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Bob"}
        )
        bob_id = bob_response.json()["id"]

        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Dinner",
                "amount": 100.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id, bob_id]
            }
        )

        response = test_client.get(f"/api/v1/groups/{group_id}/settlements")

        assert response.status_code == 200
        data = response.json()
        assert data["group_id"] == group_id
        assert data["total_transactions"] == 1
        assert len(data["settlements"]) == 1
        assert data["settlements"][0]["from_member_id"] == bob_id
        assert data["settlements"][0]["to_member_id"] == alice_id
        assert data["settlements"][0]["amount"] == 50.0

    def test_circular_debt_simplification_integration(
        self, test_client: TestClient
    ):
        """
        Integration test for the problem statement example:
        A -> paid -> B - $40
        B -> paid -> C - $40
        C -> paid -> A - $10

        Should simplify to: C pays A $30
        """
        # Create group
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Circular Debt Test"}
        )
        group_id = group_response.json()["id"]

        # Add members A, B, C
        a_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "A"}
        )
        a_id = a_response.json()["id"]

        b_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "B"}
        )
        b_id = b_response.json()["id"]

        c_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "C"}
        )
        c_id = c_response.json()["id"]

        # A pays $40 for B (only B participates)
        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "A pays for B",
                "amount": 40.0,
                "payer_id": a_id,
                "participant_ids": [b_id]
            }
        )

        # B pays $40 for C (only C participates)
        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "B pays for C",
                "amount": 40.0,
                "payer_id": b_id,
                "participant_ids": [c_id]
            }
        )

        # C pays $10 for A (only A participates)
        test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "C pays for A",
                "amount": 10.0,
                "payer_id": c_id,
                "participant_ids": [a_id]
            }
        )

        # Get settlement plan
        response = test_client.get(f"/api/v1/groups/{group_id}/settlements")

        assert response.status_code == 200
        data = response.json()

        # Should be simplified to single transaction: C pays A $30
        assert data["total_transactions"] == 1
        settlement = data["settlements"][0]
        assert settlement["from_member_name"] == "C"
        assert settlement["to_member_name"] == "A"
        assert settlement["amount"] == 30.0


class TestHealthEndpoints:
    """Integration tests for health check endpoints."""

    def test_root_endpoint(self, test_client: TestClient):
        """Test GET / returns API info."""
        response = test_client.get("/")

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Expense Splitter API"
        assert "version" in data

    def test_health_endpoint(self, test_client: TestClient):
        """Test GET /health returns healthy status."""
        response = test_client.get("/health")

        assert response.status_code == 200
        assert response.json()["status"] == "healthy"


class TestValidation:
    """Integration tests for input validation."""

    def test_create_group_empty_name_fails(self, test_client: TestClient):
        """Test that creating a group with empty name fails."""
        response = test_client.post("/api/v1/groups", json={"name": ""})
        assert response.status_code == 422

    def test_add_expense_negative_amount_fails(self, test_client: TestClient):
        """Test that adding expense with negative amount fails."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Invalid",
                "amount": -50.0,
                "payer_id": alice_id,
                "participant_ids": [alice_id]
            }
        )

        assert response.status_code == 422

    def test_add_expense_zero_amount_fails(self, test_client: TestClient):
        """Test that adding expense with zero amount fails."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Invalid",
                "amount": 0,
                "payer_id": alice_id,
                "participant_ids": [alice_id]
            }
        )

        assert response.status_code == 422

    def test_add_expense_empty_participants_fails(
        self, test_client: TestClient
    ):
        """Test that adding expense with no participants fails."""
        group_response = test_client.post(
            "/api/v1/groups",
            json={"name": "Test Group"}
        )
        group_id = group_response.json()["id"]

        alice_response = test_client.post(
            f"/api/v1/groups/{group_id}/members",
            json={"name": "Alice"}
        )
        alice_id = alice_response.json()["id"]

        response = test_client.post(
            f"/api/v1/groups/{group_id}/expenses",
            json={
                "description": "Invalid",
                "amount": 50.0,
                "payer_id": alice_id,
                "participant_ids": []
            }
        )

        assert response.status_code == 422
