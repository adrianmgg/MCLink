package net.dries007.mclink.common;

import com.google.common.collect.ImmutableList;
import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IMinecraft;
import net.dries007.mclink.binding.ISender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FunctionCommand implements ICommand
{
    private final String name;
    private final CommandRunFunction runFunction;
    private final String helpText;

    public FunctionCommand(String name, String helpText, CommandRunFunction runFunction)
    {
        this.name = name;
        this.runFunction = runFunction;
        this.helpText = helpText;
    }

    @Override
    public @NotNull String getUsage(ISender sender)
    {
        return name;
    }

    @Override
    public void run(@NotNull IMinecraft mc, @NotNull ISender sender, @NotNull String[] args) throws CommandException
    {
        runFunction.run(mc, sender, args);
    }

    @Override
    public @NotNull List<String> getTabOptions(@NotNull ISender sender, @NotNull String[] args)
    {
        return ImmutableList.of();
    }

    @Override
    public @NotNull List<ICommand> getSubCommands()
    {
        return ImmutableList.of();
    }

    @Override
    public @NotNull String getHelpText()
    {
        return helpText;
    }

    @Override
    public @NotNull String getName()
    {
        return name;
    }

    @FunctionalInterface
    public
    interface CommandRunFunction
    {
        void run(@NotNull IMinecraft mc, @NotNull ISender sender, @NotNull String[] args);
    }
}
