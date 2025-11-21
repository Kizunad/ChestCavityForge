package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.nbt.CompoundTag;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 通用资源快照：道痕、真元、精力、念头、魂魄。
 */
public final class HunDaoSoulAvatarResourceState {

  private static final String DAO_HEN_KEY = "daohen_hundao";

  private double daoHenHunDao;
  private double zhenyuan;
  private double maxZhenyuan;
  private double jingli;
  private double maxJingli;
  private double niantou;
  private double maxNiantou;
  private double hunpo;
  private double maxHunpo;

  public void copyFrom(ResourceHandle handle) {
    if (handle == null) {
      return;
    }
    this.daoHenHunDao = DaoHenResourceOps.get(handle, DAO_HEN_KEY);
    this.zhenyuan = handle.getZhenyuan().orElse(0.0);
    this.maxZhenyuan = handle.getMaxZhenyuan().orElse(0.0);
    this.jingli = handle.getJingli().orElse(0.0);
    this.maxJingli = handle.getMaxJingli().orElse(0.0);
    this.niantou = handle.getNiantou().orElse(0.0);
    this.maxNiantou = handle.getMaxNiantou().orElse(0.0);
    this.hunpo = handle.getHunpo().orElse(0.0);
    this.maxHunpo = handle.getMaxHunpo().orElse(0.0);
  }

  public void applyTo(ResourceHandle handle) {
    if (handle == null) {
      return;
    }
    DaoHenResourceOps.set(handle, DAO_HEN_KEY, this.daoHenHunDao);
    handle.writeDouble("zhenyuan", this.zhenyuan);
    handle.writeDouble("zuida_zhenyuan", this.maxZhenyuan);
    handle.writeDouble("jingli", this.jingli);
    handle.writeDouble("zuida_jingli", this.maxJingli);
    handle.writeDouble("niantou", this.niantou);
    handle.writeDouble("niantou_zhida", this.maxNiantou);
    handle.writeDouble("hunpo", this.hunpo);
    handle.writeDouble("zuida_hunpo", this.maxHunpo);
  }

  public void save(CompoundTag tag) {
    tag.putDouble("HunDaoDaoHen", this.daoHenHunDao);
    tag.putDouble("HunDaoZhenyuan", this.zhenyuan);
    tag.putDouble("HunDaoMaxZhenyuan", this.maxZhenyuan);
    tag.putDouble("HunDaoJingli", this.jingli);
    tag.putDouble("HunDaoMaxJingli", this.maxJingli);
    tag.putDouble("HunDaoNiantou", this.niantou);
    tag.putDouble("HunDaoMaxNiantou", this.maxNiantou);
    tag.putDouble("HunDaoHunpo", this.hunpo);
    tag.putDouble("HunDaoMaxHunpo", this.maxHunpo);
  }

  public void load(CompoundTag tag) {
    this.daoHenHunDao = tag.getDouble("HunDaoDaoHen");
    this.zhenyuan = tag.getDouble("HunDaoZhenyuan");
    this.maxZhenyuan = tag.getDouble("HunDaoMaxZhenyuan");
    this.jingli = tag.getDouble("HunDaoJingli");
    this.maxJingli = tag.getDouble("HunDaoMaxJingli");
    this.niantou = tag.getDouble("HunDaoNiantou");
    this.maxNiantou = tag.getDouble("HunDaoMaxNiantou");
    this.hunpo = tag.getDouble("HunDaoHunpo");
    this.maxHunpo = tag.getDouble("HunDaoMaxHunpo");
  }

  public double getDaoHenHunDao() {
    return daoHenHunDao;
  }

  public double getZhenyuan() {
    return zhenyuan;
  }

  public double getMaxZhenyuan() {
    return maxZhenyuan;
  }

  public double getJingli() {
    return jingli;
  }

  public double getMaxJingli() {
    return maxJingli;
  }

  public double getNiantou() {
    return niantou;
  }

  public double getMaxNiantou() {
    return maxNiantou;
  }

  public double getHunpo() {
    return hunpo;
  }

  public double getMaxHunpo() {
    return maxHunpo;
  }

  public void setHunpoSnapshot(double hunpo, double maxHunpo) {
    this.hunpo = Math.max(0.0D, hunpo);
    this.maxHunpo = Math.max(this.hunpo, maxHunpo);
  }
}
