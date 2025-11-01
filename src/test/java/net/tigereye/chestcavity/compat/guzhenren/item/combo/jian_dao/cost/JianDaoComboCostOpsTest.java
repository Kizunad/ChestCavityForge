package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.cost;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.JianDaoComboRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;
import org.junit.jupiter.api.Test;

class JianDaoComboCostOpsTest {

  @Test
  void tuningConstantsMatchDesign() {
    assertEquals(100.0D, JianDaoComboTuning.COST_ZHENYUAN_BASE, 1.0E-6);
    assertEquals(30.0D, JianDaoComboTuning.COST_JINGLI, 1.0E-6);
    assertEquals(30.0D, JianDaoComboTuning.COST_HUNPO, 1.0E-6);
    assertEquals(30.0F, JianDaoComboTuning.COST_HEALTH, 1.0E-6);
    assertEquals(100.0D, JianDaoComboTuning.COST_JIANDAO_DAOHEN, 1.0E-6);
    assertEquals("daohen_jiandao", JianDaoComboTuning.KEY_DAOHEN_JIANDAO);
    assertEquals(20L * 60L * 5L, JianDaoComboTuning.EFFECT_WEAKNESS_TICKS);
    assertEquals(2, JianDaoComboTuning.EFFECT_WEAKNESS_AMPLIFIER);
  }

  @Test
  void missingRequiredOrgansDetectsMissingItems() {
    Set<ResourceLocation> have = new HashSet<>();
    // only two present
    have.add(JianDaoComboRegistry.ZHI_LU_GU);
    have.add(JianDaoComboRegistry.JIAN_QI_GU);
    List<ResourceLocation> missing = JianDaoComboCostOps.missingRequiredOrgansFromIds(have);
    assertEquals(2, missing.size());
    assertTrue(missing.contains(JianDaoComboRegistry.YU_JUN_GU));
    assertTrue(missing.contains(JianDaoComboRegistry.YI_ZHUAN_REN_DAO_XI_WANG_GU));
  }

  @Test
  void missingRequiredOrgansAllPresent() {
    Set<ResourceLocation> have = new HashSet<>();
    have.add(JianDaoComboRegistry.ZHI_LU_GU);
    have.add(JianDaoComboRegistry.JIAN_QI_GU);
    have.add(JianDaoComboRegistry.YU_JUN_GU);
    have.add(JianDaoComboRegistry.YI_ZHUAN_REN_DAO_XI_WANG_GU);
    List<ResourceLocation> missing = JianDaoComboCostOps.missingRequiredOrgansFromIds(have);
    assertTrue(missing.isEmpty());
  }

  @Test
  void precheckResourcesPassAndFail() {
    // exactly enough should pass (health requires > cost + 1.0 safety)
    assertTrue(
        JianDaoComboCostOps.precheckResources(
            100.0, 30.0, 30.0, JianDaoComboTuning.COST_HEALTH + 1.1, 100.0));
    // insufficient zhenyuan
    assertFalse(
        JianDaoComboCostOps.precheckResources(
            99.9, 30.0, 30.0, JianDaoComboTuning.COST_HEALTH + 2.0, 100.0));
    // insufficient jingli
    assertFalse(
        JianDaoComboCostOps.precheckResources(
            100.0, 29.9, 30.0, JianDaoComboTuning.COST_HEALTH + 2.0, 100.0));
    // insufficient hunpo
    assertFalse(
        JianDaoComboCostOps.precheckResources(
            100.0, 30.0, 29.9, JianDaoComboTuning.COST_HEALTH + 2.0, 100.0));
    // insufficient health safety
    assertFalse(
        JianDaoComboCostOps.precheckResources(
            100.0, 30.0, 30.0, JianDaoComboTuning.COST_HEALTH + 0.9, 100.0));
    // insufficient dao hen
    assertFalse(
        JianDaoComboCostOps.precheckResources(
            100.0, 30.0, 30.0, JianDaoComboTuning.COST_HEALTH + 2.0, 99.9));
  }
}

