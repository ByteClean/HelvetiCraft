"""Minecraft server status helper using mcstatus."""
import asyncio
from mcstatus import JavaServer
try:
    from Discord_Bot.config import MC_SERVER_URL
except ModuleNotFoundError:
    from config import MC_SERVER_URL


async def get_mc_status():
    """Ping the MC server and return Online/Offline + player count.

    Uses asyncio.to_thread to avoid blocking the event loop because mcstatus
    performs network IO synchronously.
    """
    if not MC_SERVER_URL:
        return "Offline"

    def _query():
        server = JavaServer.lookup(MC_SERVER_URL)
        return server.status()

    try:
        status = await asyncio.to_thread(_query)
        return f"Online ({status.players.online}/{status.players.max})"
    except Exception:
        return "Offline"
