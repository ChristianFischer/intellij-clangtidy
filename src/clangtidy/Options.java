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

import com.intellij.ide.util.PropertiesComponent;

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
}
