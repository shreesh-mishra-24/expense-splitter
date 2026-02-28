"""
Dependency injection for API routes.

This module provides singleton instances of services that can be
injected into route handlers using FastAPI's dependency injection system.
"""
from functools import lru_cache
from app.services import GroupService


@lru_cache()
def get_group_service() -> GroupService:
    """
    Get the singleton GroupService instance.

    Returns:
        The GroupService singleton
    """
    return GroupService()
