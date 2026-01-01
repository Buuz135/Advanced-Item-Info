package com.buuz135.advancediteminfo;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.CraftingRecipe;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.protocol.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.MatchResult;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AdvancedItemInfoGui extends InteractiveCustomUIPage<AdvancedItemInfoGui.SearchGuiData> {

    private String searchQuery = "";
    private final Map<String, Item> visibleItems = new HashMap<>();

    public AdvancedItemInfoGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String defaultSearchQuery) {
        super(playerRef, lifetime, SearchGuiData.CODEC);
        this.searchQuery = defaultSearchQuery;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/Buuz135_AdvancedItemInfo_Gui.ui");
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);
        this.buildList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchGuiData data) {
        super.handleDataEvent(ref, store, data);
        if (data.item != null) {
            this.sendUpdate();
        }
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        HashMap<String, Item> itemList = new HashMap<>(Main.ITEMS);
        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());

        assert playerComponent != null;

        if (this.searchQuery.isEmpty()) {
            this.visibleItems.clear();
            this.visibleItems.putAll(itemList);
            //Collections.sort(this.visibleCommands);
        } else {
            ObjectArrayList<SearchResult> results = new ObjectArrayList<>();

            for (Map.Entry<String, Item> entry : itemList.entrySet()) {
                if (entry.getValue() != null) {
                    results.add(new SearchResult(entry.getKey(), MatchResult.EXACT));
                }
            }

            String[] terms = this.searchQuery.split(" ");

            for (int termIndex = 0; termIndex < terms.length; ++termIndex) {
                String term = terms[termIndex].toLowerCase(Locale.ENGLISH);

                for (int cmdIndex = results.size() - 1; cmdIndex >= 0; --cmdIndex) {
                    SearchResult result = results.get(cmdIndex);
                    Item item = itemList.get(result.name);
                    MatchResult match = MatchResult.NONE;
                    if (item != null) {
                        var message = I18nModule.get().getMessage(this.playerRef.getLanguage(), item.getTranslationKey());
                        match = message != null && message.toLowerCase(Locale.ENGLISH).contains(term) ? MatchResult.EXACT : MatchResult.NONE;
                    }

                    if (match == MatchResult.NONE) {
                        results.remove(cmdIndex);
                    } else {
                        result.match = result.match.min(match);
                    }
                }
            }

            results.sort(SearchResult.COMPARATOR);
            this.visibleItems.clear();

            for (SearchResult result : results) {
                this.visibleItems.put(result.name, itemList.get(result.name));
            }
        }
        this.buildButtons(this.visibleItems, playerComponent, commandBuilder, eventBuilder);
    }

    private void buildButtons(Map<String, Item> items, @Nonnull Player playerComponent, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#SubcommandCards");
        commandBuilder.set("#SubcommandSection.Visible", true);
        int rowIndex = 0;
        int cardsInCurrentRow = 0;

        for (Map.Entry<String, Item> entry : items.entrySet()) {
            Item item = entry.getValue();

            if (cardsInCurrentRow == 0) {
                commandBuilder.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 0); }");
            }

            commandBuilder.append("#SubcommandCards[" + rowIndex + "]", "Pages/Buuz135_AdvancedItemInfo_SearchItemIcon.ui");

            /*commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipText", Message.join(
                    Message.translation(item.getTranslationKey()),
                    Message.raw("\n"),
                    Message.translation(item.getTranslationKey())));*/

            var tooltip = MessageHelper.multiLine();
            tooltip.append(Message.translation(item.getTranslationKey()).bold(true)).nl();
            tooltip.separator();
            tooltip = addTooltipLine(tooltip, "ID: " , entry.getKey());
            tooltip = addTooltipLine(tooltip, "Icon: " , item.getIcon());
            tooltip = addTooltipLine(tooltip, "Quality: " , item.getQualityIndex());
            tooltip = addTooltipLine(tooltip, "Item Level: " , item.getItemLevel());
            tooltip = addTooltipLine(tooltip, "Max Stack: " , item.getMaxStack());
            tooltip = addTooltipLine(tooltip, "Max Durability: " , item.getMaxDurability());
            tooltip = addTooltipLine(tooltip, "Is Consumable: " , formatBoolean(item.isConsumable()));
            tooltip = addTooltipLine(tooltip, "Has Block: " , formatBoolean(item.hasBlockType()));
            tooltip = addTooltipLine(tooltip, "Fuel Quality: " , item.getFuelQuality());
            tooltip = tooltip.separator();
            tooltip = addTooltipLine(tooltip, "Is Tool: " , formatBoolean(item.getTool() != null));
            tooltip = addTooltipLine(tooltip, "Is Weapon: " , formatBoolean(item.getWeapon() != null));
            tooltip = addTooltipLine(tooltip, "Is Armor: " , formatBoolean(item.getArmor() != null));
            tooltip = addTooltipLine(tooltip, "Is Glider: " , formatBoolean(item.getGlider() != null));
            tooltip = addTooltipLine(tooltip, "Is Utility: " , formatBoolean(item.getUtility() != null));
            tooltip = addTooltipLine(tooltip, "Is Portal Key: " , formatBoolean(item.getPortalKey() != null));
            if (Main.recipeRegistries.containsKey(entry.getKey())) {
                tooltip = tooltip.separator();
                tooltip = addTooltipLine(tooltip, "Can Be Made In:" ,"");
                List<String> addedRecipes = new ArrayList<>();
                for (CraftingRecipe.BenchRequirement[] value : Main.recipeRegistries.get(entry.getKey()).values()) {
                    for (CraftingRecipe.BenchRequirement benchRequirement : value) {
                        var customId = benchRequirement.id + benchRequirement.requiredTierLevel;
                        if (!addedRecipes.contains(customId)) {
                            tooltip = addTooltipLine(tooltip, " - " , formatBench(benchRequirement.id) + " Tier " + benchRequirement.requiredTierLevel);
                            addedRecipes.add(customId);
                        }
                    }
                }
            }


            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip.build());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", entry.getKey());
            commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", Message.translation(item.getTranslationKey()));
            //commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #SubcommandUsage.TextSpans", this.getSimplifiedUsage(item, playerComponent));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "]", EventData.of("Item", entry.getKey()));
            ++cardsInCurrentRow;
            if (cardsInCurrentRow >= 7) {
                cardsInCurrentRow = 0;
                ++rowIndex;
            }
        }


        //commandBuilder.set("#BackButton.Visible", !this.subcommandBreadcrumb.isEmpty());
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, int value) {
        return this.addTooltipLine(tooltip, key, value + "");
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, double value) {
        return this.addTooltipLine(tooltip, key, value + "");
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, String value) {
        return tooltip.append(Message.raw(key).color("#93844c").bold(true)).append(Message.raw(value)).nl();
    }

    private MessageHelper.ML addTooltipLine(MessageHelper.ML tooltip, String key, Message value) {
        return tooltip.append(Message.raw(key).color("#93844c").bold(true)).append(value).nl();
    }

    private Message formatBoolean(boolean value){
        return value ? Message.raw("Yes").color(Color.GREEN) : Message.raw("No").color(Color.RED);
    }

    private String formatBench(String name){
        name = name.replaceAll("_", " ");
        if (!name.contains("Bench")){
            name += " Bench";
        }
        return name;
    }

    public static class SearchGuiData {
        static final String KEY_ITEM = "Item";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        public static final BuilderCodec<SearchGuiData> CODEC = BuilderCodec.<SearchGuiData>builder(SearchGuiData.class, SearchGuiData::new)
                .addField(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (searchGuiData, s) -> searchGuiData.searchQuery = s, searchGuiData -> searchGuiData.searchQuery)
                .addField(new KeyedCodec<>(KEY_ITEM, Codec.STRING), (searchGuiData, s) -> searchGuiData.item = s, searchGuiData -> searchGuiData.item).build();

        private String item;
        private String searchQuery;

    }

    private static class SearchResult {
        public static final Comparator<SearchResult> COMPARATOR = Comparator.comparing((o) -> o.match);
        private final String name;
        private MatchResult match;

        public SearchResult(String name, MatchResult match) {
            this.name = name;
            this.match = match;
        }
    }

}
