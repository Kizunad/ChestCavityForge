package net.tigereye.chestcavity.client.modernui.config.docs;

import java.util.Collection;

public interface DocProvider {
    String name();

    Collection<DocEntry> loadAll();
}
