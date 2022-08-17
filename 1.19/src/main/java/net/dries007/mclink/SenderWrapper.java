/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ISender;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatSender;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
public abstract class SenderWrapper implements ISender
{
    public static class OfCommandSourceStack extends SenderWrapper
    {
        private final CommandSourceStack sender;

        OfCommandSourceStack(CommandSourceStack sender)
        {
            this.sender = sender;
        }

        @NotNull
        @Override
        public String getName()
        {
            return sender.getTextName();
        }

        @Override
        public void sendMessage(String message)
        {
            // TODO not all the sendMessage calls are for successes
            sender.sendSuccess(Component.literal(message), true);
        }

        @Override
        public void sendMessage(String message, FormatCode formatCode)
        {
            sender.sendSuccess(Component.literal(message).withStyle(getFormatCode(formatCode)), true);
        }
    }

    public static class OfPlayer extends SenderWrapper
    {
        private final Player sender;

        OfPlayer(Player sender)
        {
            this.sender = sender;
        }

        @NotNull
        @Override
        public String getName()
        {
            return sender.getGameProfile().getName();
        }

        @Override
        public void sendMessage(String message)
        {
            // TODO not all the sendMessage calls are for successes
            sender.sendSystemMessage(Component.literal(message));
        }

        @Override
        public void sendMessage(String message, FormatCode formatCode)
        {
            sender.sendSystemMessage(Component.literal(message).withStyle(getFormatCode(formatCode)));
        }
    }

    private static ChatFormatting getFormatCode(FormatCode formatCode)
    {
        switch (formatCode)
        {
            case BLACK:
                return ChatFormatting.BLACK;
            case DARK_BLUE:
                return ChatFormatting.DARK_BLUE;
            case DARK_GREEN:
                return ChatFormatting.DARK_GREEN;
            case DARK_AQUA:
                return ChatFormatting.DARK_AQUA;
            case DARK_RED:
                return ChatFormatting.DARK_RED;
            case DARK_PURPLE:
                return ChatFormatting.DARK_PURPLE;
            case GOLD:
                return ChatFormatting.GOLD;
            case GRAY:
                return ChatFormatting.GRAY;
            case DARK_GRAY:
                return ChatFormatting.DARK_GRAY;
            case BLUE:
                return ChatFormatting.BLUE;
            case GREEN:
                return ChatFormatting.GREEN;
            case AQUA:
                return ChatFormatting.AQUA;
            case RED:
                return ChatFormatting.RED;
            case LIGHT_PURPLE:
                return ChatFormatting.LIGHT_PURPLE;
            case YELLOW:
                return ChatFormatting.YELLOW;
            case WHITE:
                return ChatFormatting.WHITE;
            case OBFUSCATED:
                return ChatFormatting.OBFUSCATED;
            case BOLD:
                return ChatFormatting.BOLD;
            case STRIKETHROUGH:
                return ChatFormatting.STRIKETHROUGH;
            case UNDERLINE:
                return ChatFormatting.UNDERLINE;
            case ITALIC:
                return ChatFormatting.ITALIC;
            case RESET:
                return ChatFormatting.RESET;
        }
        throw new RuntimeException("Enum constant has magically disappeared?");
    }

    @Override
    public String toString()
    {
        return "SenderWrapper{" + getName() + "}";
    }
}
