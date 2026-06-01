package com.vcwdfca.ae2ct.tree;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

final class TestKey extends AEKey {
    private final String id;

    TestKey(String id) {
        this.id = id;
    }

    @Override
    public AEKeyType getType() {
        return null;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        return tag;
    }

    @Override
    public Object getPrimaryKey() {
        return id;
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.tryBuild("ae2ct_test", id);
    }

    @Override
    public void writeToPacket(FriendlyByteBuf data) {
        data.writeUtf(id);
    }

    @Override
    protected Component computeDisplayName() {
        return Component.literal(id);
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TestKey other && Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
