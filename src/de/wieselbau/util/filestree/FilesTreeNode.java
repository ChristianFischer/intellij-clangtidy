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

import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColoredTreeCellRenderer;

import javax.swing.tree.TreeNode;

/**
 * Base class for any tree node within a {@link FilesTreeModel}.
 */
public class FilesTreeNode extends CheckedTreeNode {
	public FilesTreeNode(Object userObject) {
		super(userObject);
	}


	/**
	 * Flattens this node and all of it's children.
	 * @see FilesTreeModel#flatten()
	 */
	public void flatten() {
		for(int i=getChildCount(); --i>=0;) {
			TreeNode node = getChildAt(i);

			if (node instanceof FilesTreeNode) {
				FilesTreeNode filesTreeNode = (FilesTreeNode)node;
				filesTreeNode.flatten();
			}
		}
	}


	/**
	 * Customizes the rendering of this node.
	 * Should be overridden by each subclass.
	 */
	public void customizeRenderer(final ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
	}
}
