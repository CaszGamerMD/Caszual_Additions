package dev.casz.utils.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Minecraft.class)
public interface MinecraftScreenAccessor {
 @Invoker("setScreen") void caszutils$setScreen(Screen screen);
}
