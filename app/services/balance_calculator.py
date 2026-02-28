"""
Service for calculating balances within a group.
"""
from uuid import UUID
from app.models import Group, Balance, Expense, Member


class BalanceCalculator:
    """
    Calculates individual and group balances based on expenses.

    The balance calculation considers:
    - Total amount paid by each member
    - Total amount owed by each member (their share of expenses they participated in)
    - Net balance (paid - owed)
    """

    def calculate_balances(self, group: Group) -> list[Balance]:
        """
        Calculate the balance for each member in the group.

        Args:
            group: The group to calculate balances for

        Returns:
            List of Balance objects for each member
        """
        if not group.members:
            return []

        member_map = {member.id: member for member in group.members}

        # Initialize tracking dictionaries
        total_paid: dict[UUID, float] = {member.id: 0.0 for member in group.members}
        total_owed: dict[UUID, float] = {member.id: 0.0 for member in group.members}

        # Process each expense
        for expense in group.expenses:
            self._process_expense(expense, total_paid, total_owed)

        # Create balance objects
        balances = []
        for member in group.members:
            paid = round(total_paid[member.id], 2)
            owed = round(total_owed[member.id], 2)
            net = round(paid - owed, 2)

            balances.append(Balance(
                member_id=member.id,
                member_name=member.name,
                total_paid=paid,
                total_owed=owed,
                net_balance=net
            ))

        return balances

    def _process_expense(
        self,
        expense: Expense,
        total_paid: dict[UUID, float],
        total_owed: dict[UUID, float]
    ) -> None:
        """
        Process a single expense and update the running totals.

        Args:
            expense: The expense to process
            total_paid: Dictionary tracking total paid by each member
            total_owed: Dictionary tracking total owed by each member
        """
        # Credit the payer
        if expense.payer_id in total_paid:
            total_paid[expense.payer_id] += expense.amount

        # Calculate each participant's share
        num_participants = len(expense.participant_ids)
        if num_participants == 0:
            return

        share_per_person = expense.amount / num_participants

        # Debit each participant
        for participant_id in expense.participant_ids:
            if participant_id in total_owed:
                total_owed[participant_id] += share_per_person

    def get_net_balances(self, group: Group) -> dict[UUID, float]:
        """
        Get simplified net balances for debt simplification.

        Args:
            group: The group to get net balances for

        Returns:
            Dictionary mapping member_id to net balance
        """
        balances = self.calculate_balances(group)
        return {balance.member_id: balance.net_balance for balance in balances}
