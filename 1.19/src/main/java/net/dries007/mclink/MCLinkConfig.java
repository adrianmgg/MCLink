/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import net.dries007.mclink.api.APIException;
import net.dries007.mclink.common.CommonConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
public class MCLinkConfig extends CommonConfig
{
    private static final String CATEGORY_SERVICES = "Services";

    private final CommentedFileConfig cfg;

    private static final String CATEGORY_GENERAL = "general";

    private static final Logger LOGGER = LogUtils.getLogger();

    MCLinkConfig(File file)
    {
        this.cfg = CommentedFileConfig.of(file);
    }

    @Nullable
    @Override
    public String reload() throws ConfigException, IOException, APIException
    {
        cfg.load();
        String msg = super.reload();
        cfg.save();
        return msg;
    }

    @Override
    public boolean setClosed(boolean close)
    {
        cfg.set(List.of(CATEGORY_GENERAL, "closed"), close);
        cfg.save();
        return super.setClosed(close);
    }

    private CommentedConfig _getOrCreateCategory(String categoryName)
    {
        try {
            CommentedConfig category = cfg.get(List.of(categoryName));
            if (category != null) return category;
            else {
                CommentedConfig newCategory = CommentedConfig.inMemory();
                cfg.set(List.of(categoryName), newCategory);
                return newCategory;
            }
        } catch(ClassCastException e) {
            LOGGER.warn("config category {} didn't match expected type (CommentedConfig), replacing with default value.", categoryName);
            LOGGER.warn("", e);
            CommentedConfig newCategory = CommentedConfig.inMemory();
            cfg.set(List.of(categoryName), newCategory);
            return newCategory;
        }
    }

    private void _setDefaultComment(String comment, String category, String key)
    {
        List<String> path = List.of(category, key);
        @Nullable String curComment = cfg.getComment(path);
        if(curComment == null) {
            cfg.setComment(path, comment);
        }
    }

    private <T> T _getOrDefaultAndPopulate(T defaultVal, String category, String key)
    {
        _getOrCreateCategory(category);
        List<String> pathList = List.of(category, key);
        try {
            T curVal = cfg.get(pathList);
            if (curVal != null) return curVal;
            else {
                cfg.<T>set(pathList, defaultVal);
                return defaultVal;
            }
        } catch(ClassCastException e) {
            LOGGER.warn("config property {}.{} didn't match expected type, replacing with default value.", category, key);
            LOGGER.warn("", e);
            cfg.<T>set(pathList, defaultVal);
            return defaultVal;
        }
    }

    @NotNull
    private <T> T _getOrSetDefaultWithComment(@NotNull T defaultVal, @NotNull String comment, @NotNull String category, @NotNull String key)
    {
        T ret = _getOrDefaultAndPopulate(defaultVal, category, key);
        _setDefaultComment(comment, category, key);
        return ret;
    }

    @Override
    protected String getString(String key, String def, String comment)
    {
        return _getOrSetDefaultWithComment(def, comment, CATEGORY_GENERAL, key);
    }

    @Override
    protected boolean getBoolean(String key, boolean def, String comment)
    {
        return _getOrSetDefaultWithComment(def, comment, CATEGORY_GENERAL, key);
    }

    @Override
    protected int getInt(String key, int def, int min, int max, String comment)
    {
        int val = _getOrSetDefaultWithComment(def, comment, CATEGORY_GENERAL, key);
        int clamped = Math.max(min, Math.min(max, val));
        if(val == clamped) return val;
        else
        {
            LOGGER.warn("config property {}.{} was {}, which is outside its allowed range [{}, {}]. replacing with clamped value {}.", CATEGORY_GENERAL, key, val, min, max, clamped);
            cfg.set(List.of(CATEGORY_GENERAL, key), clamped);
            return clamped;
        }
    }

    @Override
    protected void addService(String name, String comment)
    {
        _getOrSetDefaultWithComment(List.<List<String>>of(), comment, CATEGORY_SERVICES, name);
    }

    @Override
    protected Set<String> getAllDefinedServices()
    {
        return _getOrCreateCategory(CATEGORY_SERVICES).entrySet().stream()
            .map(UnmodifiableConfig.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    protected void setGlobalCommentServices(String comment)
    {
        cfg.setComment(List.of(CATEGORY_SERVICES), comment);
    }

    @Override
    protected List<String>[] getServiceEntries(String name)
    {
        return _getOrDefaultAndPopulate(List.<List<String>>of(), CATEGORY_SERVICES, name)
            .toArray((IntFunction<List<String>[]>) List[]::new);
    }

    @Override
    protected void setServiceComment(String name, String comment)
    {
        _setDefaultComment(comment, CATEGORY_SERVICES, name);
    }
}
