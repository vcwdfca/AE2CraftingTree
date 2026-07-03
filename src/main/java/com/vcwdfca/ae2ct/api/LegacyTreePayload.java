package com.vcwdfca.ae2ct.api;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.tree.Amounts;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTreePayload {
    public static final int MAX_SYNCED_NODES = 8_192;
    public static final int MAX_SYNCED_BYTES = 512 * 1024;
    private static final Logger LOGGER = LogManager.getLogger();

    private LegacyTreePayload() {
    }

    public static void writeNullable(RegistryFriendlyByteBuf buffer, LegacyTreeData data) {
        if (data == null || data.root() == null) {
            buffer.writeBoolean(false);
            return;
        }
        if (data.allNodes().size() > MAX_SYNCED_NODES) {
            LOGGER.warn("Skipping legacy crafting tree sync: {} nodes exceeds limit {}",
                data.allNodes().size(), MAX_SYNCED_NODES);
            buffer.writeBoolean(false);
            return;
        }
        int estimatedBytes = estimateBytes(data.root());
        if (estimatedBytes > MAX_SYNCED_BYTES) {
            LOGGER.warn("Skipping legacy crafting tree sync: estimated {} bytes exceeds limit {}",
                estimatedBytes, MAX_SYNCED_BYTES);
            buffer.writeBoolean(false);
            return;
        }

        RegistryFriendlyByteBuf payload = new RegistryFriendlyByteBuf(Unpooled.buffer(), buffer.registryAccess());
        try {
            write(payload, data);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to serialize legacy crafting tree payload", e);
            buffer.writeBoolean(false);
            return;
        }
        if (payload.readableBytes() > MAX_SYNCED_BYTES) {
            LOGGER.warn("Skipping legacy crafting tree sync: encoded {} bytes exceeds limit {}",
                payload.readableBytes(), MAX_SYNCED_BYTES);
            buffer.writeBoolean(false);
            return;
        }
        buffer.writeBoolean(true);
        buffer.writeBytes(payload);
    }

    public static LegacyTreeData readNullable(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? read(buffer) : null;
    }

    public static void write(RegistryFriendlyByteBuf buffer, LegacyTreeData data) {
        if (data == null || data.root() == null) {
            throw new IllegalArgumentException("Cannot write an empty legacy tree payload");
        }
        writeNode(buffer, data.root());
    }

    public static LegacyTreeData read(RegistryFriendlyByteBuf buffer) {
        return new LegacyTreeData(readNode(buffer));
    }

    private static void writeNode(RegistryFriendlyByteBuf buffer, LegacyTreeNode node) {
        GenericStack.writeBuffer(node.output(), buffer);
        buffer.writeVarLong(node.missing());
        buffer.writeVarLong(node.amounts().missing());
        buffer.writeVarLong(node.amounts().stored());
        buffer.writeVarLong(node.amounts().craft());
        buffer.writeVarInt(node.inputs().size());
        for (LegacyTreeProcess process : node.inputs()) {
            writeProcess(buffer, process);
        }
    }

    private static LegacyTreeNode readNode(RegistryFriendlyByteBuf buffer) {
        GenericStack output = GenericStack.readBuffer(buffer);
        long missing = buffer.readVarLong();
        Amounts amounts = new Amounts(buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong());
        int processCount = buffer.readVarInt();
        LegacyTreeNode node = new LegacyTreeNode(null, output, List.of(), missing, amounts);
        for (int i = 0; i < processCount; i++) {
            node.addInput(readProcess(buffer));
        }
        return node;
    }

    private static void writeProcess(RegistryFriendlyByteBuf buffer, LegacyTreeProcess process) {
        buffer.writeVarInt(process.inputs().size());
        for (LegacyTreeNode input : process.inputs()) {
            writeNode(buffer, input);
        }
    }

    private static LegacyTreeProcess readProcess(RegistryFriendlyByteBuf buffer) {
        int nodeCount = buffer.readVarInt();
        List<LegacyTreeNode> inputs = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            inputs.add(readNode(buffer));
        }
        return new LegacyTreeProcess(inputs);
    }

    private static int estimateBytes(LegacyTreeNode node) {
        int bytes = 48 + node.key().toString().length() + node.key().getId().toString().length();
        for (LegacyTreeProcess process : node.inputs()) {
            bytes += 4;
            for (LegacyTreeNode input : process.inputs()) {
                bytes += estimateBytes(input);
            }
        }
        return bytes;
    }
}
