package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ILogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class Slf4jLogger implements ILogger
{
    private final Logger logger;

    public Slf4jLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void debug(String msg)
    {
        logger.debug(msg);
    }

    @Override
    public void info(String msg)
    {
        logger.info(msg);
    }

    @Override
    public void warn(String msg)
    {
        logger.warn(msg);
    }

    @Override
    public void error(String msg)
    {
        logger.error(msg);
    }

    @Override
    public void catching(Throwable error)
    {
        logger.error("CATCHING: ", error);
    }

    @Override
    public @NotNull String getName()
    {
        return logger.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        info(message);
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        info(message);
    }
}
