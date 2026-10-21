# Caszual Suite JAR Bundler

This utility gathers the newest successfully packaged, unexpired GitHub Actions mod JAR from each of nine Caszual Suite repositories into one ZIP. It does not rebuild mods.

## Run on GitHub

1. Merge the bundler feature pull request into Caszual_Additions/main.
2. Create a fine-grained GitHub personal access token for all nine private repositories, granting Actions: Read-only and Metadata: Read-only.
3. Set the CASZUAL_SUITE_TOKEN secret in Caszual_Additions > Settings > Secrets and variables > Actions. Never put this token in source code or a workflow input.
4. Select Caszual_Additions > Actions > Bundle Caszual Suite JARs > Run workflow.
5. Download Caszual-Suite-Latest.zip from the resulting GitHub prerelease (Releases page), or from the workflow artifact.

Leave the Branch input empty for the latest successful packaged build on any branch, or set it to main to limit the result to main. GitHub's regular GITHUB_TOKEN cannot read other private repos, which is why the extra token is required.

## Run on your computer

Install Python 3.10+ and GitHub CLI, then run gh auth login. Alternatively, set an environment variable CASZUAL_SUITE_TOKEN or GH_TOKEN with access to your private repositories.

From a local checkout of Caszual_Additions:

    py scripts/bundle_caszual_suite.py
    py scripts/bundle_caszual_suite.py --branch main
    py scripts/bundle_caszual_suite.py --allow-missing

On macOS/Linux, replace py with python3. The output defaults to dist/Caszual-Suite-Latest.zip.

## Included mods

| Repo | Artifact |
| --- | --- |
| Caszual_Additions | Caszual-Additions-* |
| PrettyFrogs | PrettyFrogs-1.0.0 |
| TVtime | caszual-tv-time-build |
| Caszual_MTG_addon | caszual-mtg-* |
| Caszual_Aquariums | linked-aquariums-build |
| MC-MiningRig | miningrig-26.2 |
| Ore_Laundering | Ore-Laundering-Fabric-26.2 |
| Caszual_Pallets_Plus | Caszual-Pallets-Plus |
| golems_plus | golems-plus-dev |

## ZIP layout

The ZIP contains README.txt, manifest.json, and a mods/ folder with exactly one installable JAR per mod.

The manifest records the exact branch, commit, workflow URL, artifact ID, JAR name, size, and SHA-256 hash. Source, development, test, and Javadoc JARs are excluded. Unrelated artifacts (such as Blockbench exports or logs) are not downloaded.

The default behavior is to fail rather than create an incomplete ZIP. Use --allow-missing only when you explicitly want a partial bundle; missing mods are recorded in the manifest.

The newest builds may come from different branches and may not be compatible with each other. This is a packaging convenience, not a multi-mod integration test. External mod dependencies, such as Fabric API, are not included. If upstream artifact names change, update the ARTIFACTS mapping at the beginning of the script.

