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

import clangtidy.Options;
import clangtidy.tidy.ToolController;
import clangtidy.tidy.tools.options.CaseType;
import clangtidy.tidy.tools.options.IncludeStyle;
import clangtidy.util.properties.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * An implementation of {@link ToolController} for
 * simple tools which doesn't provide any configuration.
 */
public class SimpleTool implements ToolController {
	private String				name;
	private PropertiesContainer	propertiesContainer;


	private static class SimpleToolPropertiesContainer implements PropertiesContainer {
		private PropertyInstance[] propertyInstances;


		private static Class guessPropertyType(@NotNull String name, String defaultValue) {
			if (
					(name.endsWith("NamingStyle") || name.endsWith("Case"))
				&&	(TypeConverter.isValidEnumValue(CaseType.class, defaultValue))
			) {
				return CaseType.class;
			}

			if (
					(name.endsWith("IncludeStyle"))
				&&	(TypeConverter.isValidEnumValue(IncludeStyle.class, defaultValue))
			) {
				return IncludeStyle.class;
			}

			return String.class;
		}


		public SimpleToolPropertiesContainer(Properties properties) {
			List<PropertyInstance> list = new ArrayList<>(properties.size());

			for(Map.Entry entry : properties.entrySet()) {
				final Object v     = entry.getValue();
				final String key   = entry.getKey().toString();
				final String value = v != null ? v.toString() : null;

				PropertyDescriptor pdesc = SimplePropertyDescriptor.create(
						key,
						guessPropertyType(key, value)
				);

				list.add(new PropertyInstance() {
					@Override
					public PropertyDescriptor getDescriptor() {
						return pdesc;
					}

					@Override
					public void set(Object value) throws InvocationTargetException, IllegalAccessException {
						properties.setProperty(key, value!=null ? value.toString() : null);
					}

					@Override
					public Object get() throws InvocationTargetException, IllegalAccessException {
						return properties.getProperty(key);
					}
				});
			}

			propertyInstances = list.toArray(new PropertyInstance[list.size()]);
		}

		@Override
		public PropertyInstance[] getProperties() {
			return propertyInstances;
		}
	}



	public SimpleTool(@NotNull String name) {
		this.name					= name;
		this.propertiesContainer	= new SimpleToolPropertiesContainer(new Properties());
	}

	public SimpleTool(@NotNull String name, @NotNull Properties properties) {
		this.name					= name;
		this.propertiesContainer	= new SimpleToolPropertiesContainer(properties);
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
		for(PropertyInstance property : getProperties().getProperties()) {
			try {
				String value = Options.getToolProperty(
						this,
						property.getDescriptor().getName(),
						property.getAsString() // = default value
				);

				property.setAsString(value);
			}
			catch (InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void OnConfigAccepted() {
		for(PropertyInstance property : getProperties().getProperties()) {
			try {
				Options.setToolProperty(
						this,
						property.getDescriptor().getName(),
						property.getAsString()
				);
			}
			catch (InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
