package com.vcwdfca.ae2ct.api;

import appeng.api.stacks.GenericStack;
import com.vcwdfca.ae2ct.tree.Amounts;
import com.vcwdfca.ae2ct.tree.LegacyTreeData;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTreePayload {
    private LegacyTreePayload() {
    }

    public static void writeNullable(FriendlyByteBuf buffer, LegacyTreeData data) {
        boolean present = data != null && data.root() != null;
        buffer.writeBoolean(present);
        if (present) {
            write(buffer, data);
        }
    }

    public static LegacyTreeData readNullable(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? read(buffer) : null;
    }

    public static void write(FriendlyByteBuf buffer, LegacyTreeData data) {
        if (data == null || data.root() == null) {
            throw new IllegalArgumentException("Cannot write an empty legacy tree payload");
        }
        writeNode(buffer, data.root());
    }

    public static LegacyTreeData read(FriendlyByteBuf buffer) {
        return new LegacyTreeData(readNode(buffer));
    }

    private static void writeNode(FriendlyByteBuf buffer, LegacyTreeNode node) {
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

    private static LegacyTreeNode readNode(FriendlyByteBuf buffer) {
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

    private static void writeProcess(FriendlyByteBuf buffer, LegacyTreeProcess process) {
        buffer.writeVarInt(process.inputs().size());
        for (LegacyTreeNode input : process.inputs()) {
            writeNode(buffer, input);
        }
    }

    private static LegacyTreeProcess readProcess(FriendlyByteBuf buffer) {
        int nodeCount = buffer.readVarInt();
        List<LegacyTreeNode> inputs = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            inputs.add(readNode(buffer));
        }
        return new LegacyTreeProcess(inputs);
    }
}
