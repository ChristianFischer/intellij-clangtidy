/*
 * Copyright (C) 2016
 * Christian Fischer
 *
 * https://bitbucket.org/baldur/clion-clangtidy/
 *
 * This plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301 USA
 */

package de.wieselbau.clion.clangtidy;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.wieselbau.clion.clangtidy.tidy.ToolCollection;
import de.wieselbau.clion.clangtidy.tidy.ToolController;
import de.wieselbau.util.properties.TypeConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A helper class to access the plugin's settings.
 */
public class Options {
	public final static String OPTION_KEY_CLANGTIDY_EXE		= "clangtidy.exe";

	private final static PropertiesComponent properties = PropertiesComponent.getInstance();


	public static void setCLangTidyExe(@NotNull String exe) {
		if (!Objects.equals(exe, getCLangTidyExe())) {
			properties.setValue(OPTION_KEY_CLANGTIDY_EXE, exe);
			ToolCollection.clearCachedData();
		}
	}


	public static String getCLangTidyExe() {
		// todo: default path on unix - add proper path for windows
		String defaultValue = "/usr/bin/clang-tidy";

		return properties.getValue(OPTION_KEY_CLANGTIDY_EXE, defaultValue);
	}


	/**
	 * Checks if the path to the clang-tidy executable is configured and the file exists.
	 */
	public static boolean isCLangTidyReady() {
		if (!getCLangTidyExe().isEmpty()) {
			VirtualFile file = LocalFileSystem.getInstance().findFileByPath(getCLangTidyExe());

			if (file != null) {
				return file.exists();
			}
		}

		return false;
	}



	public static boolean isToolEnabled(@NotNull ToolController tool) {
		return properties.isTrueValue(tool.getName() + ".enabled");
	}

	public static void setToolEnabled(@NotNull ToolController tool, boolean enabled) {
		properties.setValue(tool.getName() + ".enabled", enabled);
	}


	private static @NotNull String getPropertyName(@NotNull ToolController tool, @NotNull String property) {
		return tool.getName() + ".property." + property;
	}


	public static boolean hasToolProperty(@NotNull ToolController tool, @NotNull String property) {
		String propertyName = getPropertyName(tool, property);
		return properties.isValueSet(propertyName);
	}


	public static @Nullable String getToolProperty(@NotNull ToolController tool, @NotNull String property) {
		String propertyName = getPropertyName(tool, property);
		if (properties.isValueSet(propertyName)) {
			return properties.getValue(propertyName);
		}

		return null;
	}


	public static @NotNull <T> T getToolProperty(@NotNull ToolController tool, @NotNull String property, @NotNull T defaultValue) {
		if (hasToolProperty(tool, property)) {
			try {
				String stringVal = getToolProperty(tool, property);

				Class<?> clazz = defaultValue.getClass();
				Object value = TypeConverter.convertTo(defaultValue.getClass(), stringVal);

				if (clazz.isInstance(value)) {
					@SuppressWarnings("unchecked")
					T tValue = (T) value;
					return tValue;
				}
			}
			catch (Exception e) {
				Logger.getInstance(Options.class).error(e);
			}
		}

		return defaultValue;
	}


	public static void setToolProperty(@NotNull ToolController tool, @NotNull String property, @Nullable String value) {
		if (value == null) {
			properties.unsetValue(getPropertyName(tool, property));
		}
		else {
			properties.setValue(getPropertyName(tool, property), value);
		}
	}


	public static <T> void setToolProperty(@NotNull ToolController tool, @NotNull String property, @Nullable T value) {
		String stringVal = (value!=null ? value.toString() : null);
		setToolProperty(tool, property, stringVal);
	}
}
