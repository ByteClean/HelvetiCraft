"""Dummy API layer for /initiative commands."""

def create_initiative_request(user_id: int, title: str, description: str):
    """Pretend to create a new initiative and return a fake record."""
    return {
        "id": 42,  # placeholder id
        "user_id": user_id,
        "title": title,
        "description": description,
    }


def get_user_initiatives_request(user_id: int):
    """Pretend to fetch initiatives owned by a user."""
    # Simulate different responses for different users
    if user_id % 2 == 0:
        return [
            {"id": 1, "title": "Even Hero Project", "description": "A sample initiative for even IDs"},
            {"id": 2, "title": "Second Example", "description": "Testing initiative listing"},
        ]
    else:
        # Return empty list for odd user IDs
        return []
