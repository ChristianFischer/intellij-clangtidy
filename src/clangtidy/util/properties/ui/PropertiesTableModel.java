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

package clangtidy.util.properties.ui;

import clangtidy.util.properties.ClassDescriptor;
import clangtidy.util.properties.PropertyDescriptor;
import clangtidy.util.properties.PropertyInstance;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.InvocationTargetException;

/**
 * A subclass of {@link TableModel} which provides the properties
 * of a class, annotated with {@link clangtidy.util.properties.Property}.
 */
public class PropertiesTableModel<T> implements TableModel {
	private T						object;
	private PropertyInstance[]		properties;


	public PropertiesTableModel(T object) {
		@SuppressWarnings("unchecked")
		Class<T> clazz			= (Class<T>)object.getClass();

		this.object				= object;
		ClassDescriptor<T> desc	= ClassDescriptor.create(clazz);

		PropertyDescriptor[] pd	= desc.getProperties();
		properties				= new PropertyInstance[pd.length];

		for(int i=0; i<pd.length; i++) {
			properties[i] = PropertyInstance.create(object, pd[i]);
		}
	}


	public PropertiesTableModel(T object, PropertyInstance[] properties) {
		this.object		= object;
		this.properties	= properties;
	}


	public @NotNull PropertyInstance getProperty(int index) throws ArrayIndexOutOfBoundsException {
		return properties[index];
	}


	@Override
	public int getRowCount() {
		return properties.length;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch(columnIndex) {
			case 0: return "Name";
			case 1: return "Value";
		}

		return null;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch(columnIndex) {
			case 0: return String.class;
			case 1: return Object.class;
		}

		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			return getProperty(rowIndex).getDescriptor().isEditable();
		}

		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0: {
				return getProperty(rowIndex).getDescriptor().getName();
			}

			case 1: {
				try {
					return getProperty(rowIndex).get();
				}
				catch (InvocationTargetException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			try {
				PropertyInstance property = getProperty(rowIndex);
			//	if (String.class.equals(aValue.getClass())) {
				if (aValue.getClass() == String.class) {
					property.setAsString(String.valueOf(aValue));
				}
				else {
					property.set(aValue);
				}
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
	}
}
