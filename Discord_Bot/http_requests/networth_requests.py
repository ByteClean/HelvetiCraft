"""Dummy API layer for /networth command."""

def get_all_networths_request():
    """Return a mock list of players with fake networths."""
    return [
        {"name": "Steve", "amount": 1500},
        {"name": "Alex", "amount": 1200},
        {"name": "Herobrine", "amount": 666},
    ]
