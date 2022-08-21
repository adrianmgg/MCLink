/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.google.common.collect.ImmutableCollection;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.dries007.mclink.api.Authentication;
import net.dries007.mclink.api.Constants;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IPlayer;
import net.dries007.mclink.common.MCLinkCommon;
import net.dries007.mclink.common.ThreadStartConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerNegotiationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.UUID;

/**
 * @author Dries007
 */
@Mod(Constants.MODID)
public class MCLink extends MCLinkCommon
{
    private MinecraftServer server;

    private static final Logger LOGGER = LogUtils.getLogger();

    public MCLink()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommandsEvent(RegisterCommandsEvent event) {
        super.registerCommands(command -> {
            event.getDispatcher().register(buildCommand(command));
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildCommand(ICommand command) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(command.getName())
            .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_ADMINS))
            .executes(context -> {
                try
                {
                    command.run(this, new SenderWrapper.OfCommandSourceStack(context.getSource()), new String[0]);
                }
                catch (ICommand.CommandException e)
                {
                    context.getSource().sendFailure(Component.literal(e.getMessage()));
                    LOGGER.error("encountered error running mclink command", e);
                }
                return 0;
            });
        for(ICommand subcommand : command.getSubCommands())
        {
            builder = builder.then(buildCommand(subcommand));
        }
        return builder;
    }

    public void commonSetup(FMLCommonSetupEvent event)
    {
        IModInfo ourInfo = ModList.get().getModContainerByObject(this).orElseThrow().getModInfo();
        super.setModVersion(ourInfo.getVersion().toString());
        super.setMcVersion(SharedConstants.VERSION_STRING);
        super.setBranding(ourInfo.getDisplayName());
        super.setLogger(new Slf4jLogger(LOGGER));
        super.setConfig(new MCLinkConfig(FMLPaths.CONFIGDIR.get().resolve("mclink.toml").toFile()));
    }

    public void serverSetup(FMLDedicatedServerSetupEvent event)
    {
        super.setSide(Side.SERVER);
        try {
            super.init();
        } catch (Throwable t) {
            LOGGER.error("error initializing mclink", t);
        }
    }

    public void clientSetup(FMLClientSetupEvent event)
    {
        super.setSide(Side.CLIENT);
        try {
            super.init();
        } catch (Throwable t) {
            LOGGER.error("error initializing mclink", t);
        }
    }

    @SubscribeEvent
    public void serverStarting(ServerStartingEvent event)
    {
        server = event.getServer();
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event)
    {
        server = null;
        super.deInit();
    }

    private net.dries007.mclink.common.Player getPlayerFromEntity(net.minecraft.world.entity.player.Player player)
    {
        return new net.dries007.mclink.common.Player(new SenderWrapper.OfPlayer(player), player.getGameProfile().getName(), player.getGameProfile().getId());
    }

    @SubscribeEvent
    public void onPlayerNegotiation(PlayerNegotiationEvent event)
    {
        GameProfile gp = event.getProfile();
        PlayerList pl = server.getPlayerList();
        boolean op = pl.canBypassPlayerLimit(gp) || server.getProfilePermissions(gp) > 0;
        boolean wl = pl.getWhiteList().isWhiteListed(gp);
        super.checkAuthStatusAsync(new ConnectingPlayerWrapper(gp.getId(), gp.getName(), event.getConnection()), op, wl, event::enqueueWork);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        super.login(getPlayerFromEntity(event.getEntity()), server.getPlayerList().isOp(event.getEntity().getGameProfile()));
    }

    @Override
    protected void authCompleteAsync(IPlayer player, ImmutableCollection<Authentication> authentications, Marker result)
    {
        // Don't kick players unless result was one of the DENIED_* values
        if(result != Marker.ALLOWED)
        {
            // if the player is still connecting we can't just get them via the player list (since they haven't
            // finished connecting yet), so we need to special case how we disconnect them to use the Connection we got
            // from the PlayerNegotiationEvent instead.
            if(player instanceof ConnectingPlayerWrapper) {
                Connection connection = ((ConnectingPlayerWrapper)player).connection;
                Component msg = Component.literal(getConfig().getMessage(result));
                connection.send(new ClientboundLoginDisconnectPacket(msg));
                connection.disconnect(msg);
            } else {
                ServerPlayer p = server.getPlayerList().getPlayer(player.getUuid());
                if(p != null) // The player may have disconnected before this could happen.
                    p.connection.disconnect(Component.literal(getConfig().getMessage(result)));
            }
        }
        // Fire the event in all cases
        MinecraftForge.EVENT_BUS.post(new MCLinkAuthEvent(player.getUuid(), authentications, result));
    }

    @Nullable
    @Override
    protected String nameFromUUID(UUID uuid)
    {
        return server.getProfileCache().get(uuid).map(GameProfile::getName).orElse(null);
    }

    @Override
    public void sendMessage(String message)
    {
        Component m = Component.translatable("chat.type.admin", Constants.MODNAME, message)
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        if (server.shouldInformAdmins())
        {
            for (ServerPlayer p : server.getPlayerList().getPlayers())
            {
                if(server.getPlayerList().isOp(p.getGameProfile())) {
                    p.sendSystemMessage(m);
                }
            }
        }
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sendMessage(message);
    }
}
