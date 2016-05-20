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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An implementation of {@link FilesTreeNode} to represent a single module.
 * Modules are root elements within the {@link FilesTreeModel} to group a set of files and directories.
 * Usually files and directories should not be added directly on this node, but via
 * {@link FilesTreeModel#addFiles(List, ModuleNode)} or {@link FilesTreeModel#addFiles(List, Function, ModuleNode)}.
 */
public class ModuleNode extends FilesTreeNode {
	private @NotNull Icon		icon;
	private @NotNull String		name;


	public ModuleNode(@NotNull Icon icon, @NotNull String name) {
		super(name);
		this.icon = icon;
		this.name = name;
	}


	@Override
	public String getDisplayName() {
		return name;
	}


	/**
	 * Creates a new {@link DirectoryNode} for a given directory or returns an existing one.
	 * Multiple calls for the same directory should always return the same object.
	 */
	public @NotNull DirectoryNode findOrCreateDirectoryNode(@NotNull VirtualFile directory) {
		VirtualFile parent = directory.getParent();
		FilesTreeNode parentNode =
				parent!=null
			?	findOrCreateDirectoryNode(parent)
			:	this
		;

		for(int i=parentNode.getChildCount(); --i>=0;) {
			TreeNode treeNode = parentNode.getChildAt(i);

			if (treeNode instanceof DirectoryNode) {
				DirectoryNode directoryNode = (DirectoryNode)treeNode;

				if (Objects.equals(directoryNode.getUserObject(), directory)) {
					return directoryNode;
				}
			}
		}

		DirectoryNode directoryNode = new DirectoryNode(directory);
		parentNode.add(directoryNode);

		return directoryNode;
	}


	@Override
	public void customizeRenderer(ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
		renderer.setIcon(icon);
		renderer.append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
	}
}
