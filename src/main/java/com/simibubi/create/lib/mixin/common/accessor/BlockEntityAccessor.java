package com.simibubi.create.lib.mixin.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {
	@Invoker("saveMetadata")
	void create$saveMetadata(CompoundTag compoundTag);
}
