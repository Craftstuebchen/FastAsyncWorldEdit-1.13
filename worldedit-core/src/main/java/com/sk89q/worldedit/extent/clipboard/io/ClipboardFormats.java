/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.extent.clipboard.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.clipboard.LazyClipboardHolder;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClipboardFormats {

    private static final Map<String, ClipboardFormat> aliasMap = new HashMap<>();
    private static final Multimap<String, ClipboardFormat> fileExtensionMap = HashMultimap.create();
    private static final List<ClipboardFormat> registeredFormats = new ArrayList<>();

    public static void registerClipboardFormat(ClipboardFormat format) {
        checkNotNull(format);

        for (String key : format.getAliases()) {
            String lowKey = key.toLowerCase(Locale.ENGLISH);
            ClipboardFormat old = aliasMap.put(lowKey, format);
            if (old != null) {
                aliasMap.put(lowKey, old);
                WorldEdit.logger.warn(format.getClass().getName() + " cannot override existing alias '" + lowKey + "' used by " + old.getClass().getName());
            }
        }
        for (String ext : format.getFileExtensions()) {
            String lowExt = ext.toLowerCase(Locale.ENGLISH);
            fileExtensionMap.put(lowExt, format);
        }
        registeredFormats.add(format);
    }

    static {
        for (BuiltInClipboardFormat format : BuiltInClipboardFormat.values()) {
            registerClipboardFormat(format);
        }
    }

    /**
     * Find the clipboard format named by the given alias.
     *
     * @param alias
     *            the alias
     * @return the format, otherwise null if none is matched
     */
    @Nullable
    public static ClipboardFormat findByAlias(String alias) {
        checkNotNull(alias);
        return aliasMap.get(alias.toLowerCase(Locale.ENGLISH).trim());
    }

    /**
     * Detect the format of given a file.
     *
     * @param file
     *            the file
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByFile(File file) {
        checkNotNull(file);

        for (ClipboardFormat format : registeredFormats) {
            if (format.isFormat(file)) {
                return format;
            }
        }

        return null;
    }

    /**
     * @return a multimap from a file extension to the potential matching formats.
     */
    public static Multimap<String, ClipboardFormat> getFileExtensionMap() {
        return Multimaps.unmodifiableMultimap(fileExtensionMap);
    }

    public static Collection<ClipboardFormat> getAll() {
        return Collections.unmodifiableCollection(registeredFormats);
    }

    /**
     * Not public API, only used by SchematicCommands.
     * It is not in SchematicCommands because it may rely on internal register calls.
     */
    public static String[] getFileExtensionArray() {
        return fileExtensionMap.keySet().toArray(new String[fileExtensionMap.keySet().size()]);
    }

    private ClipboardFormats() {
    }

}
