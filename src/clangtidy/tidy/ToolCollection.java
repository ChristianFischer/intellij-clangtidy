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
package clangtidy.tidy;

import clangtidy.Options;
import clangtidy.tidy.tools.SimpleTool;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Helper class to retrieve a list of checks available in clang-tidy.
 */
public class ToolCollection {
	private static Set<String>		cachedToolNames;
	private static List<String>		blacklistedToolNames;

	static {
		blacklistedToolNames = new ArrayList<>();
		blacklistedToolNames.add("cert-");
		blacklistedToolNames.add("clang-analyzer-alpha");
		blacklistedToolNames.add("clang-analyzer-");
		blacklistedToolNames.add("cppcoreguidelines-");
		blacklistedToolNames.add("google-");
		blacklistedToolNames.add("llvm-");
		blacklistedToolNames.add("misc-");
	}


	private ToolCollection() {
	}



	private static boolean fetchToolsList() {
		if (cachedToolNames == null) {
			cachedToolNames = new HashSet<>();

			ProcessWrapper process = new ProcessWrapper(
					Options.getCLangTidyExe(),
					"-checks=*",
					"-list-checks"
			);

			process.setOutputConsumer(line -> {
				if (line.startsWith("    ")) {
					String item = line.trim();

					for (String blacklistedItem : blacklistedToolNames) {
						if (item.startsWith(blacklistedItem)) {
							return;
						}
					}

					cachedToolNames.add(item);
				}
			});

			try {
				return process.run();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			return false;
		}

		return true;
	}


	private static ToolController createToolForName(@NotNull ToolController[] knownControllers, @NotNull String toolName) {
		for(ToolController tc : knownControllers) {
			if (tc.getName().equals(toolName)) {
				return tc;
			}
		}

		return new SimpleTool(toolName);
	}


	/**
	 * Request the list of all available tools applicable with clang-tidy.
	 * The list will be fetched asynchronously by executing clang-tidy -list-checks.
	 * @param consumer		A consumer to receive the list of tools.
	 */
	public static void requestAvailableTools(@NotNull Consumer<List<ToolController>> consumer) {
		new Thread(() -> {
			ExtensionPointName<ToolController> tcExtensionPoint = new ExtensionPointName<>("com.your.company.unique.plugin.id.ToolController");
			ToolController[] extensions = Extensions.getExtensions(tcExtensionPoint);

			List<ToolController> tools = new ArrayList<>();

			if (cachedToolNames == null) {
				fetchToolsList();
			}

			assert cachedToolNames != null;

			for(String toolName : cachedToolNames) {
				ToolController tool = createToolForName(extensions, toolName);
				tools.add(tool);
			}

			consumer.accept(tools);
		}).start();
	}


	/**
	 * Clear cached data, when clang-tidy was changed.
	 */
	public static void clearCachedData() {
		cachedToolNames = null;
	}
}
