package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.function.Supplier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guzhenren.nudao.GuzhenrenNudaoBridge;

final class FlowActionUtils {

    private FlowActionUtils() {
    }

    static FlowEdgeAction describe(Supplier<String> description) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
            }

            @Override
            public String describe() {
                return description.get();
            }
        };
    }

    static boolean isAlly(Player performer, LivingEntity candidate) {
        if (performer == null || candidate == null) {
            return false;
        }
        if (!candidate.isAlive()) {
            return false;
        }
        if (candidate instanceof TamableAnimal tam && tam.isOwnedBy(performer)) {
            return true;
        }
        return GuzhenrenNudaoBridge.openSubject(candidate)
                .map(handle -> handle.isOwnedBy(performer))
                .orElse(false);
    }
}
