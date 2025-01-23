package net.tigereye.chestcavity.registration;

import net.minecraft.world.damagesource.DamageSource;

public class CCDamageSource extends DamageSource {
    public static final DamageSource HEARTBLEED = new CCDamageSource("ccHeartbleed").bypassArmor();
    public static final DamageSource ORGAN_REJECTION = new CCDamageSource("ccOrganRejection").bypassArmor();

    public CCDamageSource(String name) {
        super(name);
    }


}
