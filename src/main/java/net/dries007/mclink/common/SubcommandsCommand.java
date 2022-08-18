package net.dries007.mclink.common;

import com.google.common.collect.ImmutableList;
import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ICommand;
import net.dries007.mclink.binding.IMinecraft;
import net.dries007.mclink.binding.ISender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubcommandsCommand implements ICommand
{
    private final String name;
    private final List<ICommand> subcommands;
    private final String helpText;

    public SubcommandsCommand(String name, String helpText, ICommand... subcommands)
    {
        this.name = name;
        this.subcommands = Arrays.asList(subcommands);
        this.helpText = helpText;
    }

    @Override
    public @NotNull
    String getName()
    {
        return name;
    }

    @Override
    public @NotNull
    String getUsage(ISender sender)
    {
        StringBuilder sb = new StringBuilder(getName());
        if (!subcommands.isEmpty())
        {
            sb.append(" [");
            for (int i = 0; i < subcommands.size(); i++)
            {
                if (i > 0) sb.append("|");
                sb.append(subcommands.get(i).getUsage(sender));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public void run(@NotNull IMinecraft mc, @NotNull ISender sender, @NotNull String[] args) throws CommandException
    {
        if (args.length == 0) // if no arguments, print help text
        {
            if (!helpText.isEmpty())
            {
                sender.sendMessage(helpText, FormatCode.AQUA);
            }
            if (!subcommands.isEmpty())
            {
                sender.sendMessage("Subcommands:");
                for (ICommand cmd : subcommands)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("- ");
                    sb.append(cmd.getName());
                    String cmdHelp = cmd.getHelpText();
                    if (!cmdHelp.isEmpty())
                    {
                        sb.append(": ");
                        sb.append(cmdHelp);
                    }
                    sender.sendMessage(sb.toString(), FormatCode.AQUA);
                }
            }
        }
        else // otherwise, call relevant subcommand
        {
            String subcommandName = args[0];
            String[] subcommandArgs = Arrays.copyOfRange(args, 1, args.length);
            for (ICommand cmd : subcommands)
            {
                if (cmd.getName().equals(subcommandName))
                {
                    cmd.run(mc, sender, subcommandArgs);
                    return;
                }
            }
            // if we haven't returned yet then none of the subcommands matched
            throw new CommandException("Subcommand not found.");
        }
    }

    @Override
    public @NotNull
    List<String> getTabOptions(@NotNull ISender sender, @NotNull String[] args)
    {
        if (args.length == 1) return subcommands.stream().map(ICommand::getName).collect(Collectors.toList());
        else return ImmutableList.of();
    }

    @Override
    public @NotNull
    List<ICommand> getSubCommands()
    {
        return subcommands;
    }

    @Override
    public @NotNull
    String getHelpText()
    {
        return helpText;
    }
}
