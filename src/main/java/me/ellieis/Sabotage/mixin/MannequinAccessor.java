package me.ellieis.Sabotage.mixin;

import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinAccessor {
    @Invoker("setProfile")
    void sabotage$setMannequinProfile(ResolvableProfile profile);
}
