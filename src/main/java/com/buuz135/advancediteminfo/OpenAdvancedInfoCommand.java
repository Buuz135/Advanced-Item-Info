package com.buuz135.advancediteminfo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;

public class OpenAdvancedInfoCommand extends AbstractCommand {

    private final OptionalArg<String> argument;

    protected OpenAdvancedInfoCommand() {
        super("advancedinfo", "Displays all the items in the game and shows all the info from them", false);
        this.addAliases("aii", "iteminfo");
        var arg = new SingleArgumentType<String>("Default Search", "Opens the screen with this text in the search field", "iron", "stone") {
            @NullableDecl
            @Override
            public String parse(String s, ParseResult parseResult) {
                return s;
            }
        };
        this.argument = this.withOptionalArg("s", "Searches items that have this name", arg);
        this.setPermissionGroup(GameMode.Adventure);
    }


    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        CommandSender sender = context.sender();
        if (sender instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                return CompletableFuture.runAsync(() -> {
                    PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
                    var defaultSearch = "";
                    if (context.get(this.argument) != null) {
                        defaultSearch = context.get(this.argument);
                    }
                    if (playerRefComponent != null) {
                        player.getPageManager().openCustomPage(ref, store, new AdvancedItemInfoGui(playerRefComponent, CustomPageLifetime.CanDismiss, defaultSearch));

                    }
                }, world);
            } else {
                context.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
