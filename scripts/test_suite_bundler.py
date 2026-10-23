"""Offline tests for the Caszual Suite JAR bundler; no GitHub token required."""
import io
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bundle_caszual_suite as bundler


def artifact_zip(entries):
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w") as archive:
        for name, content in entries.items():
            archive.writestr(name, content)
    return output.getvalue()


class FakeGitHub:
    def __init__(self, token):
        self.token = token

    def get(self, path, *, binary=False):
        repo = path.split("/")[3]
        if path.endswith("/zip"):
            return artifact_zip({f"{repo}-1.0.jar": b"fake jar data"})
        if "/actions/runs/" in path and "/artifacts?" in path:
            pattern = bundler.ARTIFACTS[repo]
            return {
                "artifacts": [{
                    "id": 400,
                    "name": pattern.replace("*", "abc123"),
                    "expired": False,
                    "created_at": "2026-10-20T00:01:00Z",
                }]
            }
        if "/actions/runs?" in path:
            # A newer successful PR is deliberately excluded.
            return {
                "workflow_runs": [
                    {"id": 100, "event": "pull_request", "head_branch": "main"},
                    {
                        "id": 200, "event": "push",
                        "head_branch": "main", "head_sha": "a" * 40,
                        "html_url": f"https://github.com/test/{repo}/actions/runs/200",
                    },
                ]
            }
        raise AssertionError(path)


class BundleTests(unittest.TestCase):
    def test_sources_and_development_jars_are_filtered(self):
        data = artifact_zip({
            "jar/suite-1.0.jar": b"real",
            "suite-1.0-sources.jar": b"source",
            "suite-1.0-dev.jar": b"dev",
            "suite-1.0-javadoc.jar": b"docs",
        })
        name, body = bundler.extract_installable_jar(data, "TestMod")
        self.assertEqual(name, "suite-1.0.jar")
        self.assertEqual(body, b"real")

    def test_golems_plus_versioned_dev_is_installable(self):
        data = artifact_zip({
            "golems-plus-0.1.0-dev.jar": b"remapped",
            "golems-plus-0.1.0-dev-dev.jar": b"unremapped",
            "golems-plus-0.1.0-dev-sources.jar": b"sources",
        })
        name, contents = bundler.extract_installable_jar(data, "golems_plus")
        self.assertEqual(name, "golems-plus-0.1.0-dev.jar")
        self.assertEqual(contents, b"remapped")
        self.assertFalse(
            bundler.is_installable_jar("golems-plus-0.1.0-dev-dev.jar", "golems_plus")
        )
        self.assertFalse(
            bundler.is_installable_jar("othermod-0.1.0-dev.jar", "OtherMod")
        )

    def test_ambiguous_jar_artifact_is_rejected(self):
        data = artifact_zip({"a.jar": b"a", "b.jar": b"b"})
        with self.assertRaisesRegex(bundler.BundlerError, "exactly one"):
            bundler.extract_installable_jar(data, "TestMod")

    def test_newest_push_artifact_and_branch_filter(self):
        api = FakeGitHub("test")
        run, artifact = bundler.newest_jar_artifact(api, "test", "PrettyFrogs", None)
        self.assertEqual(run["id"], 200)
        self.assertEqual(artifact["id"], 400)
        self.assertIsNone(bundler.newest_jar_artifact(api, "test", "PrettyFrogs", "other-branch"))

    def test_full_zip_has_eight_mods_and_checksums(self):
        with tempfile.TemporaryDirectory() as temp:
            target = Path(temp) / "Caszual-Suite-Latest.zip"
            with mock.patch.object(bundler, "find_token", return_value="fake"), mock.patch.object(
                bundler, "GitHub", FakeGitHub
            ):
                bundler.make_bundle(target, owner="test", branch=None, allow_missing=False)
            with zipfile.ZipFile(target) as archive:
                names = archive.namelist()
                self.assertEqual(len([x for x in names if x.startswith("mods/")]), 8)
                self.assertFalse(any(name.endswith(".zip") for name in names))
                manifest = json.loads(archive.read("manifest.json"))
                self.assertTrue(manifest["complete"])
                self.assertEqual(len(manifest["mods"]), 8)
                self.assertNotIn("TVtime", {x["repo"] for x in manifest["mods"]})
                self.assertTrue(all(x["branch"] == "main" for x in manifest["mods"]))
                for entry in manifest["mods"]:
                    data = archive.read("mods/" + entry["filename"])
                    self.assertEqual(bundler.hashlib.sha256(data).hexdigest(), entry["sha256"])


if __name__ == "__main__":
    unittest.main()
