package by.dragonsurvivalteam.dragonsurvival.mixins;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ApplyMixinPlugin implements IMixinConfigPlugin {
    private final static String PREFIX = ApplyMixinPlugin.class.getPackageName() + ".";

    @Override
    public void onLoad(final String mixinPackage) { /* Nothing to do */ }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        String modid = mixinClassName.replace(PREFIX, "");
        // Remove directories which are not related to mods
        modid = modid.replace("client.", "");
        modid = modid.replace("tool_swap.", "");
        // If a directory is still present it will run through the check below
        String[] elements = modid.split("\\.");

        if (elements.length == 2) {
            return LoadingModList.get().getModFileById(elements[0]) != null;
        }

        if (mixinClassName.equals("by.dragonsurvivalteam.dragonsurvival.mixins.client.LiquidBlockRendererMixin")) {
            return LoadingModList.get().getModFileById("embeddium") == null;
        }

        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) { /* Nothing to do */ }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) { /* Nothing to do */ }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) { /* Nothing to do */ }
}
