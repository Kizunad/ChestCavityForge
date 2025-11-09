package kizuna.guzhenren_event_ext.common.attachment;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GuzhenrenEventExtension.MODID);

    public static final Supplier<AttachmentType<EventExtensionPlayerData>> PLAYER_EVENT_DATA =
            ATTACHMENT_TYPES.register(
                    "player_event_data",
                    () -> AttachmentType.serializable(EventExtensionPlayerData::new).build()
            );

    public static EventExtensionPlayerData get(Player player) {
        return player.getData(PLAYER_EVENT_DATA.get());
    }
}
