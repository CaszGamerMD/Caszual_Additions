"""Smoke-check the external catalog contract used by HeadDbSearch.

Run with: python3 scripts/head_db_api_smoke.py
Network outages are distinguished from schema incompatibility in the logs.
"""
import json
from urllib.request import Request, urlopen

URL = "https://headdb.net/api/v1/heads?limit=12&sort=name&direction=asc&page=1&q=melon"
try:
    with urlopen(Request(URL, headers={"User-Agent": "CaszualAdditions-CI/1.0", "Accept": "application/json"}), timeout=20) as resp:
        print("HTTP:", resp.status, "type:", resp.headers.get("Content-Type"))
        data = json.load(resp)
        print("Top-level keys:", list(data)[:20])
        print("Pagination:", data.get("pagination"))
        items = data.get("items", [])
        print("Entries:", len(items))
        if items:
            print("First entry keys:", list(items[0]))
            print("First entry metadata:", {key: val for key, val in items[0].items() if key in ("id", "name", "textureHash", "hash", "category", "texture", "textureUrl")})
        assert isinstance(items, list), "Missing items array"
        assert len(items) > 0, "Searching for melon yielded zero results"
except Exception as error:
    print("HeadDB smoke test failed:", repr(error))
    raise

print("\nCategory endpoint:")
try:
    with urlopen(Request("https://headdb.net/api/v1/categories", headers={"User-Agent":"CaszualAdditions-CI/1.0","Accept":"application/json"}), timeout=15) as resp:
        categories = json.load(resp)
        print("Category response type:", type(categories).__name__)
        if isinstance(categories, dict):
            print("Category keys:", list(categories)[:15])
            for key,value in categories.items():
                if isinstance(value,list):
                    print("Sample categories:", value[:2])
                    break
        elif isinstance(categories,list):
            print("Sample categories:", categories[:2])
except Exception as error:
    print("Category API unavailable:", str(error))
