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
import java.util.Collections;
import java.util.Objects;

/**
 * Base class for any tree node within a {@link FilesTreeModel}.
 */
public class FilesTreeNode extends CheckedTreeNode {
	public FilesTreeNode(Object userObject) {
		super(userObject);
	}


	/**
	 * Get the name of this node, which will be displayed in the UI.
	 */
	public String getDisplayName() {
		return Objects.toString(getUserObject());
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
	 * Sort this node's children based on their display name and order priority.
	 * @see FilesTreeNode#getDisplayName()
	 * @see FilesTreeNode#getSortPriority()
	 */
	public void sortChildren() {
		if (children != null) {
			Collections.sort(
					children,
					(Object a, Object b) -> {
						if (a instanceof FilesTreeNode && b instanceof FilesTreeNode) {
							FilesTreeNode aNode = (FilesTreeNode)a;
							FilesTreeNode bNode = (FilesTreeNode)b;

							int aPriority = aNode.getSortPriority();
							int bPriority = bNode.getSortPriority();

							if (aPriority == bPriority) {
								String aName = aNode.getDisplayName();
								String bName = bNode.getDisplayName();

								return aName.compareToIgnoreCase(bName);
							}

							return aPriority - bPriority;
						}

						return 0;
					}
			);
		}
	}


	/**
	 * Sort all children under this node by their display name and order priority.
	 * @see FilesTreeNode#sortChildren()
	 */
	public void sortChildrenRecursive() {
		sortChildren();

		for(int i=getChildCount(); --i>=0;) {
			TreeNode node = getChildAt(i);

			if (node instanceof FilesTreeNode) {
				FilesTreeNode filesTreeNode = (FilesTreeNode)node;
				filesTreeNode.sortChildrenRecursive();
			}
		}
	}


	/**
	 * Get a priority value for changing the sort order of nodes.
	 * Lower values will be inserted before higher values.
	 */
	public int getSortPriority() {
		return 0;
	}


	/**
	 * Customizes the rendering of this node.
	 * Should be overridden by each subclass.
	 */
	public void customizeRenderer(final ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
	}
}
