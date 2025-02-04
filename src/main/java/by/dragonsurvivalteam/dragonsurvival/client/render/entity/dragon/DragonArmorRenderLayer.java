package by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon;

import static by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod.MODID;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvivalMod;
import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.common.items.armor.EvilDragonArmorItem;
import by.dragonsurvivalteam.dragonsurvival.common.items.armor.GoodDragonArmorItem;
import by.dragonsurvivalteam.dragonsurvival.util.ResourceHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DragonArmorRenderLayer extends GeoRenderLayer<DragonEntity> {
	private final GeoEntityRenderer<DragonEntity> renderer;
	private static final AbstractTexture missingno = Minecraft.getInstance().getTextureManager().getTexture(ResourceLocation.fromNamespaceAndPath("minecraft", "missingno"));
	public DragonArmorRenderLayer(final GeoEntityRenderer<DragonEntity> renderer) {
		super(renderer);
		this.renderer = renderer;
	}

	@Override
	public void render(final PoseStack poseStack, final DragonEntity animatable, final BakedGeoModel bakedModel, final RenderType renderType, final MultiBufferSource bufferSource, final VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
		Player player = animatable.getPlayer();
		if (player == null) {
			return;
		}

		if (player.isSpectator()) {
			return;
		}

		ResourceLocation armorTexture = constructTrimmedDragonArmorTexture(player);
		((DragonRenderer) renderer).isRenderLayers = true;
		renderArmor(poseStack, animatable, bakedModel, bufferSource, partialTick, packedLight, armorTexture);
		((DragonRenderer) renderer).isRenderLayers = false;
	}

	private void renderArmor(final PoseStack poseStack, final DragonEntity animatable, final BakedGeoModel bakedModel, final MultiBufferSource bufferSource, float partialTick, int packedLight, final ResourceLocation texture) {
		if (animatable == null) {
			return;
		}

		Color armorColor = new Color(1f, 1f, 1f);

		ClientDragonRenderer.dragonModel.setCurrentTexture(texture);
		RenderType type = renderer.getRenderType(animatable, texture, bufferSource, partialTick);
		if (type != null) {
			VertexConsumer vertexConsumer = bufferSource.getBuffer(type);
			renderer.actuallyRender(poseStack, animatable, bakedModel, type, bufferSource, vertexConsumer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY, armorColor.getRGB());
		}
	}

	public static ResourceLocation constructTrimmedDragonArmorTexture(final Player pPlayer) {
		try (NativeImage image = new NativeImage(512, 512, true)) {
			String armorUUID = buildUniqueArmorUUID(pPlayer);
			ResourceLocation imageLoc = ResourceLocation.fromNamespaceAndPath(MODID, "armor_" + armorUUID);
			if (Minecraft.getInstance().getTextureManager().getTexture(imageLoc, missingno) instanceof DynamicTexture texture && !texture.equals(missingno)) {
				return imageLoc;
			}
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack itemstack = pPlayer.getItemBySlot(slot);
				ResourceLocation existingArmorLocation = ResourceLocation.fromNamespaceAndPath(MODID, constructArmorTexture(pPlayer, slot));
				if (itemstack.getItem() instanceof ArmorItem item) {
					try {
						ArmorTrim trim = itemstack.get(DataComponents.TRIM);
						Optional<Resource> armorFile = Minecraft.getInstance().getResourceManager().getResource(existingArmorLocation);
						NativeImage armorImage, trimImage = null;
						boolean trimOk = false;

						Color trimBaseColor;
						float[] trimBaseHSB = new float[3];

						if (armorFile.isPresent()) {
							InputStream textureStream = armorFile.get().open();
							armorImage = NativeImage.read(textureStream);
							textureStream.close();
						} else {
							continue;
						}

						if (trim != null) {
							String patternPath = trim.pattern().value().assetId().getPath();
							Optional<Resource> trimFile = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(MODID, "textures/trims/" + patternPath + "_" + item.getType().getName() + ".png"));

							if (trimFile.isPresent()) {
								InputStream textureStream = trimFile.get().open();
								trimImage = NativeImage.read(textureStream);
								textureStream.close();
								trimOk = true;
							}
							TextColor tc = trim.material().value().description().getStyle().getColor();
							if (tc != null) {
								// Not the most elegant solution,
								// but the best way I could find to get a single color reliably...
								// TODO: something better
								trimBaseColor = new Color(tc.getValue());
								Color.RGBtoHSB(trimBaseColor.getBlue(), trimBaseColor.getGreen(), trimBaseColor.getRed(), trimBaseHSB);
							}
						}
						float[] armorHSB = new float[3];
						float[] trimHSB = new float[3];
						float[] dyeHSB = new float[3];
						DyedItemColor dyeColor = itemstack.get(DataComponents.DYED_COLOR);
						if (dyeColor != null) {
							Color armorDye = new Color(dyeColor.rgb());
							Color.RGBtoHSB(armorDye.getBlue(), armorDye.getGreen(), armorDye.getRed(), dyeHSB);
						}

						for (int x = 0; x < armorImage.getWidth(); x++) {
							for (int y = 0; y < armorImage.getHeight(); y++) {
								Color armorColor = new Color(armorImage.getPixelRGBA(x, y), true);
								if (trimOk) {
									Color trimColor = new Color(trimImage.getPixelRGBA(x, y), true);
									Color.RGBtoHSB(trimColor.getRed(), trimColor.getGreen(), trimColor.getBlue(), trimHSB);
									if (trimColor.getAlpha() != 0) {
										// Changes the hue and saturation to be the same as the trim's base color while keeping the design's brightness
										if (trimHSB[1] == 0) {
											// Replace any grayscale parts with the appropriate trim color
											image.setPixelRGBA(x, y, Color.HSBtoRGB(trimBaseHSB[0], trimBaseHSB[1], trimHSB[2]));
										} else {
											// Otherwise, keep the same color (for parts that should not change color)
											image.setPixelRGBA(x, y, trimColor.getRGB());
										}
									} else if (armorColor.getAlpha() != 0) {
										// There is no trim on this pixel and we can ignore it safely
										if (dyeHSB[0] != 0 && dyeHSB[1] != 0) {
											// Get the armor's brightness, and the dye's hue and saturation
											image.setPixelRGBA(x, y, Color.HSBtoRGB(dyeHSB[0], dyeHSB[1], Color.RGBtoHSB(armorColor.getRed(), armorColor.getGreen(), armorColor.getBlue(), null)[1]));
										} else {
											image.setPixelRGBA(x, y, armorColor.getRGB());
										}
									}
								} else if (armorColor.getAlpha() != 0){
									// No armor trim, just the armor
									Color.RGBtoHSB(armorColor.getRed(), armorColor.getGreen(), armorColor.getBlue(), armorHSB);
									if ((dyeHSB[0] != 0 || dyeHSB[1] != 0) && armorHSB[1] == 0) {
										// Get the armor's brightness, and the dye's hue and saturation
										image.setPixelRGBA(x, y, Color.HSBtoRGB(dyeHSB[0], dyeHSB[1], armorHSB[2]));
									} else {
										image.setPixelRGBA(x, y, armorColor.getRGB());
									}
								}
							}
						}
					} catch (IOException e) {
						DragonSurvivalMod.LOGGER.error("An error occurred while compiling the dragon armor trim texture", e);
					}
				}
			}
			uploadTexture(image, imageLoc);
			image.close();
			return imageLoc;
		}
	}

	public static String buildUniqueArmorUUID(Player pPlayer) {
		StringBuilder armorTotal = new StringBuilder();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmor())
				continue;
			ItemStack itemstack = pPlayer.getItemBySlot(slot);
			armorTotal.append(itemstack);

			ArmorTrim armorTrim = itemstack.getComponents().get(DataComponents.TRIM);
			if (armorTrim != null) {
				armorTotal.append("_").append(armorTrim.material().value().assetName()).append("_").append(armorTrim.pattern().value().assetId());
			}

			DyedItemColor dyeColor = itemstack.get(DataComponents.DYED_COLOR);
			if (dyeColor != null) {
				armorTotal.append(dyeColor);
			}
		}
		return UUID.nameUUIDFromBytes(armorTotal.toString().getBytes()).toString();
	}

	public static void uploadTexture(NativeImage image, ResourceLocation location) {
		try (image) {
			if (Minecraft.getInstance().getTextureManager().getTexture(location, missingno) instanceof DynamicTexture texture && !texture.equals(missingno)) {
				texture.setPixels(image);
				texture.upload();
            } else {
				DynamicTexture layer = new DynamicTexture(image);
				Minecraft.getInstance().getTextureManager().register(location, layer);
            }
            /*File file = new File(Minecraft.getInstance().gameDirectory, "texture");
			file.mkdirs();
			file = new File(file.getPath(), armorTrimKey.toString().replace(":", "_") + ".png");
			file.getParentFile().mkdirs();
			image.writeToFile(file);*/
		} catch (Exception e) {
			DragonSurvivalMod.LOGGER.error(e);
		}
	}

	public static String constructArmorTexture(Player playerEntity, EquipmentSlot equipmentSlot){
		String texture = "textures/armor/";
		Item item = playerEntity.getItemBySlot(equipmentSlot).getItem();
		String texture2 = itemToResLoc(item);
		if (texture2 != null) {
			texture2 = texture + texture2;
			if (Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(MODID, texture2)).isPresent()) {
				return texture2;
			}
		}
		if(item instanceof ArmorItem armorItem) {
			Holder<ArmorMaterial> armorMaterial = armorItem.getMaterial();
			boolean isVanillaArmor = false;
			if (armorMaterial == ArmorMaterials.NETHERITE) {
				isVanillaArmor = true;
				texture += "netherite_";
			} else if (armorMaterial == ArmorMaterials.DIAMOND) {
				isVanillaArmor = true;
				texture += "diamond_";
			} else if (armorMaterial == ArmorMaterials.IRON) {
				isVanillaArmor = true;
				texture += "iron_";
			} else if (armorMaterial == ArmorMaterials.LEATHER) {
				isVanillaArmor = true;
				texture += "leather_";
			} else if (armorMaterial == ArmorMaterials.GOLD) {
				isVanillaArmor = true;
				texture += "gold_";
			} else if (armorMaterial == ArmorMaterials.CHAIN) {
				isVanillaArmor = true;
				texture += "chainmail_";
			} else if (armorMaterial == ArmorMaterials.TURTLE) {
				isVanillaArmor = true;
				texture += "turtle_";
			}

			if(isVanillaArmor || item instanceof EvilDragonArmorItem || item instanceof GoodDragonArmorItem) {
				if(isVanillaArmor) {
					texture += "dragon_";
				} else if(item instanceof EvilDragonArmorItem) {
					texture += "dragon_dark_";
				} else if(item instanceof GoodDragonArmorItem) {
					texture += "dragon_light_";
				}
				switch(equipmentSlot){
					case HEAD -> texture += "helmet";
					case CHEST -> texture += "chestplate";
					case LEGS -> texture += "leggings";
					case FEET -> texture += "boots";
				}
				texture += ".png";
				return stripInvalidPathChars(texture);
			}

			int defense = armorItem.getDefense();
			switch(equipmentSlot){
				case FEET -> texture += Mth.clamp(defense, 1, 4) + "_dragon_boots";
				case CHEST -> texture += Mth.clamp(defense / 2, 1, 4) + "_dragon_chestplate";
				case HEAD -> texture += Mth.clamp(defense, 1, 4) + "_dragon_helmet";
				case LEGS -> texture += Mth.clamp((int)(defense / 1.5), 1, 4) + "_dragon_leggings";
			}
			texture += ".png";
			return stripInvalidPathChars(texture);
		}
		return texture + "empty_armor.png";
	}
	
	public static String itemToResLoc(Item item) {
		if (item == Items.AIR) return null;
		return ResourceHelper.getKey(item).getPath();
    }
	public static String stripInvalidPathChars(String loc) {
		// filters certain characters (non [a-z0-9/._-]) to prevent crashes
		// this probably should never be relevant, but you can never be too safe
		loc = loc.chars()
			.filter(ch -> ResourceLocation.validPathChar((char) ch))
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
		return loc;
	}
}