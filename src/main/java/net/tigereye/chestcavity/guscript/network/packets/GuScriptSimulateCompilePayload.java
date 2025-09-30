package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.registry.GuScriptRegistry;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptCompiler;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;
import net.tigereye.chestcavity.registration.CCAttachments;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulate GuScript reduction for the current page and hand the player a paper item
 * whose description lists the reaction steps taken.
 */
public record GuScriptSimulateCompilePayload(int pageIndex) implements CustomPacketPayload {

    public static final Type<GuScriptSimulateCompilePayload> TYPE = new Type<>(ChestCavity.id("guscript_simulate_compile"));

    public static final StreamCodec<FriendlyByteBuf, GuScriptSimulateCompilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.pageIndex),
            buf -> new GuScriptSimulateCompilePayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuScriptSimulateCompilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            GuScriptAttachment attachment = CCAttachments.getGuScript(player);
            if (attachment == null) {
                return;
            }
            if (payload.pageIndex >= 0 && payload.pageIndex < attachment.pages().size()) {
                attachment.setCurrentPage(payload.pageIndex);
            }
            int pageIndex = attachment.getCurrentPageIndex();
            GuScriptPageState page = attachment.activePage();
            if (page == null) {
                return;
            }

            // Build leaves like compiler (with stack count scaling and UI metadata)
            List<LeafGuNode> leaves = new ArrayList<>();
            int bindingSlot = page.items().size() - 1;
            for (int i = 0; i < bindingSlot; i++) {
                var stack = page.items().get(i);
                if (stack.isEmpty()) continue;
                int slotIndex = i;
                var itemId = stack.getItem().builtInRegistryHolder().key().location();
                GuScriptRegistry.leaf(itemId).ifPresent(def -> {
                    int scaledCount = Math.max(1, stack.getCount());
                    com.google.common.collect.HashMultiset<String> scaledTags = com.google.common.collect.HashMultiset.create();
                    def.tags().forEachEntry((tag, tagCount) -> scaledTags.add(tag, tagCount * scaledCount));
                    leaves.add(new LeafGuNode(def.name(), scaledTags, def.actions(), pageIndex, slotIndex));
                });
            }

            List<ReactionRule> rules = GuScriptRegistry.reactionRules();
            GuScriptReducer reducer = new GuScriptReducer();
            GuScriptReducer.ReductionResult result = reducer.reduce(new ArrayList<>(leaves), rules);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("GuScript 编译过程 (页 " + (pageIndex + 1) + ")"));
            if (result.applications().isEmpty()) {
                lore.add(Component.literal("无反应：没有可用的规则或素材不足"));
            } else {
                int idx = 1;
                for (GuScriptReducer.ReactionApplication app : result.applications()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(idx++).append(") ")
                      .append(app.rule().id())
                      .append(" : ");
                    // inputs
                    boolean first = true;
                    for (GuNode in : app.inputs()) {
                        if (!first) sb.append(" + ");
                        sb.append(in.name());
                        first = false;
                    }
                    sb.append(" -> ").append(app.output().name());
                    lore.add(Component.literal(sb.toString()));
                }
            }

            ItemStack paper = new ItemStack(Items.PAPER);
            paper.set(DataComponents.CUSTOM_NAME, Component.literal("GuScript 编译日志"));
            paper.set(DataComponents.LORE, new ItemLore(lore));
            if (!player.getInventory().add(paper)) {
                player.drop(paper, false);
            }
        });
    }
}
