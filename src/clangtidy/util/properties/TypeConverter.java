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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An utility class to convert strings into various types.
 */
public class TypeConverter {
	/**
	 * Checks if a string matches one of the fields of the given enum class.
	 * @param enumClass	The expected enum class
	 * @param value		The string to be tested
	 * @return {@code true}, if the string matches one of the enum's fields.
	 */
	public static boolean isValidEnumValue(Class<? extends Enum> enumClass, String value) {
		for(Enum enumValue : enumClass.getEnumConstants()) {
			if (enumValue.toString().equals(value)) {
				return true;
			}
		}

		return false;
	}


	private static <T> Method findConverterMethod(Class<T> type) {
		try {
			return type.getMethod("valueOf", String.class);
		}
		catch (NoSuchMethodException ignored) {
		}

		return null;
	}


	/**
	 * Get an wrapper class for a given class.
	 * For all primitive types like byte, int, float, etc, which can be autoboxed,
	 * this will return the type of their wrapper class.
	 * @param c		Any class.
	 * @return		An boxed class for {@code c}, or {@code c} itself, if it cant be boxed.
	 */
	public static Class getWrapperClass(Class c) {
		if (c.isPrimitive()) {
			if (c == void.class) {
				return Void.class;
			}

			if (c == boolean.class) {
				return Boolean.class;
			}

			if (c == byte.class) {
				return Byte.class;
			}

			if (c == short.class) {
				return Short.class;
			}

			if (c == int.class) {
				return Integer.class;
			}

			if (c == long.class) {
				return Long.class;
			}

			if (c == float.class) {
				return Float.class;
			}

			if (c == double.class) {
				return Double.class;
			}

			if (c == char.class) {
				return Character.class;
			}

			// no wrapper class for primitive found
			assert false;
		}

		return c;
	}


	public static <T> T convertTo(Class<T> type, String string) {
		if (type == String.class) {
			@SuppressWarnings("unchecked")
			T t = (T)string;
			return t;
		}

		Class aliasType  = getWrapperClass(type);
		Class targetType = aliasType!=null ? aliasType : type;

		Method m = findConverterMethod(targetType);
		if (m == null) {
			throw new IllegalArgumentException();
		}

		Object value;
		try {
			value = m.invoke(null, string);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}

		if (!targetType.isInstance(value)) {
			throw new IllegalTypeException(targetType, value);
		}

		@SuppressWarnings("unchecked")
		T tValue = (T)value;

		return tValue;
	}
}
