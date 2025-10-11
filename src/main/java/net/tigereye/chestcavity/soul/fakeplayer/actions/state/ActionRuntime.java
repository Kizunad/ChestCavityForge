package net.tigereye.chestcavity.soul.fakeplayer.actions.state;

import net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionId;

/** Per-action runtime snapshot stored in the state manager. */
public final class ActionRuntime {
    public final ActionId id;
    public final long startedAt;
    public long nextReadyAt;
    public String step;

    public ActionRuntime(ActionId id, long startedAt, long nextReadyAt, String step) {
        this.id = id;
        this.startedAt = startedAt;
        this.nextReadyAt = nextReadyAt;
        this.step = step;
    }
}

