package net.tigereye.chestcavity.soul.util;

import java.util.Random;

/**
 * Name pool for random SoulPlayer identities. You can edit RANDOM_NAMES to your liking.
 */
public final class SoulNamePool {

    private SoulNamePool() {}

    // TODO: Populate with your preferred names. Keep each <=16 chars if possible.
    public static final String[] RANDOM_NAMES = new String[] {
            "古月方源","古月方正","梦求真","何春秋","李小白","吴帅","战部渡","气海老祖","房睇长"
    };

    public static String pick(Random random) {
        if (RANDOM_NAMES == null || RANDOM_NAMES.length == 0) return null;
        int idx = Math.max(0, random.nextInt(RANDOM_NAMES.length));
        return RANDOM_NAMES[idx];
    }
}

