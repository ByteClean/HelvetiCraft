"""Dummy API layer for /finance command."""

def get_finance_data_request(user_id: int):
    """Return a mock financial snapshot for the given user."""
    return {
        "user_id": user_id,
        "balance": 12345,
        "savings": 500,
        "total": 120,
    }
