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
package clangtidy.util;

import clangtidy.tidy.CompileCommandsNotFoundException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Helper class to fix missing compile commands within the current cmake project.
 */
public class FixCompileCommandsUtil {
	public static void askToFixMissingCompileCommands(@NotNull Project project, @NotNull CompileCommandsNotFoundException e) {
		int result = Messages.showYesNoDialog(
				project,
				"There is no compile_commands.json file in your project's build directory.\n"
				+ "This file is required for clang-tidy to process your source files. "
				+ "CMake will create this file, when adding the option CMAKE_EXPORT_COMPILE_COMMANDS=ON to your project"
				+ "\n\n"
				+ "Should this option being added to your current configuration?\n"
				+ "This will cause your project configuration being updated and your cmake settings being reloaded.",
				"Missing compile_commands.json",
				Messages.getErrorIcon()
		);

		if (result == Messages.YES) {
			fixMissingCompileCommands(e);
		}
	}


	public static void fixMissingCompileCommands(@NotNull CompileCommandsNotFoundException e) {
		CMakeWorkspace workspace = e.getCMakeWorkspace();
		String options = workspace.getSettings().getGenerationOptions();
		String option = "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON";

		if (options == null) {
			options = option;
		}
		else {
			options += " " + option;
		}

		workspace.getSettings().setGenerationOptions(options);
	}


	public static boolean needsToFixWindowsPaths(@NotNull File compileCommandsFile) {
		if (SystemInfo.isWindows) {
			try(InputStream in = new FileInputStream(compileCommandsFile)) {
				int c;

				while((c = in.read()) != -1) {
					if (c == '\\') {
						return true;
					}
				}
			}
			catch (IOException e) {
				Logger.getInstance(FixCompileCommandsUtil.class).error(e);
			}
		}

		return false;
	}


	public static void fixWindowsPaths(@NotNull File compileCommandsFile) {
		assert compileCommandsFile.exists();
		byte[] content;

		// read the file's full content
		try(InputStream in = new FileInputStream(compileCommandsFile)) {
			content = new byte[(int)compileCommandsFile.length()];
			int readCount = in.read(content);
			assert readCount == compileCommandsFile.length();
		}
		catch (IOException e) {
			Logger.getInstance(FixCompileCommandsUtil.class).error(e);
			content = null;
		}

		if (content == null) {
			return;
		}

		// replace all backslash with slash
		for(int i=content.length; --i>=0;) {
			if (content[i] == '\\') {
				content[i] = '/';
			}
		}

		// write back to the file
		try(OutputStream out = new FileOutputStream(compileCommandsFile)) {
			out.write(content);
		}
		catch (IOException e) {
			Logger.getInstance(FixCompileCommandsUtil.class).error(e);
		}
	}
}
