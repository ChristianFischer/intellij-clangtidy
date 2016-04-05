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

package de.wieselbau.util.properties;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * An implementation of {@link PropertiesContainer}, which uses properties
 * defined by a Java properties list using a {@link java.util.Properties} object.
 */
public class SimplePropertiesContainer implements PropertiesContainer {
	private Properties properties;
	private PropertyInstance[] propertyInstances;


	public SimplePropertiesContainer(@NotNull Properties properties) {
		this.properties = properties;
		createPropertyInstances();
	}


	private void createPropertyInstances() {
		List<PropertyInstance>  instances = new ArrayList<>(properties.size());
		this.propertyInstances = null;

		for(Map.Entry entry : properties.entrySet()) {
			final Object v     = entry.getValue();
			final String key   = entry.getKey().toString();
			final String value = v!=null ? v.toString() : null;

			instances.add(new PropertyInstance() {
				private PropertyDescriptor descriptor = SimplePropertyDescriptor.create(key, String.class);

				@Override
				public PropertyDescriptor getDescriptor() {
					return descriptor;
				}

				@Override
				public void set(Object value) throws InvocationTargetException, IllegalAccessException {
					setAsString(value.toString());
				}

				@Override
				public void setAsString(String stringVal) throws InvocationTargetException, IllegalAccessException {
					properties.setProperty(key, stringVal);
				}

				@Override
				public Object get() throws InvocationTargetException, IllegalAccessException {
					return getAsString();
				}

				@Override
				public String getAsString() throws InvocationTargetException, IllegalAccessException {
					Object o = properties.getProperty(key);
					return o!=null ? o.toString() : null;
				}
			});
		}

		this.propertyInstances = instances.toArray(new PropertyInstance[instances.size()]);
	}


	@Override
	public PropertyInstance[] getProperties() {
		return propertyInstances;
	}
}
