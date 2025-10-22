package net.tigereye.chestcavity.guscript.ast;

import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/** Represents an executable action attached to a GuScript node. */
public interface Action {

  /** Stable identifier used for registry/serialization. */
  String id();

  /** Human-readable description for UI/debug. */
  String description();

  /** Execute the action within the provided runtime context. */
  void execute(GuScriptContext context);
}
