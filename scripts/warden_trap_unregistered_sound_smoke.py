#!/usr/bin/env python3
"""Guard against crashes when unrelated mods emit unregistered footsteps.

Animal Garden's mouse can emit a SoundEvent without a registry key; a null
identifier must pass through the trap filter rather than stopping world ticks.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
java=(root/"src/main/java/com/caszgamermd/caszualadditions/antiwarden/mixin/WardenTrapSoundMixin.java").read_text()
silence=(root/"src/main/java/com/caszgamermd/caszualadditions/antiwarden/WardenTrapSilence.java").read_text()
section=java.split("private boolean caszual$mute(",1)[1].split('    @Inject(',1)[0]

# Both a null sound holder and an unregistered SoundEvent must fail open.
assert "if(sound==null||sound.value()==null)return false;" in section
assert "BuiltInRegistries.SOUND_EVENT.getKey(sound.value())" in section
assert "if(soundId==null)return false;" in section
assert "soundId.toString()" in section
assert section.index("if(soundId==null)return false;") < section.index("soundId.toString()")
assert "getKey(sound.value()).toString()" not in java

# The fix must not remove the intended suppression or accidentally silence
# every sound made by third-party mobs near a Warden Trap.
assert "WardenTrapSilence.sculkOrWardenSound(soundId.toString())" in section
assert "WardenTrapSilence.near((ServerLevel)(Object)this,position)" in section
assert section.index("sculkOrWardenSound") < section.index("WardenTrapSilence.near")
assert "soundId.contains(\"warden\")" in silence
assert "soundId.contains(\"sculk\")" in silence
assert "soundId.contains(\"shriek\")" in silence
assert 'method="playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;' in java
assert 'method="playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;' in java
print("PASS: unregistered third-party sounds pass safely; Warden/sculk suppression retained")
