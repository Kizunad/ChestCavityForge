package net.tigereye.chestcavity.soul.registry;

public final class SoulHurtResult {

    public enum Action { PASS, MODIFY, APPLY, CANCEL }

    private final Action action;
    private final float amount;
    private final boolean appliedResult;

    private SoulHurtResult(Action action, float amount, boolean appliedResult) {
        this.action = action;
        this.amount = amount;
        this.appliedResult = appliedResult;
    }

    public static SoulHurtResult pass() { return new SoulHurtResult(Action.PASS, 0f, false); }
    public static SoulHurtResult cancel() { return new SoulHurtResult(Action.CANCEL, 0f, false); }
    public static SoulHurtResult modify(float amount) { return new SoulHurtResult(Action.MODIFY, amount, false); }
    public static SoulHurtResult applied(boolean success) { return new SoulHurtResult(Action.APPLY, 0f, success); }

    public Action action() { return action; }
    public float amount() { return amount; }
    public boolean appliedResult() { return appliedResult; }
}
