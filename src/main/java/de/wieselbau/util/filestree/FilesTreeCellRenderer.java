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

package de.wieselbau.util.filestree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

/**
 * A cell renderer to display nodes within a {@link FilesTreeModel}.
 */
public class FilesTreeCellRenderer extends ColoredTreeCellRenderer {
	public FilesTreeCellRenderer() {
		super();
	}


	/**
	 * This static method provides a default implementation for cell renderers and may be used
	 * for any class implementing a cell renderer for a files tree, which cannot directly inherit this class.
	 */
	public static void defaultCustomizeFilesTreeCellRenderer(
			@NotNull JTree tree, @NotNull ColoredTreeCellRenderer renderer,
			Object value,
			boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus
	) {
		if (value instanceof FilesTreeNode) {
			FilesTreeNode node = (FilesTreeNode)value;
			node.customizeRenderer(renderer, selected, expanded, hasFocus);
		}
		else {
			renderer.append(Objects.toString(value), SimpleTextAttributes.DARK_TEXT);
		}
	}


	@Override
	public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		defaultCustomizeFilesTreeCellRenderer(tree, this, value, selected, expanded, leaf, row, hasFocus);
		SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, selected);
	}
}
