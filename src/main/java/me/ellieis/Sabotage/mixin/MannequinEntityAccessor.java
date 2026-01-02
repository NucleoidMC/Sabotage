package me.ellieis.Sabotage.mixin;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.decoration.MannequinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MannequinEntity.class)
public interface MannequinEntityAccessor {
    @Invoker("setMannequinProfile")
    void sabotage$setMannequinProfile(ProfileComponent profile);
}
