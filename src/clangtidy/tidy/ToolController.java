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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * This interface represents a single tool for clang-tidy.
 * An implementation
 */
public interface ToolController {
	/**
	 * @return The name of this tool within clang-tidy.
	 */
	@NotNull String getName();

	/**
	 * @return An alternate display name for this tool.
	 */
	default @NotNull String getDisplayName() { return getName(); }

	/**
	 * Get an UI component to allow the user to configure the settings
	 * of this tool. May be null, if this tool doesn't provide any options.
	 */
	default @Nullable JComponent getConfigPanel() { return null; }

	/**
	 * Create a string representing the current configuration of this tool.
	 * The string should be in JSON format. May be null, if no configuration is available.
	 */
	default @Nullable Map<String,String> getConfigParameters() { return null; }

	/**
	 * Notifies the tool, when the user has accepted the settings.
	 * The implementing class should store it's properties now.
	 */
	void OnConfigAccepted();
}
