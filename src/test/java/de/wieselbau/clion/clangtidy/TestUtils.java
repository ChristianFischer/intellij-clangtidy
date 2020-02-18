/*
 * Copyright (C) 2020
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

import de.wieselbau.clion.clangtidy.results.AbstractTidyResultsTest;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;


/**
 * A collection of utility functions to help on running tests.
 */
public class TestUtils {
	/**
	 * Reads a file with a given name from the test resources folder.
	 * @param filename The filename and path relative to the test resources root
	 * @return A file object for the requested file.
	 * @throws NullPointerException when the requested resource was not found.
	 */
	public static @NotNull File getTestFile(@NotNull String filename) {
		ClassLoader classLoader = AbstractTidyResultsTest.class.getClassLoader();
		String f = Objects.requireNonNull(classLoader.getResource(filename)).getFile();
		return new File(f);
	}
}
