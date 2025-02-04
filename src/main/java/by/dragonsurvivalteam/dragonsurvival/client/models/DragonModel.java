package by.dragonsurvivalteam.dragonsurvival.client.models;

import static by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod.MODID;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.client.skin_editor_system.DragonEditorHandler;
import by.dragonsurvivalteam.dragonsurvival.client.skin_editor_system.objects.SkinPreset.SkinAgeGroup;
import by.dragonsurvivalteam.dragonsurvival.client.util.FakeClientPlayer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateHandler;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.capability.objects.DragonMovementData;
import by.dragonsurvivalteam.dragonsurvival.common.dragon_types.AbstractDragonBody;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.util.Functions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.model.GeoModel;

public class DragonModel extends GeoModel<DragonEntity> {
	private final ResourceLocation defaultTexture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/dragon/cave_newborn.png");
	private final ResourceLocation model = ResourceLocation.fromNamespaceAndPath(MODID, "geo/dragon_model.geo.json");
	private ResourceLocation currentTexture;

	/**TODO Body Types Update
	Required:
	 - tips for body types like for magic abilities

	 Extras:
     - customization.json - Ability to disallow some details in the editor for some Body Types (for example, wing details are not required for wingless).
	 - emotes.json - Ability to disallow some emotions for certain Body Types.
	*/

	@Override
	public void applyMolangQueries(final AnimationState<DragonEntity> animationState, double currentTick) {
		super.applyMolangQueries(animationState, currentTick);

		DragonEntity dragon = animationState.getAnimatable();

		// In case the Integer (id of the player) is null
		if (dragon.playerId == null || dragon.getPlayer() == null) {
			return;
		}

		Player player = dragon.getPlayer();
		DragonStateHandler handler = DragonStateProvider.getOrGenerateHandler(player);
		DragonMovementData md = handler.getMovementData();

		MathParser.setVariable("query.y_velocity", () -> md.deltaMovement.y);
		MathParser.setVariable("query.head_yaw", () -> md.headYaw);
		MathParser.setVariable("query.head_pitch", () -> md.headPitch);

		double gravity = player.getAttribute(Attributes.GRAVITY).getValue();
		MathParser.setVariable("query.gravity", () -> gravity);

        double yAccel = (md.deltaMovement.y - md.deltaMovementLastFrame.y) * md.getTickFactor();

		double bodyYawAvg;
		double headYawAvg;
		double headPitchAvg;
		double yAccelAvg;
		if(!ClientDragonRenderer.isOverridingMovementData) {
			double bodyYawChange = Functions.angleDifference(md.bodyYawLastFrame, md.bodyYaw) * md.getTickFactor();
			double headYawChange = Functions.angleDifference(md.headYawLastFrame, md.headYaw) * md.getTickFactor();
			double headPitchChange = Functions.angleDifference(md.headPitchLastFrame, md.headPitch) * md.getTickFactor();

			dragon.bodyYawHistory.add(bodyYawChange);
			while (dragon.bodyYawHistory.size() > 10 * md.getTickFactor()) {
				dragon.bodyYawHistory.removeFirst();
			}

			dragon.headYawHistory.add(headYawChange);
			while (dragon.headYawHistory.size() > 10 * md.getTickFactor()) {
				dragon.headYawHistory.removeFirst();
			}

			dragon.headPitchHistory.add(headPitchChange);
			while (dragon.headPitchHistory.size() > 10 * md.getTickFactor()) {
				dragon.headPitchHistory.removeFirst();
			}

			dragon.yAccelHistory.add(yAccel);
			while (dragon.yAccelHistory.size() > 10 * md.getTickFactor()) {
				dragon.yAccelHistory.removeFirst();
			}

			bodyYawAvg = dragon.bodyYawHistory.stream().mapToDouble(a -> a).sum() / dragon.bodyYawHistory.size();
			headYawAvg = dragon.headYawHistory.stream().mapToDouble(a -> a).sum() / dragon.headYawHistory.size();
			headPitchAvg = dragon.headPitchHistory.stream().mapToDouble(a -> a).sum() / dragon.headPitchHistory.size();
			yAccelAvg = dragon.yAccelHistory.stream().mapToDouble(a -> a).sum() / dragon.yAccelHistory.size();
		} else {
			bodyYawAvg = 0;
			headYawAvg = 0;
			headPitchAvg = 0;
			yAccelAvg = 0;
		}

		MathParser.setVariable("query.body_yaw_change", () -> bodyYawAvg);
		MathParser.setVariable("query.head_yaw_change", () -> headYawAvg);
		MathParser.setVariable("query.head_pitch_change", () -> headPitchAvg);

		MathParser.setVariable("query.y_accel", () -> yAccelAvg);
	}
	
	@Override
	public ResourceLocation getModelResource(final DragonEntity dragon) {
		return model;
	}

	public ResourceLocation getTextureResource(final DragonEntity dragon) {
		if (dragon.playerId != null || dragon.getPlayer() != null) {
			DragonStateHandler handler = DragonStateProvider.getOrGenerateHandler(dragon.getPlayer());
			SkinAgeGroup ageGroup = handler.getSkinData().skinPreset.skinAges.get(handler.getLevel()).get();

			if (handler.getSkinData().recompileSkin) {
				DragonEditorHandler.generateSkinTextures(dragon);
			}

			if (handler.getSkinData().blankSkin) {
				return ResourceLocation.fromNamespaceAndPath(MODID, "textures/dragon/blank_skin_" + handler.getTypeNameLowerCase() + ".png");
			}

			if (ageGroup.defaultSkin) {
				if (currentTexture != null) {
					return currentTexture;
				}

				return ResourceLocation.fromNamespaceAndPath(MODID, "textures/dragon/" + handler.getTypeNameLowerCase() + "_" + handler.getLevel().getNameLowerCase() + ".png");
			}

			if (handler.getSkinData().isCompiled && currentTexture == null) {
				return ResourceLocation.fromNamespaceAndPath(MODID, "dynamic_normal_" + dragon.getPlayer().getStringUUID() + "_" + handler.getLevel().name);
			}
		}

		if (currentTexture == null && dragon.getPlayer() instanceof FakeClientPlayer) {
			LocalPlayer localPlayer = Minecraft.getInstance().player;

			if (localPlayer != null) { // TODO :: Check if skin is compiled?
				return ResourceLocation.fromNamespaceAndPath(MODID, "dynamic_normal_" + localPlayer.getStringUUID() + "_" + DragonStateProvider.getOrGenerateHandler(dragon.getPlayer()).getLevel().name);
			}
		}

		return currentTexture == null ? defaultTexture : currentTexture;
	}

	public void setCurrentTexture(final ResourceLocation currentTexture) {
		this.currentTexture = currentTexture;
	}

	@Override
	public ResourceLocation getAnimationResource(final DragonEntity dragon) {
		if (dragon.playerId != null || dragon.getPlayer() != null) {
			DragonStateHandler handler = DragonStateProvider.getOrGenerateHandler(dragon.getPlayer());
			AbstractDragonBody body = handler.getBody();
			if (body != null) {
				return ResourceLocation.fromNamespaceAndPath(MODID, String.format("animations/dragon_%s.json", body.getBodyNameLowerCase()));
			}
		}
		return ResourceLocation.fromNamespaceAndPath(MODID, "animations/dragon_center.json");
	}

	@Override
	public RenderType getRenderType(final DragonEntity animatable, final ResourceLocation texture) {
		return RenderType.entityCutout(texture);
	}
}