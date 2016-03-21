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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Presents a description of a class and provides
 * attributes and properties declared in that class.
 * @see PropertyDescriptor
 * @see Property
 */
public class ClassDescriptor<ClassType> {

	private Class<ClassType>		clazz;
	private String					description;
	private PropertyDescriptor[]	properties;


	/**
	 * Creates a {@link ClassDescriptor} based on a given class.
	 * @param clazz		The class which should be described.
	 * @param <T>		Type parameter of the described class.
	 * @return the description of the given class.
	 */
	public static <T> ClassDescriptor<T> create(@NotNull Class<T> clazz) {
		ClassDescriptor<T> cdesc = new ClassDescriptor<>();
		cdesc.clazz = clazz;

		Description desc = clazz.getAnnotation(Description.class);
		if (desc != null) {
			cdesc.description = desc.value();
		}

		List<PropertyDescriptor> propertiesList = new ArrayList<>();
		findDeclaredProperties(clazz, propertiesList);

		cdesc.properties = propertiesList.toArray(new PropertyDescriptor[propertiesList.size()]);

		return cdesc;
	}


	private static void findDeclaredProperties(@NotNull Class clazz, @NotNull List<PropertyDescriptor> targetList) {
		Class superclass = clazz.getSuperclass();

		if (superclass != null) {
			findDeclaredProperties(superclass, targetList);
		}

		for(Field field : clazz.getDeclaredFields()) {
			PropertyDescriptor desc = PropertyDescriptor.create(field);
			if (desc != null) {
				targetList.add(desc);
			}
		}
	}


	/**
	 * Get the class which is described by this descriptor.
	 */
	public @NotNull Class<ClassType> getClassObject() {
		return clazz;
	}


	/**
	 * Get the described class' name.
	 */
	public @NotNull String getName() {
		return getClassObject().getName();
	}


	/**
	 * Get the class' description, or {@code null}, when no description is available.
	 */
	public @NotNull String getDescription() {
		return description;
	}


	/**
	 * Get all properties available in the described class.
	 */
	public @NotNull PropertyDescriptor[] getProperties() {
		return properties;
	}


	/**
	 * Creates a list of {@link PropertyInstance}s for an object of this property class.
	 * @param object	An object of type {@link ClassType}.
	 * @return			A list of properties of the given object.
	 */
	public @NotNull <T extends ClassType> PropertyInstance[] createPropertiesOf(T object) {
		PropertyDescriptor[] propertyDescriptors = getProperties();
		PropertyInstance[]   propertyInstances   = new PropertyInstance[propertyDescriptors.length];

		for(int i=0; i<propertyDescriptors.length; ++i) {
			propertyInstances[i] = PropertyInstance.create(object, propertyDescriptors[i]);
			assert propertyInstances[i] != null;
		}

		return propertyInstances;
	}


	/**
	 * Find the property with the given name.
	 * The result object will get {@link Object} as it's template parameter.
	 * To get the correct type, use {@link #findProperty(String, Class)}.
	 * @param name	The name of the property to find.
	 * @return the requested property or {@code null} when the property was not found.
	 */
	public @Nullable PropertyDescriptor findProperty(@NotNull String name) {
		for(PropertyDescriptor pdesc : properties) {
			if (Objects.equals(pdesc.getName(), name)) {
				return pdesc;
			}
		}

		return null;
	}


	/**
	 * Find the property with the given name and type.
	 * If a property was found, but the type does not match, {@code null} will be returned.
	 * The result object will get the requested type as it's template parameter.
	 * @param name	The name of the property to find.
	 * @param type	The type of the property to find.
	 * @return the requested property or {@code null} when the property was not found.
	 */
	public @Nullable <PropertyType>
	PropertyDescriptor<PropertyType> findProperty(@NotNull String name, @NotNull Class<PropertyType> type) {
		for(PropertyDescriptor pdesc : properties) {
			if (Objects.equals(pdesc.getName(), name)) {
				assert pdesc.getType().equals(type);

				@SuppressWarnings("unchecked")
				PropertyDescriptor<PropertyType> casted = (PropertyDescriptor<PropertyType>)pdesc;

				return casted;
			}
		}

		return null;
	}
}
