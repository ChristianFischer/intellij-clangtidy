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

package clangtidy.util.properties;

import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link PropertiesContainer} which wraps a property
 * of an class object described via {@link Property} annotations.
 */
public class ClassPropertiesContainer<ClassType> implements PropertiesContainer {
	private ClassType 								object;
	private ClassDescriptor<? extends ClassType>	descriptor;
	private PropertyInstance[]						properties;


	public static <T> ClassPropertiesContainer<T> create(@NotNull T object) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>)object.getClass();
		ClassDescriptor<T> desc = ClassDescriptor.create(clazz);

		ClassPropertiesContainer<T> container = new ClassPropertiesContainer<>();
		container.object		= object;
		container.descriptor	= desc;
		container.properties	= desc.createPropertiesOf(object);

		return container;
	}


	private ClassPropertiesContainer() {
	}


	@Override
	public PropertyInstance[] getProperties() {
		return properties;
	}
}
