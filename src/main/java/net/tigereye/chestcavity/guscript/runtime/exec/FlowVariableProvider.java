package net.tigereye.chestcavity.guscript.runtime.exec;

/**
 * Optional capability for GuScript contexts to expose live flow variables to actions.
 */
public interface FlowVariableProvider {

    double resolveFlowVariable(String name, double defaultValue);

    long resolveFlowVariableAsLong(String name, long defaultValue);
}
