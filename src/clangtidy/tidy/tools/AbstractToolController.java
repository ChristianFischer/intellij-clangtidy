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

package clangtidy.tidy.tools;

import clangtidy.Options;
import clangtidy.tidy.ToolController;
import clangtidy.util.properties.ClassPropertiesContainer;
import clangtidy.util.properties.PropertiesContainer;
import clangtidy.util.properties.PropertyInstance;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

/**
 * A base class for {@link ToolController} implementations, which provides
 * basic functionality like property binding via {@link PropertyChangeSupport}.
 */
public abstract class AbstractToolController implements ToolController {
	protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	protected ClassPropertiesContainer<AbstractToolController> propertiesContainer;


	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}


	protected void firePropertyChange(String name, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(name, oldValue, newValue);
	}


	@Override
	public @NotNull PropertiesContainer getProperties() {
		if (propertiesContainer == null) {
			propertiesContainer = ClassPropertiesContainer.create(this);
		}

		return propertiesContainer;
	}


	@Override
	public void OnConfigAccepted() {
		for(PropertyInstance property : propertiesContainer.getProperties()) {
			try {
				Options.setToolProperty(this, property.getDescriptor().getName(), property.getAsString());
			}
			catch (InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
