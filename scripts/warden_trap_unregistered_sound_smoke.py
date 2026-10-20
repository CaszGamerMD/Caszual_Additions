#!/usr/bin/env python3
"""Ensure unregistered third-party sound events never crash Warden Trap."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = (root / "src/main/java/com/caszgamermd/caszualadditions/antiwarden/mixin/WardenTrapSoundMixin.java").read_text()
silence = (root / "src/main/java/com/caszgamermd/caszualadditions/antiwarden/WardenTrapSilence.java").read_text()
section = java.split("private boolean caszual$mute(", 1)[1].split("    @Inject(", 1)[0]

assert "sound==null||sound.value()==null" in section
assert "BuiltInRegistries.SOUND_EVENT.getKey(sound.value())" in section
assert "key!=null" in section
assert "sculkOrWardenSound(key.toString())" in section
assert "getKey(sound.value()).toString()" not in java
assert section.index("sound==null") < section.index("getKey(sound.value())")
assert section.index("key!=null") < section.index("key.toString()")
assert "WardenTrapSilence.near((ServerLevel)(Object)this,position)" in section
assert 'soundId.contains("warden")' in silence
assert 'soundId.contains("sculk")' in silence
assert 'soundId.contains("shriek")' in silence
assert 'method="playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;' in java
assert 'method="playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;' in java
print("PASS: null registry IDs fail open; registered Warden/sculk sounds still obey active-trap suppression")
