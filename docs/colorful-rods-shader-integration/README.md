Reference data for shader-pack authors. Do not drop this folder directly into shaderpacks.

Iris block.properties maps these block identifiers to shader-defined material IDs. Choose unused IDs in the particular shader, then modify its emissive/light-source code to use the supplied colors or the animated albedo. Reusing vanilla End Rod's material may force every rod to white.

RGB should sample its animated texture, with the shader's normal color-space conversion, rather than use a fixed RGB constant. Exact integration depends on shader source and version.
