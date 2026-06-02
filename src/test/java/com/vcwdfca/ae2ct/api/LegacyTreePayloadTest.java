package com.vcwdfca.ae2ct.api;

import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyTreePayloadTest {
    @Test
    void roundTripsNullableTree() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        LegacyTreePayload.writeNullable(buffer, null);

        assertNull(LegacyTreePayload.readNullable(buffer));
    }

    @Test
    void treatsEmptyTreeAsNullablePayload() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        LegacyTreePayload.writeNullable(buffer, new LegacyTreeData(null));

        assertNull(LegacyTreePayload.readNullable(buffer));
        assertThrows(IllegalArgumentException.class, () -> LegacyTreePayload.write(new FriendlyByteBuf(Unpooled.buffer()),
                new LegacyTreeData(null)));
    }
}
