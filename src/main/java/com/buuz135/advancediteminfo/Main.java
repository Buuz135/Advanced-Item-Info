package com.buuz135.advancediteminfo;



import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin {

    public static Map<String, Item> ITEMS = new HashMap<>();
    public static final Map<String, Map<String, com.hypixel.hytale.protocol.CraftingRecipe.BenchRequirement[]>> recipeRegistries = new Object2ObjectOpenHashMap();

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new OpenAdvancedInfoCommand());
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, Main::onItemAssetLoad);
        this.getEventRegistry().register(LoadedAssetsEvent.class, CraftingRecipe.class, Main::onRecipeLoad);
        this.getEventRegistry().register(RemovedAssetsEvent.class, CraftingRecipe.class, Main::onRecipeRemove);
    }

    private static void onItemAssetLoad(LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        ITEMS = event.getLoadedAssets();
    }

    private static void onRecipeLoad(LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) {
        for(CraftingRecipe recipe : event.getLoadedAssets().values()) {
            for (MaterialQuantity output : recipe.getOutputs()) {
                if (recipeRegistries.containsKey(output.getItemId())){
                    recipeRegistries.get(output.getItemId()).remove(recipe.getId());
                }
            }

            if (recipe.getBenchRequirement() != null) {
                for (MaterialQuantity output : recipe.getOutputs()) {
                    if (!recipeRegistries.containsKey(output.getItemId())) {
                        recipeRegistries.put(output.getItemId(), new HashMap<>());
                    }
                    recipeRegistries.get(output.getItemId()).put(recipe.getId(), recipe.getBenchRequirement());
                }
            }
        }
    }

    private static void onRecipeRemove(RemovedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event) {
        for (String key : recipeRegistries.keySet()) {
            for (String removedAsset : event.getRemovedAssets()) {
                recipeRegistries.get(key).remove(removedAsset);
            }
        }


    }
}