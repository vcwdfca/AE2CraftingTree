package com.vcwdfca.ae2ct.api;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyTreePayloadTest {
    @Test
    void roundTripsNullableTree() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        LegacyTreePayload.writeNullable(buffer, null);

        assertNull(LegacyTreePayload.readNullable(buffer));
    }
}
