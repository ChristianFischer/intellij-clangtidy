/*
 * Copyright (C) 2017
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

package de.wieselbau.clion.clangtidy.results;

import de.wieselbau.clion.clangtidy.tidy.ScannerResult;
import de.wieselbau.clion.clangtidy.tidy.ScannerResultUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Base class to test clang-tidy result files
 */
public abstract class AbstractTidyResultsTest
{
	protected @NotNull ScannerResult parseResultsFromFile(@NotNull File source) {
		ScannerResult result = new ScannerResult();
		ScannerResultUtil resultUtil = new ScannerResultUtil(result);

		resultUtil.readFixesList(source);

		return result;
	}


	/**
	 * Get the path of the given file object with unix-style separators
	 * to make tests more platform independent.
	 */
	protected static @NotNull String asUnixPath(@NotNull File file) {
		return file.getPath().replace(File.separatorChar, '/');
	}


	/**
	 * Get the path of the given file object with windows-style separators
	 * to make tests more platform independent.
	 */
	protected static @NotNull String asWindowsPath(@NotNull File file) {
		return file.getAbsolutePath().replace(File.separatorChar, '\\');
	}
}