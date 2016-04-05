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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Describes a single property marked via {@link Property} annotation.
 */
public class ClassPropertyDescriptor<Type> implements PropertyDescriptor<Type> {
	private String			name;
	private String			description;

	private Class<Type>		type;
	private Field			field;
	private boolean			isPublic;
	private Method			getter;
	private Method			setter;


	/**
	 * Helper function to capitalize a given string.
	 */
	public static String capitalize(@NotNull String s) {
		if (s.length() >= 1) {
			s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}

		return s;
	}


	static ClassPropertyDescriptor create(@NotNull Field field) {
		Property property = field.getAnnotation(Property.class);
		if (property != null) {
			ClassPropertyDescriptor pdesc = new ClassPropertyDescriptor(field);

			Description description = field.getAnnotation(Description.class);
			if (description != null) {
				pdesc.description = description.value();
			}

			String capitalizedName = capitalize(pdesc.name);
			Class clazz = field.getDeclaringClass();

			// check if this field is public
			pdesc.isPublic = Modifier.isPublic(field.getModifiers());

			try {
				@SuppressWarnings("unchecked")
				Method setter = clazz.getMethod("set" + capitalizedName, field.getType());
				pdesc.setter = setter;
			}
			catch (NoSuchMethodException ignored) {
			}

			// booleans may have a getter called isXXX
			if (pdesc.getter == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
				try {
					@SuppressWarnings("unchecked")
					Method getter = clazz.getMethod("is" + capitalizedName);
					if (getter != null && getter.getReturnType() == field.getType()) {
						pdesc.getter = getter;
					}
				}
				catch (NoSuchMethodException ignored) {
				}
			}

			if (pdesc.getter == null) {
				try {
					@SuppressWarnings("unchecked")
					Method getter = clazz.getMethod("get" + capitalizedName);
					if (getter != null && getter.getReturnType() == field.getType()) {
						pdesc.getter = getter;
					}
				}
				catch (NoSuchMethodException ignored) {
				}
			}

			return pdesc;
		}

		return null;
	}


	private ClassPropertyDescriptor(@NotNull Field field) {
		@SuppressWarnings("unchecked")
		Class<Type> type	= (Class<Type>)field.getType();

		this.name		= field.getName();
		this.field		= field;
		this.type		= type;
	}


	/**
	 * Get the properties name.
	 */
	@Override
	public @NotNull String getName() {
		return name;
	}


	/**
	 * Get the properties description, if any.
	 */
	@Override
	public @Nullable String getDescription() {
		return description;
	}


	/**
	 * Get the properties type.
	 */
	@Override
	public @NotNull Class getType() {
		return type;
	}


	/**
	 * Get the properties field, which was annotated via {@link Property}.
	 */
	public @NotNull Field getField() {
		return field;
	}


	/**
	 * Checks whether this field has public access level or not.
	 */
	public boolean isPublic() {
		return isPublic;
	}


	/**
	 * Get the getter method, which will return the properties value.
	 */
	public @Nullable Method getGetter() {
		return getter;
	}


	/**
	 * Get the setter method, which will set the properties value.
	 */
	public @Nullable Method getSetter() {
		return setter;
	}


	/**
	 * Checks if the property is readable.
	 */
	@Override
	public boolean isReadable() {
		return isPublic || getter != null;
	}


	/**
	 * Checks if the property is writable.
	 */
	@Override
	public boolean isEditable() {
		return isPublic || setter != null;
	}
}
