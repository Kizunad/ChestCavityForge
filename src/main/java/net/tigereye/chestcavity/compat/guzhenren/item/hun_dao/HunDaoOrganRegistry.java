package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.DaHunGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.GuiQiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.XiaoHunGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 魂道（Hun Dao）相关器官的注册入口。
 * <p>
 * 该类将小魂蛊（以及其他魂道器官）与胸腔系统进行行为绑定：
 * - 在每秒（SlowTick）时执行资源维护与状态同步；
 * - 在命中（OnHit）时触发“魂焰”持续伤害效果与魂魄消耗；
 * - 在器官被移除（Removal）时保留必要的绑定与活跃态；
 * - 在装备时初始化绑定信息并确保联动通道可用。
 * <p>
 * 如需扩展更多魂道器官，按同样方式向 {@link #SPECS} 中追加条目即可。
 */
public final class HunDaoOrganRegistry {

    private static final String MOD_ID = "guzhenren";
    public static final ResourceLocation XIAO_HUN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiao_hun_gu");
    public static final ResourceLocation DA_HUN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dahungu");
    public static final ResourceLocation GUI_QI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "guiqigu");

    private static final List<OrganIntegrationSpec> SPECS = List.of(
            OrganIntegrationSpec.builder(XIAO_HUN_GU_ID)
                    .addSlowTickListener(XiaoHunGuBehavior.INSTANCE)
                    .addRemovalListener(XiaoHunGuBehavior.INSTANCE)
                    .ensureAttached(XiaoHunGuBehavior.INSTANCE::ensureAttached)
                    .onEquip(XiaoHunGuBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(DA_HUN_GU_ID)
                    .addSlowTickListener(DaHunGuBehavior.INSTANCE)
                    .ensureAttached(DaHunGuBehavior.INSTANCE::ensureAttached)
                    .onEquip(DaHunGuBehavior.INSTANCE::onEquip)
                    .build(),
            OrganIntegrationSpec.builder(GUI_QI_GU_ID)
                    .addSlowTickListener(GuiQiGuOrganBehavior.INSTANCE)
                    .addOnHitListener(GuiQiGuOrganBehavior.INSTANCE)
                    .ensureAttached(GuiQiGuOrganBehavior.INSTANCE::ensureAttached)
                    .onEquip(GuiQiGuOrganBehavior.INSTANCE::onEquip)
                    .build()
    );

    private static final Set<ResourceLocation> ORGAN_IDS;

    static {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        for (OrganIntegrationSpec spec : SPECS) {
            ids.add(spec.organId());
        }
        ORGAN_IDS = Collections.unmodifiableSet(ids);
    }

    private HunDaoOrganRegistry() {
    }

    /**
     * 提供给集成桥的规格列表。
     * @return 针对魂道器官的集成规格集合
     */
    public static List<OrganIntegrationSpec> specs() {
        return SPECS;
    }

    /**
     * Returns the set of registered Hun Dao organ ids (魂道蛊) for chest cavity counting.
     * @return immutable set of organ identifiers
     */
    public static Set<ResourceLocation> organIds() {
        return ORGAN_IDS;
    }
}
