package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;

/**
 * 力道系列器官的基础行为模板，提供常用计算方法。
 */
public abstract class AbstractLiDaoOrganBehavior extends AbstractGuzhenrenOrganBehavior {

    protected AbstractLiDaoOrganBehavior() {}

    protected double liDaoIncrease(LivingEntity entity) {
        return LiDaoHelper.getLiDaoIncrease(entity);
    }

    protected double liDaoIncrease(ChestCavityInstance cc) {
        return LiDaoHelper.getLiDaoIncrease(cc);
    }

    protected int countMuscles(ChestCavityInstance cc) {
        return LiDaoHelper.countMuscleStacks(cc);
    }

    protected ActiveLinkageContext linkageContext(ChestCavityInstance cc) {
        return cc == null ? null : LinkageManager.getContext(cc);
    }
}
