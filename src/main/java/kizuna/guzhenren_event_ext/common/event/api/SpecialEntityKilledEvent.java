package kizuna.guzhenren_event_ext.common.event.api;

import net.minecraft.world.entity.player.Player;

/**
 * 玩家击杀带有特殊标记的实体时触发的事件
 * <p>
 * 当玩家击杀的实体 NBT 中包含 {@code guzhenren_event_ext:entity_tag} 时，
 * 该事件会被触发。
 * <p>
 * 该事件由 EventManager 监听 {@link net.neoforged.neoforge.event.entity.living.LivingDeathEvent} 时发布。
 */
public class SpecialEntityKilledEvent extends GuzhenrenPlayerEvent {

    private final String entityTag;

    /**
     * @param player 击杀实体的玩家
     * @param entityTag 被击杀实体的标签值（来自 NBT {@code guzhenren_event_ext:entity_tag}）
     */
    public SpecialEntityKilledEvent(Player player, String entityTag) {
        super(player);
        this.entityTag = entityTag;
    }

    /**
     * @return 被击杀实体的标签值
     */
    public String getEntityTag() {
        return entityTag;
    }
}
