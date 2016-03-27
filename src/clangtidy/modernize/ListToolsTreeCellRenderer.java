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

package clangtidy.modernize;

import clangtidy.tidy.ToolController;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;


/**
 * Cell renderer to display nodes representing a single {@link ToolController}
 */
public class ListToolsTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
	public ListToolsTreeCellRenderer() {
		super(true, true);
	}


	@Override
	public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof ListToolsTreeDataModel.Entry) {
			@SuppressWarnings("unchecked")
			ListToolsTreeDataModel.Entry<ToolController> entry = (ListToolsTreeDataModel.Entry<ToolController>)value;
			getTextRenderer().append(entry.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
		}
	}
}
