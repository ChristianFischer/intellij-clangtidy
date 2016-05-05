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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;
import java.util.function.Function;


/**
 * Implementation of a {@link javax.swing.tree.TreeModel} to display a list of files.
 */
public class FilesTreeModel extends DefaultTreeModel {
	private final static String	ROOT_ID		= "root";
	private final FilesTreeNode	ROOT;

	public FilesTreeModel() {
		super(new FilesTreeNode(ROOT_ID));
		ROOT = (FilesTreeNode)getRoot();
	}


	/**
	 * Creates and adds a new module node on this model's root.
	 * A module node should be used to group file entries within this models tree.
	 * @param icon	The module's icon
	 * @param name	The module's name
	 * @return The module node which was created.
	 */
	public @NotNull ModuleNode createModule(@NotNull Icon icon, @NotNull String name) {
		ModuleNode node = new ModuleNode(icon, name);
		ROOT.add(node);
		return node;
	}


	/**
	 * Adds a list of files to a given module node.
	 * @param files		A list of files to be added.
	 * @param parent	A module node, where the created file nodes will be added.
	 */
	public void addFiles(@NotNull List<VirtualFile> files, @NotNull ModuleNode parent) {
		addFiles(files, FileNode::new, parent);
	}


	/**
	 * Adds a list of used defined types to a given module node.
	 * @param files			A list of used defined types to be added.
	 * @param nodeCreator	A factory function to create a {@link FileNode} based on the given UserType object.
	 * @param parent		A module node, where the created file nodes will be added.
	 * @param <UserType>	Template argument for any used defined object which represents a single file.
	 */
	public <UserType>
	void addFiles(
			@NotNull List<UserType> files,
			@NotNull Function<UserType,FileNode> nodeCreator,
			@NotNull ModuleNode parent
	) {
		for(UserType file : files) {
			insert(file, nodeCreator, parent);
		}
	}


	/**
	 * Creates and adds a single {@link FileNode} to a given module node.
	 * @param userType		An user type which represents a single file.
	 * @param nodeCreator	A factory function to create a {@link FileNode} based on the given UserType object.
	 * @param parent		A module node, where the created file nodes will be added.
	 * @param <UserType>	Template argument for any used defined object which represents a single file.
	 */
	public <UserType>
	void insert(
			@NotNull UserType userType,
			@NotNull Function<UserType,FileNode> nodeCreator,
			@NotNull ModuleNode parent
	) {
		FileNode node = nodeCreator.apply(userType);
		VirtualFile file = node.getFile();

		DirectoryNode directoryNode = parent.findOrCreateDirectoryNode(file.getParent());
		directoryNode.add(node);
	}


	/**
	 * Flattens the whole tree model.
	 * This will cause all path elements to be merged with their parents if possible.
	 */
	public void flatten() {
		ROOT.flatten();
	}
}
