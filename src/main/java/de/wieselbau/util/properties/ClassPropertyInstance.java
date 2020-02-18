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

/**
 * Instance of a property, bound to an object.
 * This class can be used to get or set a properties value.
 */
public class ClassPropertyInstance<ClassType,PropertyType> extends PropertyInstance<PropertyType> {
	private ClassType				object;
	private ClassPropertyDescriptor property;


	/**
	 * Creates a {@link ClassPropertyInstance} which binds a property to a corresponding object.
	 * @param object	An object which owns the given property.
	 * @param property	A property which should be accessed.
	 * @param <C>		The objects type.
	 * @param <T>		The property to bind.
	 * @return An {@link ClassPropertyInstance} object.
	 */
	public static <C,T> ClassPropertyInstance<C,T> create(@NotNull C object, @NotNull ClassPropertyDescriptor<T> property) {
		return new ClassPropertyInstance<>(object, property);
	}


	/**
	 * Creates a {@link ClassPropertyInstance} which binds a property to a corresponding object.
	 * @param object	An object which owns the given property.
	 * @param property	A property which should be accessed.
	 */
	public ClassPropertyInstance(@NotNull ClassType object, @NotNull ClassPropertyDescriptor<PropertyType> property) {
		if (!property.getField().getDeclaringClass().isInstance(object)) {
			throw new IllegalArgumentException("'" + object.getClass() + "' is not an owner of property '" + property + "'");
		}

		this.object		= object;
		this.property	= property;
	}


	/**
	 * Get the object, this property instance is bound to.
	 */
	public ClassType getObject() {
		return object;
	}


	@Override
	public PropertyDescriptor getDescriptor() {
		return property;
	}


	@Override
	public void set(PropertyType value) throws InvocationTargetException, IllegalAccessException {
		if (!property.getType().isInstance(value)) {
			throw new IllegalTypeException(property.getType(), value);
		}

		if (property.getSetter() != null) {
			property.getSetter().invoke(object, value);
			return;
		}

		if (property.isPublic()) {
			property.getField().set(object, value);
			return;
		}

		throw new IllegalAccessException();
	}


	@Override
	public PropertyType get() throws InvocationTargetException, IllegalAccessException {
		if (property.getGetter() != null) {
			@SuppressWarnings("unchecked")
			PropertyType value = (PropertyType)property.getGetter().invoke(object);
			return value;
		}

		if (property.isPublic()) {
			@SuppressWarnings("unchecked")
			PropertyType value = (PropertyType)property.getField().get(object);
			return value;
		}

		throw new IllegalAccessException();
	}


}
