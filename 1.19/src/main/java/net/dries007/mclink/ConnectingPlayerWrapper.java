package net.dries007.mclink;

import com.mojang.authlib.GameProfile;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.IPlayer;
import net.minecraft.network.Connection;
import net.minecraftforge.event.entity.player.PlayerNegotiationEvent;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * like {@link net.dries007.mclink.common.Player}, but for players who are still connecting & therefore don't have an
 * associated entity to send messages to
 */
public class ConnectingPlayerWrapper implements IPlayer
{
//    public Player

    public final UUID uuid;

    public final String name;

    public final Connection connection;

    public ConnectingPlayerWrapper(UUID uuid, String name, Connection connection)
    {
        this.uuid = uuid;
        this.name = name;
        this.connection = connection;
    }

    @Override
    public @NotNull UUID getUuid()
    {
        return uuid;
    }

    @Override
    public @NotNull String getName()
    {
        return name;
    }

    // these aren't ever called on an IPlayer as far as I can tell, but i'll put an exception here just in case
    // since that'll be a lot easier to find the origin of than "why aren't these messages sending"
    @Override
    public void sendMessage(String message)
    {
        throw new NotImplementedException("sendMessage() not implemented for ConnectingPlayer");
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        throw new NotImplementedException("IPlayer sendMessage() not implemented for ConnectingPlayer");
    }

    @Override
    public String toString()
    {
        return "ConnectingPlayerWrapper{" +
            "name='" + name + '\'' +
            ", uuid=" + uuid +
            '}';
    }
}
