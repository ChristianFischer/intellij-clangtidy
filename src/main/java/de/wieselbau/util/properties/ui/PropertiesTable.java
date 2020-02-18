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

package de.wieselbau.util.properties.ui;

import com.intellij.ui.table.JBTable;
import de.wieselbau.util.properties.PropertyInstance;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

/**
 * A subclass of {@link JBTable} which is suitable for editing properties
 * via {@link PropertiesTableModel}
 */
public class PropertiesTable extends JBTable {


	@Override
	@SuppressWarnings("unchecked")
	protected void createDefaultEditors() {
		super.createDefaultEditors();

		// enum editor
		defaultEditorsByColumnClass.put(Enum.class, (UIDefaults.LazyValue) t -> new EnumCellEditor());
	}


	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (column == 1) {
			TableModel model = getModel();

			if (model instanceof PropertiesTableModel) {
				PropertiesTableModel propertiesTableModel = (PropertiesTableModel) model;
				PropertyInstance property = propertiesTableModel.getProperty(row);

				Class type = property.getDescriptor().getType();

				// cells should be editable with a single click
				TableCellEditor editor = getDefaultEditor(type);
				if (editor != null && editor instanceof DefaultCellEditor) {
					((DefaultCellEditor)editor).setClickCountToStart(1);
				}

				return editor;
			}
		}

		return null;
	}
}
