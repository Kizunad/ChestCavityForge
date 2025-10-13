package net.tigereye.chestcavity.config;


import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.tigereye.chestcavity.ChestCavity;


@Config(name = ChestCavity.MODID)
public class CCConfig implements ConfigData {

    @ConfigEntry.Category("core")
    public float ORGAN_BUNDLE_LOOTING_BOOST = .04f;
    @ConfigEntry.Category("core")
    public float UNIVERSAL_DONOR_RATE = .1f;
    @ConfigEntry.Category("core")
    public int ORGAN_REJECTION_DAMAGE = 2; //how much rejecting organs hurts
    @ConfigEntry.Category("core")
    public int ORGAN_REJECTION_RATE = 600; //base speed of organ rejection
    @ConfigEntry.Category("core")
    public int HEARTBLEED_RATE = 20; //how fast you die from lacking a heart in ticks
    @ConfigEntry.Category("core")
    public int KIDNEY_RATE = 60; //how often the kidneys prevent blood poisoning in ticks
    @ConfigEntry.Category("core")
    public float FILTRATION_DURATION_FACTOR = 1f; //how much extra kidneys reduce poison duration
    @ConfigEntry.Category("core")
    public float APPENDIX_LUCK = .1f; //how lucky your appendix is
    @ConfigEntry.Category("core")
    public float HEART_HP = 4; //how much health each heart is worth
    @ConfigEntry.Category("core")
    public float MUSCLE_STRENGTH = 1f; //how much 8 stacks of muscles contribute to attack damage
    @ConfigEntry.Category("core")
    public float MUSCLE_SPEED = .5f; //how much 8 stacks of muscles contribute to movement speed
    @ConfigEntry.Category("core")
    public float NERVES_HASTE = .1f; //how much a spine contributes to mining and attack speed
    @ConfigEntry.Category("core")
    public float BONE_DEFENSE = .5f; //damage reduction from 4 stacks of ribs
    @ConfigEntry.Category("core")
    public float RISK_OF_PRIONS = .01f; //risk of debuffs from human-derived foods
    @ConfigEntry.Category("core")
    public int CHEST_OPENER_ABSOLUTE_HEALTH_THRESHOLD = 20; //health below which a chest can be opened
    @ConfigEntry.Category("core")
    public float CHEST_OPENER_FRACTIONAL_HEALTH_THRESHOLD = .5f; //health below which a chest can be opened
    @ConfigEntry.Category("core")
    public boolean CAN_OPEN_OTHER_PLAYERS = false;
    @ConfigEntry.Category("core")
    public boolean KEEP_CHEST_CAVITY = false;
    @ConfigEntry.Category("core")
    public boolean DISABLE_ORGAN_REJECTION = false;

    @ConfigEntry.Category("more")
    public int ARROW_DODGE_DISTANCE = 32; //how far you can teleport when dodging projectiles
    @ConfigEntry.Category("more")
    public float BUFF_PURGING_DURATION_FACTOR = .5f; //how much withered bones reduce wither duration
    @ConfigEntry.Category("more")
    public int CRYSTALSYNTHESIS_RANGE = 32; //range at which you can link to a End Crystal
    @ConfigEntry.Category("more")
    public int CRYSTALSYNTHESIS_FREQUENCY = 10; //how often the link to an End Crystal is updated and perks gained
    @ConfigEntry.Category("more")
    public float FIREPROOF_DEFENSE = .75f; //damage reduction from 4 stacks of fireproof organs
    @ConfigEntry.Category("more")
    public float IMPACT_DEFENSE = .75f; //damage reduction from 4 stacks of impact resistant organs
    @ConfigEntry.Category("more")
    public float IRON_REPAIR_PERCENT = .25f; //damage reduction from 4 stacks of impact resistant organs
    @ConfigEntry.Category("more")
    public float LAUNCHING_POWER = .1f; //upward velocity per launching
    @ConfigEntry.Category("more")
    public int MAX_TELEPORT_ATTEMPTS = 5;
    @ConfigEntry.Category("more")
    public int PHOTOSYNTHESIS_FREQUENCY = 50; //how many ticks 8 photosynthetic organs in direct sunlight need to restore 1 hunger
    @ConfigEntry.Category("more")
    public int RUMINATION_TIME = 400; //time to eat a unit of grass
    @ConfigEntry.Category("more")
    public int RUMINATION_GRASS_PER_SQUARE = 2; //number of grass units are in a square
    @ConfigEntry.Category("more")
    public int RUMINATION_SQUARES_PER_STOMACH = 3; //number of grass squares a stomach can hold
    @ConfigEntry.Category("more")
    public int SHULKER_BULLET_TARGETING_RANGE = 20;
    @ConfigEntry.Category("more")
    public float SWIMSPEED_FACTOR = 1f; //how much 8 swimming muscles boost swim speed
    @ConfigEntry.Category("more")
    public float WITHERED_DURATION_FACTOR = .5f; //how much withered bones reduce wither duration

    @ConfigEntry.Category("cooldown")
    public int ARROW_DODGE_COOLDOWN = 200; //how often an entity is allowed to dodge projectiles
    @ConfigEntry.Category("cooldown")
    public int DRAGON_BOMB_COOLDOWN = 200; //how often an entity is allowed to fire bombs
    @ConfigEntry.Category("cooldown")
    public int DRAGON_BREATH_COOLDOWN = 200; //how often an entity is allowed to fire bombs
    @ConfigEntry.Category("cooldown")
    public int EXPLOSION_COOLDOWN = 200; //how often an entity is allowed to try exploding
    @ConfigEntry.Category("cooldown")
    public int FORCEFUL_SPIT_COOLDOWN = 20; //how often an entity is allowed to spit
    @ConfigEntry.Category("cooldown")
    public int GHASTLY_COOLDOWN = 60; //how often an entity is allowed to fire ghast bombs
    @ConfigEntry.Category("cooldown")
    public int IRON_REPAIR_COOLDOWN = 1200; //how often an entity is allowed to use iron to heal
    @ConfigEntry.Category("cooldown")
    public int PYROMANCY_COOLDOWN = 78; //how often an entity is allowed to spew fireballs
    @ConfigEntry.Category("cooldown")
    public int SHULKER_BULLET_COOLDOWN = 100; //how often an entity is allowed to shoot bullets
    @ConfigEntry.Category("cooldown")
    public int SILK_COOLDOWN = 20; //how often an entity is allowed to lay silk
    @ConfigEntry.Category("cooldown")
    public int VENOM_COOLDOWN = 40; //how often an entity is allowed to poison targets

    @ConfigEntry.Category("integration")
    public boolean BACKROOMS_INTEGRATION = true;
    @ConfigEntry.Category("integration")
    public int BACKROOMS_CHEST_ORGAN_LOOT_ATTEMPTS = 2;
    @ConfigEntry.Category("integration")
    public float BACKROOMS_CHEST_ORGAN_LOOT_CHANCE = 0.2f;
    @ConfigEntry.Category("integration")
    public boolean BEWITCHMENT_INTEGRATION = true;
    @ConfigEntry.Category("integration")
    public boolean BIOME_MAKEOVER_INTEGRATION = true;
    @ConfigEntry.Category("integration")
    public boolean GUZHENREN_NUDAO_LOGGING = true;

    @ConfigEntry.Category("guzhenren_bing_xue_dao")
    @ConfigEntry.Gui.CollapsibleObject
    public GuzhenrenBingXueDaoConfig GUZHENREN_BING_XUE_DAO = new GuzhenrenBingXueDaoConfig();

    @ConfigEntry.Category("guscript_ui")
    @ConfigEntry.Gui.CollapsibleObject
    public GuScriptUIConfig GUSCRIPT_UI = new GuScriptUIConfig();

    @ConfigEntry.Category("guscript_execution")
    @ConfigEntry.Gui.CollapsibleObject
    public GuScriptExecutionConfig GUSCRIPT_EXECUTION = new GuScriptExecutionConfig();

    @ConfigEntry.Category("sword_slash")
    @ConfigEntry.Gui.CollapsibleObject
    public SwordSlashConfig SWORD_SLASH = new SwordSlashConfig();

    public static class GuScriptUIConfig {
        @ConfigEntry.Gui.Tooltip
        public double bindingRightPaddingSlots = 0.5D;
        @ConfigEntry.Gui.Tooltip
        public double bindingTopPaddingSlots = 1.5D;
        @ConfigEntry.Gui.Tooltip
        public double bindingVerticalSpacingSlots = 0.35D;
        @ConfigEntry.Gui.Tooltip
        public double bindingButtonWidthFraction = 0.56D;
        @ConfigEntry.Gui.Tooltip
        public int bindingButtonHeightPx = 20;
        @ConfigEntry.Gui.Tooltip
        public int minBindingButtonWidthPx = 0;
        @ConfigEntry.Gui.Tooltip
        public int bindingVerticalOffsetPx = 10;
        @ConfigEntry.Gui.Tooltip
        public int minTopGutterPx = 0;
        @ConfigEntry.Gui.Tooltip
        public int minHorizontalGutterPx = 6;
        @ConfigEntry.Gui.Tooltip
        public int minBindingSpacingPx = 0;
        @ConfigEntry.Gui.Tooltip
        public int pageButtonWidthPx = 20;
        @ConfigEntry.Gui.Tooltip
        public int pageButtonHeightPx = 20;
        @ConfigEntry.Gui.Tooltip
        public double pageButtonLeftPaddingSlots = 0.8D;
        @ConfigEntry.Gui.Tooltip
        public double pageButtonTopPaddingSlots = 0.9D;
        @ConfigEntry.Gui.Tooltip
        public double pageButtonHorizontalSpacingSlots = 0.2D;
        @ConfigEntry.Gui.Tooltip
        public int minPageButtonSpacingPx = 0;
        @ConfigEntry.Gui.Tooltip
        public int pageButtonHorizontalOffsetPx = -50;
        @ConfigEntry.Gui.Tooltip
        public int pageInfoBelowNavSpacingPx = 6;
    }

    public static class GuScriptExecutionConfig {
        @ConfigEntry.Gui.Tooltip
        public int maxKeybindPagesPerTrigger = 16;
        @ConfigEntry.Gui.Tooltip
        public int maxKeybindRootsPerTrigger = 64;
        @ConfigEntry.Gui.Tooltip
        public double maxCumulativeMultiplier = 5.0D;
        @ConfigEntry.Gui.Tooltip
        public double maxCumulativeFlat = 20.0D;
        @ConfigEntry.Gui.Tooltip
        public boolean enableFlows = true; // toggle flow integration; when false, all roots execute immediately
        @ConfigEntry.Gui.Tooltip
        public boolean enableFlowQueue = true; // default off to preserve legacy single-flow behavior
        @ConfigEntry.Gui.Tooltip
        public int maxFlowQueueLength = 40;
        @ConfigEntry.Gui.Tooltip
        public boolean revalidateQueuedGuards = true;
        @ConfigEntry.Gui.Tooltip
        public int queuedGuardRetryLimit = 0;
        @ConfigEntry.Gui.Tooltip
        public TimeScaleCombineStrategy timeScaleCombine = TimeScaleCombineStrategy.MULTIPLY;
        @ConfigEntry.Gui.Tooltip
        public boolean preferUiOrder = true; // prioritize page/slot ordering over compilation order when available
    }

    public enum TimeScaleCombineStrategy {
        MULTIPLY,
        MAX,
        OVERRIDE
    }

    public static class SwordSlashConfig {
        @ConfigEntry.Gui.Tooltip
        public double defaultLength = 7.5D;
        @ConfigEntry.Gui.Tooltip
        public double defaultThickness = 0.9D;
        @ConfigEntry.Gui.Tooltip
        public int defaultLifespanTicks = 12;
        @ConfigEntry.Gui.Tooltip
        public double defaultDamage = 10.0D;
        @ConfigEntry.Gui.Tooltip
        public double defaultBreakPower = 1.5D;
        @ConfigEntry.Gui.Tooltip
        public int defaultMaxPierce = 3;
        @ConfigEntry.Gui.Tooltip
        public boolean enableBlockBreaking = true;
        @ConfigEntry.Gui.Tooltip
        public int blockBreakCapPerTick = 6;
        @ConfigEntry.Gui.Tooltip
        public boolean debugLogging = false;

        @ConfigEntry.Gui.CollapsibleObject
        public SwordSlashVisualConfig visuals = new SwordSlashVisualConfig();
    }

    public static class SwordSlashVisualConfig {
        @ConfigEntry.Gui.Tooltip
        public int slashColor = 0xFFE0A0;
        @ConfigEntry.Gui.Tooltip
        public double slashAlpha = 0.85D;
        @ConfigEntry.Gui.Tooltip
        public double slashEndAlpha = 0.3D;
    }

    public static class GuzhenrenBingXueDaoConfig {

        @ConfigEntry.Gui.CollapsibleObject
        public BingJiGuConfig BING_JI_GU = new BingJiGuConfig();

        @ConfigEntry.Gui.CollapsibleObject
        public QingReGuConfig QING_RE_GU = new QingReGuConfig();

        @ConfigEntry.Gui.CollapsibleObject
        public ShuangXiGuConfig SHUANG_XI_GU = new ShuangXiGuConfig();

        @ConfigEntry.Gui.CollapsibleObject
        public BingBuGuConfig BING_BU_GU = new BingBuGuConfig();

        public static class BingJiGuConfig {
            public double zhenyuanBaseCost = 200.0D;
            public double jingliPerTick = 1.0D;
            public float healPerTick = 4.5F;
            public int slowTickIntervalsPerMinute = 15;
            public float absorptionPerTrigger = 5.0F;
            public float absorptionCap = 20.0F;
            public double bonusDamageFraction = 0.04D;
            public double bonusTriggerChance = 0.12D;
            public int iceEffectDurationTicks = 600;
            public double iceBurstBaseDamage = 18.0D;
            public double iceBurstRadius = 6.0D;
            public double iceBurstRadiusPerStack = 0.5D;
            public double iceBurstStackDamageScale = 0.65D;
            public double iceBurstBingBaoMultiplier = 0.35D;
            public float iceBurstSlowAmplifier = 1.0F;
            public int iceBurstSlowDurationTicks = 160;
            public int invulnerabilityDurationTicks = 40;
            public int invulnerabilityCooldownTicks = 600;
            public double lowHealthThreshold = 0.30D;
        }

        public static class QingReGuConfig {
            public double baseZhenyuanCost = 100.0D;
            public double jingliPerTick = 1.0D;
            public float healPerTick = 3.0F;
            public double poisonClearChance = 0.10D;
            public double fireDamageReduction = 0.03D;
        }

        public static class ShuangXiGuConfig {
            public double increasePerStack = 0.02D;
            public double frostbiteChance = 0.15D;
            public double frostbiteDamagePercent = 0.05D;
            public int frostbiteDurationSeconds = 4;
            public int coldDurationTicks = 60;
            public int freezeReductionTicks = 40;
            public double abilityRange = 6.0D;
            public double coneDotThreshold = 0.45D;
            public int breathParticleSteps = 12;
            public double breathParticleSpacing = 0.45D;
            public double baseZhenyuanCost = 800.0D;
        }

        public static class BingBuGuConfig {
            public int playerRegenDurationTicks = 100;
            public int playerRegenAmplifier = 1;
            public int nonPlayerRegenDurationTicks = 200;
            public int nonPlayerRegenAmplifier = 1;
            public int saturationDurationTicks = 20;
            public int effectRefreshThresholdTicks = 40;
            public int nonPlayerIntervalSeconds = 120;
            public float burpVolume = 0.6F;
            public float burpPitchMin = 0.9F;
            public float burpPitchVariance = 0.1F;
        }
    }

}
