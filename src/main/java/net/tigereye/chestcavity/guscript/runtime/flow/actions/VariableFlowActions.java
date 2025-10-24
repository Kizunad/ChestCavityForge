package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/** FlowController 变量读写相关逻辑。 */
final class VariableFlowActions {

  private VariableFlowActions() {}

  static FlowEdgeAction setVariable(String name, double value, boolean asDouble) {
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || name == null) {
          return;
        }
        if (asDouble) {
          controller.setDouble(name, value);
        } else {
          controller.setLong(name, Math.round(value));
        }
      }

      @Override
      public String describe() {
        return "set_variable(" + name + ")";
      }
    };
  }

  static FlowEdgeAction addVariable(String name, double delta, boolean asDouble) {
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || name == null) {
          return;
        }
        if (asDouble) {
          controller.addDouble(name, delta);
        } else {
          controller.addLong(name, Math.round(delta));
        }
      }

      @Override
      public String describe() {
        return "add_variable(" + name + ")";
      }
    };
  }

  static FlowEdgeAction addVariableFromVariable(
      String targetName, String sourceName, double scale, boolean asDouble) {
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || targetName == null || sourceName == null) {
          return;
        }
        double delta = controller.getDouble(sourceName, 0.0D) * scale;
        if (asDouble) {
          controller.addDouble(targetName, delta);
        } else {
          controller.addLong(targetName, Math.round(delta));
        }
      }

      @Override
      public String describe() {
        return "add_variable_from_variable(" + sourceName + " -> " + targetName + ")";
      }
    };
  }

  static FlowEdgeAction clampVariable(String name, double min, double max, boolean asDouble) {
    double lower = Math.min(min, max);
    double upper = Math.max(min, max);
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || name == null) {
          return;
        }
        if (asDouble) {
          controller.setDouble(
              name, net.minecraft.util.Mth.clamp(controller.getDouble(name, 0.0D), lower, upper));
        } else {
          long current = controller.getLong(name, 0L);
          long clamped =
              Math.max((long) Math.ceil(lower), Math.min((long) Math.floor(upper), current));
          controller.setLong(name, clamped);
        }
      }

      @Override
      public String describe() {
        return "clamp_variable(" + name + ")";
      }
    };
  }

  static FlowEdgeAction setVariableFromParam(
      String paramKey, String variableName, double defaultValue) {
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || paramKey == null || variableName == null) {
          return;
        }
        double value = controller.resolveFlowParamAsDouble(paramKey, defaultValue);
        controller.setDouble(variableName, value);
      }

      @Override
      public String describe() {
        return "set_variable_from_param(" + paramKey + ")";
      }
    };
  }

  static FlowEdgeAction copyVariable(
      String sourceName, String targetName, double scale, double offset, boolean asDouble) {
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (controller == null || sourceName == null || targetName == null) {
          return;
        }
        double value = controller.getDouble(sourceName, 0.0D) * scale + offset;
        if (asDouble) {
          controller.setDouble(targetName, value);
        } else {
          controller.setLong(targetName, Math.round(value));
        }
      }

      @Override
      public String describe() {
        return "copy_variable(" + sourceName + " -> " + targetName + ")";
      }
    };
  }
}
