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
import org.jetbrains.annotations.Nullable;

/**
 * Interface which describes a property.
 * A property is defined by it's name and type.
 * The value of a property can be read or written with
 * a suitable {@link PropertyInstance} object.
 */
public interface PropertyDescriptor<Type> {
	/**
	 * Get the properties name.
	 */
	@NotNull
	String getName();

	/**
	 * Get the properties description, if any.
	 */
	@Nullable
	String getDescription();

	/**
	 * Get the properties type.
	 */
	@NotNull
	Class getType();

	/**
	 * Checks if the property is readable.
	 */
	boolean isReadable();

	/**
	 * Checks if the property is writable.
	 */
	boolean isEditable();
}
