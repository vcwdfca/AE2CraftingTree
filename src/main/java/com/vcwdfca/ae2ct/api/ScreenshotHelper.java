package com.vcwdfca.ae2ct.api;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.vcwdfca.ae2ct.AE2ct;
import com.vcwdfca.ae2ct.Config;
import com.vcwdfca.ae2ct.gui.CraftingTreeWidget;
import com.vcwdfca.ae2ct.tree.LegacyTreeLayout;
import com.vcwdfca.ae2ct.tree.LegacyTreeNode;
import com.vcwdfca.ae2ct.tree.LegacyTreeProcess;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.client.Screenshot.takeScreenshot;

public class ScreenshotHelper {
    private static final int SCALE = 2;
    private static final Font FONT = new Font("Arial", Font.BOLD, 12);
    public static final Logger LOGGER = LogManager.getLogger();

    public static void Screenshot(LegacyTreeLayout layout, Player player) {
        try {
            Minecraft minecraft = Minecraft.getInstance();

            List<LegacyTreeLayout.Entry> nodes = layout.rows().stream()
                .flatMap(List::stream)
                .filter(entry -> !entry.placeholder())
                .toList();
            Set<AEKey> keys = new HashSet<>();
            for (LegacyTreeLayout.Entry entry : nodes) {
                keys.add(entry.node().key());
            }

            int maxX = nodes.stream().mapToInt(LegacyTreeLayout.Entry::column).max().orElse(0);
            int maxY = nodes.stream().mapToInt(LegacyTreeLayout.Entry::row).max().orElse(0);

            LOGGER.info("Screenshot: width:{}, height:{}", (long) (maxX + 1) * 110L, (long) (maxY + 1) * 110L);
            BufferedImage image = new BufferedImage((maxX + 1) * 110, (maxY + 1) * 110, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            graphics.setFont(FONT);
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(4));

            Map<AEKey, Point> map = new HashMap<>();
            BufferedImage stackImage = init(keys, map);
            if (!nodes.isEmpty()) {
                draw(graphics, stackImage, layout, nodes.getFirst(), map);
            }

            graphics.dispose();
            safeImage(minecraft.gameDirectory, "CraftingTree_" + Util.getFilenameFormattedDateTime() + ".png",
                image, player::sendSystemMessage);
        } catch (Exception e) {
            LOGGER.error("Error:", e);
            player.sendSystemMessage(Component.translatable("ae2ct.screenshot.exception", e.toString()));
        }
    }

    private static BufferedImage init(Set<AEKey> keys, Map<AEKey, Point> map) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        int size = keys.size();
        int len = ((int) Math.sqrt(size)) + 1;
        int simpleLen = 22;
        int width = len * simpleLen * SCALE * 2;
        int height = len * simpleLen * SCALE * 2;

        RenderTarget target = new RenderTarget(true) {
        };
        target.createBuffers(width, height, true);
        target.setClearColor(203, 204, 212, 255);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        view.translate(-1.0f, 1.0f, 0.0f);
        view.scale(4f / width, -4f / height, -1f / 1000f);
        view.translate(0.0f, 0.0f, 10.0f);
        RenderSystem.applyModelViewMatrix();
        Matrix4f backupProj = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().identity(), VertexSorting.ORTHOGRAPHIC_Z);

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        GuiGraphics guiGraphics = new GuiGraphics(minecraft, bufferSource);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);

        guiGraphics.fill(0, 0, width / SCALE, height / SCALE, 0xffcbccd4);

        int x = 0;
        int y = 0;
        for (AEKey key : keys) {
            Point pos = new Point(x, y);
            map.put(key, pos);

            guiGraphics.blit(ResourceLocation.tryBuild(AE2ct.MODID, "icon.png"), x * simpleLen, y * simpleLen, 0, 0, 22, 22);
            AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, x * simpleLen + 3, y * simpleLen + 3, key);
            x++;
            if (x >= len) {
                x = 0;
                y++;
            }
        }

        guiGraphics.flush();
        RenderSystem.setProjectionMatrix(backupProj, VertexSorting.ORTHOGRAPHIC_Z);
        view.popMatrix();
        RenderSystem.applyModelViewMatrix();
        target.unbindWrite();
        NativeImage nativeimage = takeScreenshot(target);
        var img = ImageIO.read(new ByteArrayInputStream(nativeimage.asByteArray()));
        target.destroyBuffers();
        return img;
    }

    private static void draw(Graphics2D graphics, BufferedImage stackImage, LegacyTreeLayout layout,
                             LegacyTreeLayout.Entry entry, Map<AEKey, Point> map) {
        int spacing = 110;
        int output = 10;
        int stackLength = 44;
        int x = entry.column() * spacing + output;
        int y = entry.row() * spacing + output;
        LegacyTreeNode node = entry.node();

        if (!node.inputs().isEmpty()) {
            graphics.drawLine(x + stackLength, y + stackLength, x + stackLength, y + stackLength + spacing / 2);
        }

        Point pos = map.get(node.key());
        BufferedImage subImage = stackImage.getSubimage(pos.x * 88, pos.y * 88, 88, 88);
        graphics.drawImage(subImage, x, y, null);

        if (Config.SCREENSHOT_SHOW_COUNT.get()) {
            String text = CraftingTreeWidget.getDrawAmount(node.key(), node.amount());
            var fm = graphics.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            graphics.drawString(text, x + 80 - textWidth, y + 92 - textHeight);
        }

        for (LegacyTreeProcess process : node.inputs()) {
            for (LegacyTreeNode child : process.inputs()) {
                LegacyTreeLayout.Entry childEntry = layout.entry(child);
                if (childEntry == null) {
                    continue;
                }
                int childX = childEntry.column() * spacing + output;
                int childY = childEntry.row() * spacing + output;
                graphics.drawLine(childX + stackLength, y + stackLength + spacing / 2,
                    childX + stackLength, childY + stackLength);
                draw(graphics, stackImage, layout, childEntry, map);
            }
        }
        if (!node.inputs().isEmpty() && entry.linkedSubNodes() > 0) {
            graphics.drawLine(x + stackLength, y + stackLength + spacing / 2,
                entry.linkEndColumn() * spacing + output + stackLength, y + stackLength + spacing / 2);
        }
    }

    private static void safeImage(File file, @Nullable String fileName, BufferedImage image, Consumer<Component> receiver) throws IOException {
        File screenshotDir = new File(file, "screenshots");
        screenshotDir.mkdir();
        File outputFile;
        if (fileName == null) {
            outputFile = getFile(screenshotDir);
        } else {
            outputFile = new File(screenshotDir, fileName);
        }
        final File target = outputFile.getCanonicalFile();
        Util.ioPool().execute(() -> {
            try {
                boolean success = ImageIO.write(image, "png", target);
                if (!success) {
                    throw new IOException("Failed to write buffered image to file: " + target.getAbsolutePath());
                }
                Component component = Component.literal(outputFile.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle(style -> {
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, target.getAbsolutePath()));
                });
                receiver.accept(Component.translatable("screenshot.success", component));
            } catch (Exception exception) {
                LOGGER.error("Failed to save crafting tree screenshot", exception);
                receiver.accept(Component.translatable("screenshot.failure", exception.getMessage()));
            }
        });
    }

    private static File getFile(File directory) {
        String timestamp = Util.getFilenameFormattedDateTime();
        int index = 1;

        while (true) {
            File file = new File(directory, timestamp + (index == 1 ? "" : "_" + index) + ".png");
            if (!file.exists()) {
                return file;
            }
            ++index;
        }
    }
}
