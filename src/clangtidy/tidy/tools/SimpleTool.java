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
package clangtidy.tidy.tools;

import clangtidy.tidy.ToolController;
import clangtidy.util.properties.PropertiesContainer;
import clangtidy.util.properties.SimplePropertiesContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * An implementation of {@link ToolController} for
 * simple tools which doesn't provide any configuration.
 */
public class SimpleTool implements ToolController {
	private String				name;
	private PropertiesContainer	propertiesContainer;

	public SimpleTool(@NotNull String name) {
		this.name					= name;
		this.propertiesContainer	= new SimplePropertiesContainer(new Properties());
	}

	public SimpleTool(@NotNull String name, @NotNull PropertiesContainer container) {
		this.name					= name;
		this.propertiesContainer	= container;
	}

	@NotNull
	@Override
	public String getName() {
		return name;
	}

	@NotNull
	@Override
	public PropertiesContainer getProperties() {
		return propertiesContainer;
	}

	@Override
	public void onRestoreDefaults() {
	}

	@Override
	public void OnConfigAccepted() {
	}
}
