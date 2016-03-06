/**
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
package clangtidy;

import clangtidy.tidy.ToolController;
import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A helper class to access the plugin's settings.
 */
public class Options {
	public final static String OPTION_KEY_CLANGTIDY_EXE		= "clangtidy.exe";

	private final static PropertiesComponent properties = PropertiesComponent.getInstance();


	public static String getCLangTidyExe() {
		// todo: default path on unix - add proper path for windows
		String defaultValue = "/usr/bin/clang-tidy";

		return properties.getValue(OPTION_KEY_CLANGTIDY_EXE, defaultValue);
	}


	public static void setCLangTidyExe(String exe) {
		properties.setValue(OPTION_KEY_CLANGTIDY_EXE, exe);
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


	public static @Nullable String getToolProperty(@NotNull ToolController tool, @NotNull String property) {
		String propertyName = getPropertyName(tool, property);
		if (properties.isValueSet(propertyName)) {
			return properties.getValue(propertyName);
		}

		return null;
	}


	public static @NotNull <T> T getToolProperty(@NotNull ToolController tool, @NotNull String property, @NotNull T defaultValue) {
		String stringVal = getToolProperty(tool, property);

		if (stringVal != null) {
			try {
				Class<?> clazz = defaultValue.getClass();
				Method m = clazz.getMethod("valueOf", String.class);
				if (m != null) {
					Object value = m.invoke(null, stringVal);

					if (clazz.isInstance(value)) {
						@SuppressWarnings("unchecked")
						T tValue = (T)value;
						return tValue;
					}
				}
			}
			catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
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
