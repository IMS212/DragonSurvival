package by.jackraidenph.dragonsurvival.client.skinPartSystem.objects;

import by.jackraidenph.dragonsurvival.misc.DragonLevel;
import by.jackraidenph.dragonsurvival.misc.DragonType;

import java.util.HashMap;

public class SavedSkinPresets
{
	public HashMap<DragonType, HashMap<Integer, SkinPreset>> skinPresets = new HashMap<>();
	public HashMap<DragonType, HashMap<DragonLevel, Integer>> current = new HashMap<>();
}
