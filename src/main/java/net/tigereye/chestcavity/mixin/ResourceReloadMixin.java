package net.tigereye.chestcavity.mixin;

import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityAssignmentManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityTypeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ResourceReloadMixin {

    @Inject(at = @At("TAIL"), method = "loadResources")
    private static void loadCustomResources(ResourceManager manager, RegistryAccess.Frozen p_206863_, Commands.CommandSelection p_206864_, int p_206865_, Executor p_206866_, Executor p_206867_, CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
        new OrganManager().onResourceManagerReload(manager);
        new GeneratedChestCavityTypeManager().onResourceManagerReload(manager);
        new GeneratedChestCavityAssignmentManager().onResourceManagerReload(manager);
    }
}
