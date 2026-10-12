"""Test the Fun Barrel's shared Caszual item-tag integration without Minecraft runtime."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/caszgamermd/caszualadditions"
TAG = ROOT / "src/main/resources/data/caszual_additions/tags/item/caszual_content.json"


class FunBarrelTagSmokeTest(unittest.TestCase):
    def test_tag_exists_and_accepts_companion_contributions(self):
        tag = json.loads(TAG.read_text(encoding="utf-8"))
        self.assertFalse(tag["replace"])
        self.assertTrue(tag["values"])

    def test_shared_tag_is_read_in_catalogue(self):
        tags = (SRC / "CaszualTags.java").read_text(encoding="utf-8")
        self.assertIn('CaszualAdditions.id("caszual_content")', tags)
        barrel = (SRC / "funbarrel/FunBarrelBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn("CaszualTags.CASZUAL_CONTENT", barrel)
        self.assertIn("item.getDefaultInstance().is(CaszualTags.CASZUAL_CONTENT)", barrel)
        self.assertIn("caszualFamily(id) ||", barrel,
                      "Older Caszual mods still work without adding resource-pack tags")

    def test_existing_barrels_refresh_without_duplicate_refills(self):
        block = (SRC / "funbarrel/FunBarrelBlock.java").read_text(encoding="utf-8")
        barrel = (SRC / "funbarrel/FunBarrelBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn("barrel.refreshCatalog()", block)
        self.assertIn("if (cataloged.contains(id)) continue;", barrel)
        self.assertIn("cataloged.add(id)", barrel)
        self.assertIn("if (changed) setChanged();", barrel)


if __name__ == "__main__":
    unittest.main()
