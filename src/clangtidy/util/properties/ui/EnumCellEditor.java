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

import clangtidy.util.properties.PropertyInstance;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * A cell editor implementation for editing enum types.
 */
public class EnumCellEditor extends DefaultCellEditor {
	private ComboBox comboBox;

	public EnumCellEditor() {
		super(new ComboBox());

		comboBox = (ComboBox)this.editorComponent;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Class enumClass = null;
		TableModel model = table.getModel();

		if (model instanceof PropertiesTableModel && column == 1) {
			PropertiesTableModel propertiesTableModel = (PropertiesTableModel)model;
			PropertyInstance property = propertiesTableModel.getProperty(row);

			if (property.getDescriptor().getType().isEnum()) {
				enumClass = property.getDescriptor().getType();
			}
		}

		if (enumClass != null) {
			comboBox.removeAllItems();
			for(Object enumConstant : enumClass.getEnumConstants()) {
				comboBox.addItem(enumConstant);
			}

			return super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}

		return null;
	}
}
