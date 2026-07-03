package com.vcwdfca.ae2ct.api;

import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import com.vcwdfca.ae2ct.tree.TestKey;
import appeng.api.stacks.GenericStack;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void oversizedTreeIsOmittedFromNullablePayload() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        LegacyTreeNode root = node("root");
        List<LegacyTreeNode> children = new ArrayList<>();
        for (int i = 0; i < LegacyTreePayload.MAX_SYNCED_NODES; i++) {
            children.add(node("child-" + i));
        }
        root.addInput(new LegacyTreeProcess(children));

        LegacyTreePayload.writeNullable(buffer, new LegacyTreeData(root));

        assertNull(LegacyTreePayload.readNullable(buffer));
    }

    @Test
    void oversizedSerializedPayloadIsOmittedEvenWithinNodeBudget() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        LegacyTreeNode root = node("root");
        List<LegacyTreeNode> children = new ArrayList<>();
        String largeId = "x".repeat(LegacyTreePayload.MAX_SYNCED_BYTES / 16);
        for (int i = 0; i < 32; i++) {
            children.add(node(largeId + i));
        }
        root.addInput(new LegacyTreeProcess(children));

        LegacyTreePayload.writeNullable(buffer, new LegacyTreeData(root));

        assertNull(LegacyTreePayload.readNullable(buffer));
    }

    private static LegacyTreeNode node(String id) {
        return new LegacyTreeNode(null, new GenericStack(new TestKey(id), 1), List.of(), 0, null);
    }
}
