package net.tigereye.chestcavity.interfaces;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCAttachments;

import java.util.Optional;

public interface ChestCavityEntity {
    static Optional<ChestCavityEntity> of(Entity entity) {
        if (entity instanceof ChestCavityEntity chestCavityEntity) {
            return Optional.of(chestCavityEntity);
        }
        if (entity instanceof LivingEntity living) {
            return Optional.of(new AttachmentBacked(living));
        }
        return Optional.empty();
    }

    ChestCavityInstance getChestCavityInstance();

    default void setChestCavityInstance(ChestCavityInstance cc) {
        throw new UnsupportedOperationException();
    }

    final class AttachmentBacked implements ChestCavityEntity {
        private final LivingEntity living;

        private AttachmentBacked(LivingEntity living) {
            this.living = living;
        }

        @Override
        public ChestCavityInstance getChestCavityInstance() {
            return CCAttachments.getChestCavity(living);
        }

        @Override
        public void setChestCavityInstance(ChestCavityInstance cc) {
            cc.owner = living;
            cc.inventory.setInstance(cc);
            living.setData(CCAttachments.CHEST_CAVITY.get(), cc);
        }
    }
}
