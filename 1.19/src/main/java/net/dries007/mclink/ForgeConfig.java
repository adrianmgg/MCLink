/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
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

// TODO probably rename this to avoid confusion with `ForgeConfigSpec`
// TODO integrate this more into the new forge config system?
/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
public class ForgeConfig extends CommonConfig
{
    private static final String CATEGORY_SERVICES = "Services";
//    private final FileConfig cfg;
    private final CommentedFileConfig cfg;

    private static final String CATEGORY_GENERAL = "general";

    private static final Logger LOGGER = LogUtils.getLogger();

    ForgeConfig(File file)
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
        // TODO handle case where category present but wrong type
        CommentedConfig category = cfg.<CommentedConfig>get(List.of(categoryName));
        if(category != null) return category;
        else {
            CommentedConfig newCategory = CommentedConfig.inMemory();
            cfg.set(List.of(categoryName), newCategory);
            return newCategory;
        }
    }

    private <T> void _setDefaultComment(String comment, String category, String key)
    {
        List<String> path = List.of(category, key);
        @Nullable String curComment = cfg.getComment(path);
        if(curComment == null) {
            cfg.setComment(path, comment);
            // TODO check that setting the comment set actually stuck
        }
    }

    private <T> T _getOrDefaultAndPopulate(T defaultVal, String category, String key)
    {
        _getOrCreateCategory(category);
        List<String> pathList = List.of(category, key);
        // TODO handle case where key present on category but wrong value type
        T curVal = cfg.<T>get(pathList);
        if(curVal != null) return curVal;
        else {
            cfg.<T>set(pathList, defaultVal);
            return defaultVal;
        }
    }

    // TODO rename this
    @NotNull
    private <T> T _get(@NotNull T defaultVal, @NotNull String comment, @NotNull String category, @NotNull String key)
    {
        T ret = _getOrDefaultAndPopulate(defaultVal, category, key);
        _setDefaultComment(comment, category, key);
        return ret;
    }

    @Override
    protected String getString(String key, String def, String comment)
    {
        return _get(def, comment, CATEGORY_GENERAL, key);
    }

    @Override
    protected boolean getBoolean(String key, boolean def, String comment)
    {
        return _get(def, comment, CATEGORY_GENERAL, key);
    }

    @Override
    protected int getInt(String key, int def, int min, int max, String comment)
    {
        // TODO range not used
        return _get(def, comment, CATEGORY_GENERAL, key);
    }

    @Override
    protected void addService(String name, String comment)
    {
        _get(new String[0], comment, CATEGORY_SERVICES, name);
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
        // TODO ough
        return (List<String>[])(new List[]{_getOrDefaultAndPopulate(List.<String>of(), CATEGORY_SERVICES, name)});
    }

    @Override
    protected void setServiceComment(String name, String comment)
    {
        _setDefaultComment(comment, CATEGORY_SERVICES, name);
    }
}
