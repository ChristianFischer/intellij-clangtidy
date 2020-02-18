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

package de.wieselbau.clion.clangtidy.tidy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import de.wieselbau.clion.clangtidy.Options;
import de.wieselbau.clion.clangtidy.tidy.tools.SimpleTool;
import de.wieselbau.util.yaml.YamlReader;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Helper class to retrieve a list of checks available in clang-tidy.
 */
public class ToolCollection {
	private static Set<String>		cachedToolNames;
	private static List<String>		blacklistedToolNames;
	private static Properties		defaultProperties;

	static {
		blacklistedToolNames = new ArrayList<>();
		blacklistedToolNames.add("cert-");
		blacklistedToolNames.add("clang-analyzer-alpha");
		blacklistedToolNames.add("clang-analyzer-");
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
				Log.clangtidy.debug(line);

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

			process.setErrorConsumer(
					Log.clangtidy::warn
			);

			try {
				Log.clangtidy.info("Run command: " + process.getCommand());
				return process.run();
			}
			catch (IOException e) {
				Logger.getInstance(ToolCollection.class).error(e);
			}

			return false;
		}

		return true;
	}


	private static boolean fetchDefaultConfig() {
		if (defaultProperties == null) {
			defaultProperties = new Properties();

			ProcessWrapper process = new ProcessWrapper(
					Options.getCLangTidyExe(),
					"-checks=*",
					"-dump-config"
			);

			final StringBuilder configYaml = new StringBuilder();
			boolean result;

			process.setOutputConsumer(line -> {
				Log.clangtidy.debug(line);
				configYaml.append(line);
				configYaml.append('\n');
			});

			process.setErrorConsumer(
					Log.clangtidy::warn
			);

			try {
				Log.clangtidy.info("Run command: " + process.getCommand());
				result = process.run();
			}
			catch (IOException e) {
				Logger.getInstance(ToolCollection.class).error(e);
				result = false;
			}

			if (result && configYaml.length() != 0) {
				try {
					YamlReader yaml = new YamlReader(new ByteArrayInputStream(configYaml.toString().getBytes()));
					boolean yamlSuccessful = false;

					if (yaml.getRootObject() instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> root = (Map<String,Object>)yaml.getRootObject();
						Object optionsObject = root.get("CheckOptions");

						if (optionsObject != null && optionsObject instanceof List) {
							for(Object optionObject : (List)optionsObject) {
								if (optionObject instanceof Map) {
									@SuppressWarnings("unchecked")
									Map<String,Object> option = (Map<String,Object>)optionObject;
									Object key   = option.get("key");
									Object value = option.get("value");

									if (key!=null && value!=null) {
										defaultProperties.put(key, value);
										yamlSuccessful = true;
									}
								}
							}
						}
					}

					result = yamlSuccessful;
				}
				catch (IOException e) {
					Logger.getInstance(ToolCollection.class).error(e);
					result = false;
				}
			}

			return result;
		}

		return true;
	}


	public static Properties findDefaultPropertiesForTool(String name) {
		Properties properties = new Properties();

		if (defaultProperties != null) {
			for(Enumeration keys = defaultProperties.keys(); keys.hasMoreElements(); ) {
				String key = keys.nextElement().toString();
				if (key.startsWith(name)) {
					String keyName = key.substring(name.length() + 1);
					String value   = defaultProperties.getProperty(key);
					properties.put(keyName, value);
				}
			}
		}

		return properties;
	}


	private static ToolController createToolForName(@NotNull ToolController[] knownControllers, @NotNull String toolName) {
		for(ToolController tc : knownControllers) {
			if (tc.getName().equals(toolName)) {
				return tc;
			}
		}

		Properties properties = findDefaultPropertiesForTool(toolName);

		return new SimpleTool(
				toolName,
				properties
		);
	}


	/**
	 * Request the list of all available tools applicable with clang-tidy.
	 * The list will be fetched asynchronously by executing clang-tidy -list-checks.
	 * @param consumer		A consumer to receive the list of tools.
	 */
	public static void requestAvailableTools(@NotNull Consumer<List<ToolController>> consumer) {
		new Thread(() -> {
			ExtensionPointName<ToolController> tcExtensionPoint = new ExtensionPointName<>("de.wieselbau.clion.clangtidy.ToolController");
			ToolController[] extensions = Extensions.getExtensions(tcExtensionPoint);

			List<ToolController> tools = new ArrayList<>();

			if (cachedToolNames == null) {
				fetchToolsList();
			}

			if (defaultProperties == null) {
				fetchDefaultConfig();
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
