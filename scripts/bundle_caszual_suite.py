#!/usr/bin/env python3
"""Bundle the newest successfully packaged JAR from each Caszual Suite repository.

Requires Python 3.10+ and GitHub authentication with Actions:read access to
all listed private repositories. No third-party Python dependencies.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

# Each pattern identifies the installable JAR artifact, not unrelated build
# artifacts such as smoke-test logs or Pretty Frogs Blockbench assets.
ARTIFACTS = {
    "Caszual_Additions": "Caszual-Additions-*",
    "PrettyFrogs": "PrettyFrogs-1.0.0",
    "Caszual_MTG_addon": "caszual-mtg-*",
    "Caszual_Aquariums": "linked-aquariums-build",
    "MC-MiningRig": "miningrig-26.2",
    "Ore_Laundering": "Ore-Laundering-Fabric-26.2",
    "Caszual_Pallets_Plus": "Caszual-Pallets-Plus",
    "golems_plus": "golems-plus-dev",
}
MAX_ARTIFACT_BYTES = 80 * 1024 * 1024
SKIP_JAR = re.compile(
    r"(?i)(?:-sources|-javadoc|-dev|-test|-tests|-plain)\.jar$"
)


class BundlerError(Exception):
    pass


class AuthSafeRedirect(urllib.request.HTTPRedirectHandler):
    """Do not forward the GitHub token to artifact storage redirects."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        if redirected and urllib.parse.urlsplit(req.full_url).hostname != urllib.parse.urlsplit(newurl).hostname:
            redirected.remove_header("Authorization")
        return redirected


def find_token() -> str:
    for env in ("CASZUAL_SUITE_TOKEN", "GH_TOKEN", "GITHUB_TOKEN"):
        if os.environ.get(env):
            return os.environ[env].strip()
    try:
        token = subprocess.check_output(
            ["gh", "auth", "token"], text=True, stderr=subprocess.DEVNULL, timeout=10
        ).strip()
        if token:
            return token
    except (OSError, subprocess.SubprocessError):
        pass
    raise BundlerError(
        "No GitHub authentication found. Run 'gh auth login' on your PC, "
        "or set CASZUAL_SUITE_TOKEN with Actions:read for all Suite repositories."
    )


class GitHub:
    def __init__(self, token: str):
        self.token = token
        self.opener = urllib.request.build_opener(AuthSafeRedirect())

    def get(self, path: str, *, binary: bool = False):
        url = "https://api.github.com" + path
        req = urllib.request.Request(
            url,
            headers={
                "Authorization": "Bearer " + self.token,
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
                "User-Agent": "Caszual-Suite-JAR-Bundler",
            },
        )
        try:
            with self.opener.open(req, timeout=90) as resp:
                data = resp.read(MAX_ARTIFACT_BYTES + 1)
        except urllib.error.HTTPError as exc:
            detail = exc.read(500).decode("utf-8", errors="replace")
            raise BundlerError(f"GitHub HTTP {exc.code} for {path}: {detail}") from exc
        except (urllib.error.URLError, TimeoutError) as exc:
            raise BundlerError(f"GitHub request failed for {path}: {exc}") from exc
        if len(data) > MAX_ARTIFACT_BYTES:
            raise BundlerError(f"Artifact/API response too large for {path}")
        if binary:
            return data
        try:
            return json.loads(data)
        except ValueError as exc:
            raise BundlerError(f"Unexpected GitHub API response for {path}") from exc


def newest_jar_artifact(client: GitHub, owner: str, repo: str, branch: str | None):
    base = f"/repos/{urllib.parse.quote(owner)}/{urllib.parse.quote(repo)}"
    pattern = ARTIFACTS[repo]
    # GitHub sorts recent runs newest first. Search multiple pages because some
    # repos also run documentation, smoke tests, and builds on pull requests.
    for page in range(1, 7):
        args = urllib.parse.urlencode({"status": "success", "per_page": 100, "page": page})
        runs = client.get(f"{base}/actions/runs?{args}").get("workflow_runs", [])
        if not runs:
            break
        for run in runs:
            if run.get("event") not in ("push", "workflow_dispatch"):
                continue  # Never bundle pull-request builds.
            if branch and run.get("head_branch") != branch:
                continue
            run_id = run["id"]
            artifacts = client.get(f"{base}/actions/runs/{run_id}/artifacts?per_page=100").get(
                "artifacts", []
            )
            matches = [
                a for a in artifacts
                if not a.get("expired") and (
                    a["name"].startswith(pattern[:-1]) if pattern.endswith("*")
                    else a["name"] == pattern
                )
            ]
            if matches:
                chosen = max(matches, key=lambda a: a.get("created_at", ""))
                return run, chosen
        if len(runs) < 100:
            break
    return None


def is_installable_jar(filename: str, repo: str) -> bool:
    """Permit Golems Plus' versioned -dev prerelease, not Loom's extra dev JAR."""
    name = Path(filename).name
    if not name.lower().endswith(".jar"):
        return False
    # Current mod_version=0.1.0-dev. The remapped, installable artifact is
    # golems-plus-0.1.0-dev.jar; Loom's development output is
    # golems-plus-0.1.0-dev-dev.jar, and is still excluded.
    if repo == "golems_plus" and re.fullmatch(
        r"golems-plus-\d+(?:\.\d+)*-dev\.jar", name, flags=re.IGNORECASE
    ):
        return True
    return not SKIP_JAR.search(name)


def extract_installable_jar(archive: bytes, repo: str):
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            options = [
                item for item in z.infolist()
                if not item.is_dir()
                and item.filename.lower().endswith(".jar")
                and is_installable_jar(item.filename, repo)
                and not item.filename.startswith("__MACOSX/")
            ]
            if len(options) != 1:
                names = [item.filename for item in options]
                raise BundlerError(
                    f"{repo}: expected exactly one installable JAR in artifact; found {names}"
                )
            item = options[0]
            if item.file_size > MAX_ARTIFACT_BYTES:
                raise BundlerError(f"{repo}: JAR is unexpectedly large")
            jar = z.read(item)
            return Path(item.filename).name, jar
    except zipfile.BadZipFile as exc:
        raise BundlerError(f"{repo}: downloaded artifact was not a valid ZIP") from exc


def make_bundle(output: Path, *, owner: str, branch: str | None, allow_missing: bool):
    client = GitHub(find_token())
    collected = []
    missing = []
    for repo in ARTIFACTS:
        print(f"Checking {owner}/{repo}...", flush=True)
        try:
            result = newest_jar_artifact(client, owner, repo, branch)
            if result is None:
                raise BundlerError("No unexpired JAR artifact from a successful push/dispatch")
            run, artifact = result
            raw = client.get(
                f"/repos/{urllib.parse.quote(owner)}/{urllib.parse.quote(repo)}"
                f"/actions/artifacts/{artifact['id']}/zip",
                binary=True,
            )
            filename, jar = extract_installable_jar(raw, repo)
            collected.append(
                {
                    "repo": repo,
                    "filename": filename,
                    "data": jar,
                    "branch": run["head_branch"],
                    "commit": run["head_sha"],
                    "run_url": run["html_url"],
                    "run_id": run["id"],
                    "artifact_id": artifact["id"],
                    "artifact_created_at": artifact.get("created_at"),
                    "bytes": len(jar),
                    "sha256": hashlib.sha256(jar).hexdigest(),
                }
            )
            print(f"  + {filename} ({run['head_branch']} @ {run['head_sha'][:8]})")
        except BundlerError as exc:
            missing.append({"repo": repo, "reason": str(exc)})
            print(f"  ! {repo}: {exc}", file=sys.stderr, flush=True)

    if missing and not allow_missing:
        raise BundlerError(
            f"{len(missing)} of {len(ARTIFACTS)} repositories could not be bundled. "
            "No incomplete ZIP was written. Rebuild missing mods, or use --allow-missing."
        )
    if not collected:
        raise BundlerError("No installable JARs were found.")
    names = [entry["filename"].lower() for entry in collected]
    if len(names) != len(set(names)):
        raise BundlerError("Two repositories produced JARs with the same filename; refusing to overwrite.")

    manifest = {
        "bundle_created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "owner": owner,
        "branch_filter": branch or "any (latest successful pushed/dispatched build)",
        "complete": not missing,
        "mods": [
            {k: v for k, v in entry.items() if k != "data"}
            for entry in collected
        ],
        "missing": missing,
    }
    instructions = (
        "Caszual Suite - Latest Packaged JARs\n"
        "===================================\n"
        "Copy the JAR files in the mods/ folder into your Minecraft mods directory.\n"
        "These are existing successful GitHub Actions builds, NOT rebuilt or re-tested here.\n"
        "See manifest.json for the exact repository, branch, commit, build and SHA-256.\n"
        "Feature-branch builds may not be mutually compatible; verify versions and dependencies.\n"
        "The ZIP does not include external dependencies such as Fabric API.\n"
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    partial = output.with_name(output.name + ".partial")
    try:
        with zipfile.ZipFile(partial, "w", compression=zipfile.ZIP_DEFLATED) as bundle:
            bundle.writestr("README.txt", instructions)
            bundle.writestr("manifest.json", json.dumps(manifest, indent=2) + "\n")
            for entry in collected:
                bundle.writestr(f"mods/{entry['filename']}", entry["data"])
        partial.replace(output)
    finally:
        partial.unlink(missing_ok=True)
    print(f"\nCreated {output.resolve()} with {len(collected)} installable JARs.")
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--owner", default="CaszGamerMD")
    parser.add_argument("--branch", default=None, help="Optional: restrict to one branch, e.g. main")
    parser.add_argument("--allow-missing", action="store_true", help="Create partial ZIP if a mod has no valid artifact")
    parser.add_argument("--output", type=Path, default=Path("dist/Caszual-Suite-Latest.zip"))
    args = parser.parse_args()
    try:
        make_bundle(args.output, owner=args.owner, branch=args.branch, allow_missing=args.allow_missing)
    except BundlerError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
