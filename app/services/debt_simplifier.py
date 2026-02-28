"""
Service for simplifying debts within a group.

Uses a greedy algorithm to minimize the number of transactions needed
to settle all debts within the group.
"""
from uuid import UUID
from app.models import Group, Settlement, SettlementPlan, Member
from app.services.balance_calculator import BalanceCalculator


class DebtSimplifier:
    """
    Simplifies debts to minimize the number of transactions.

    Algorithm:
    1. Calculate net balance for each person
    2. Separate into creditors (positive balance) and debtors (negative balance)
    3. Greedily match the largest debtor with the largest creditor
    4. Continue until all debts are settled
    """

    def __init__(self, balance_calculator: BalanceCalculator | None = None):
        """
        Initialize the debt simplifier.

        Args:
            balance_calculator: Optional BalanceCalculator instance (creates one if not provided)
        """
        self._balance_calculator = balance_calculator or BalanceCalculator()

    def simplify_debts(self, group: Group) -> SettlementPlan:
        """
        Calculate the minimum number of transactions needed to settle all debts.

        Args:
            group: The group to simplify debts for

        Returns:
            SettlementPlan with optimized list of settlements
        """
        if not group.members or not group.expenses:
            return SettlementPlan(
                group_id=group.id,
                group_name=group.name,
                settlements=[],
                total_transactions=0
            )

        member_map = {member.id: member for member in group.members}
        net_balances = self._balance_calculator.get_net_balances(group)

        # Separate creditors and debtors
        creditors: list[tuple[UUID, float]] = []
        debtors: list[tuple[UUID, float]] = []

        for member_id, balance in net_balances.items():
            if balance > 0.01:  # Small threshold to avoid floating point issues
                creditors.append((member_id, balance))
            elif balance < -0.01:
                debtors.append((member_id, abs(balance)))

        # Generate optimized settlements
        settlements = self._generate_settlements(creditors, debtors, member_map)

        return SettlementPlan(
            group_id=group.id,
            group_name=group.name,
            settlements=settlements,
            total_transactions=len(settlements)
        )

    def _generate_settlements(
        self,
        creditors: list[tuple[UUID, float]],
        debtors: list[tuple[UUID, float]],
        member_map: dict[UUID, Member]
    ) -> list[Settlement]:
        """
        Generate optimized settlement transactions using a greedy approach.

        Args:
            creditors: List of (member_id, amount_owed_to_them) tuples
            debtors: List of (member_id, amount_they_owe) tuples
            member_map: Dictionary mapping member IDs to Member objects

        Returns:
            List of Settlement transactions
        """
        settlements = []

        # Sort by amount (descending) for greedy matching
        creditors = sorted(creditors, key=lambda x: x[1], reverse=True)
        debtors = sorted(debtors, key=lambda x: x[1], reverse=True)

        # Convert to mutable lists with indices
        cred_amounts = {member_id: amount for member_id, amount in creditors}
        debt_amounts = {member_id: amount for member_id, amount in debtors}

        while cred_amounts and debt_amounts:
            # Find max creditor and max debtor
            max_creditor = max(cred_amounts.items(), key=lambda x: x[1])
            max_debtor = max(debt_amounts.items(), key=lambda x: x[1])

            creditor_id, credit_amount = max_creditor
            debtor_id, debt_amount = max_debtor

            # Settlement amount is the minimum of what's owed
            settlement_amount = round(min(credit_amount, debt_amount), 2)

            if settlement_amount > 0.01:  # Only create settlement if amount is significant
                settlements.append(Settlement(
                    from_member_id=debtor_id,
                    from_member_name=member_map[debtor_id].name,
                    to_member_id=creditor_id,
                    to_member_name=member_map[creditor_id].name,
                    amount=settlement_amount
                ))

            # Update remaining amounts
            cred_amounts[creditor_id] -= settlement_amount
            debt_amounts[debtor_id] -= settlement_amount

            # Remove settled parties
            if cred_amounts[creditor_id] <= 0.01:
                del cred_amounts[creditor_id]
            if debt_amounts[debtor_id] <= 0.01:
                del debt_amounts[debtor_id]

        return settlements
