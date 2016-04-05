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
 * A simple implementation of {@link PropertyDescriptor}.
 * {@code SimplePropertyDescriptor} can be created with a custom name and type.
 */
public class SimplePropertyDescriptor<T> implements PropertyDescriptor {
	private String		name;
	private Class<T>	type;


	public static <T> SimplePropertyDescriptor<T> create(String key, Class<T> type) {
		return new SimplePropertyDescriptor<>(key, type);
	}


	public SimplePropertyDescriptor(String key, Class<T> type) {
		this.name = key;
		this.type = type;
	}

	@NotNull
	@Override
	public String getName() {
		return name;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@NotNull
	@Override
	public Class getType() {
		return type;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
}
