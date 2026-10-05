# Caszual companion-mod integration

A companion mod can remain fully standalone while still appearing as part of the
Caszual Additions family.

## 1. Add items and blocks to the shared tag

Create this file in the companion mod:

`src/main/resources/data/caszual_additions/tags/item/caszual_content.json`

Example:

```json
{
  "replace": false,
  "values": [
    "pretty_frogs:watermelon_frog_spawn_egg",
    "pretty_frogs:skeleton_frog_spawn_egg"
  ]
}
```

Blocks that have an item form are listed by their item identifier in the same
tag.

When Caszual Additions is installed, everything in this tag is automatically
placed into the **Caszual Additions** Creative Mode tab.

When JEI is installed too, every tagged item receives the search aliases
**Caszual Additions** and **Caszual**.

## 2. Optional dependency

The companion mod does not need Caszual Additions in order to run. Its
`fabric.mod.json` may declare it as a suggestion:

```json
"suggests": {
  "caszual_additions": "*"
}
```

This keeps every Caszual mod independently installable.

## 3. Naming convention

The individual mod keeps its own public name and mod id. For example:

- Pretty Frogs / `pretty_frogs`
- TV Time / `tvtime`
- Aquarium / its own mod id

The shared tag is what binds them into the Caszual Additions ecosystem.
