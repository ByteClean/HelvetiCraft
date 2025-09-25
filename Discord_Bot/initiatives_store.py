"""Simple initiatives store for the Discord bot.

This is an in-memory store for now. It's structured so you can later
replace it with a persistent backend (sqlite, json file, etc.).
"""
from typing import Dict, Any
import itertools

_counter = itertools.count(1)
_initiatives: Dict[int, Dict[str, Any]] = {}

def create_initiative(author_id: int, title: str, description: str) -> Dict[str, Any]:
    id_ = next(_counter)
    item = {
        "id": id_,
        "author_id": author_id,
        "title": title,
        "description": description,
        "status": "open",
    }
    _initiatives[id_] = item
    return item

def list_initiatives():
    return list(_initiatives.values())

def get_initiative(id_: int):
    return _initiatives.get(id_)
