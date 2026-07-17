"""
Shared pytest fixtures for anvesha-chem tests.
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app


@pytest.fixture(scope="module")
def client() -> TestClient:
    """FastAPI TestClient shared across the test module."""
    with TestClient(app) as c:
        yield c
