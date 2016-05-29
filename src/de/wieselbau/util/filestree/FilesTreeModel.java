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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Implementation of a {@link javax.swing.tree.TreeModel} to display a list of files.
 */
public class FilesTreeModel extends DefaultTreeModel {
	private final static String	ROOT_ID		= "root";
	private final FilesTreeNode	ROOT;


	/**
	 * Utility function to get the string value of a node.
	 * @param node	An object. If this object is an instance of {@link FilesTreeNode} it returns its string value.
	 * @return		if the object was a valid node, returns its string value, otherwise {@code null}.
	 */
	public static String NodeToString(@NotNull Object node) {
		if (node instanceof FilesTreeNode) {
			return NodeToString((FilesTreeNode)node);
		}

		return null;
	}


	/**
	 * Utility function to get the string value of an node.
	 * @param node	A node which string value should be returned.
	 * @return		The string value of the given node.
	 */
	public static String NodeToString(@NotNull FilesTreeNode node) {
		return node.getDisplayName();
	}


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


	/**
	 * Find a {@link FileNode} by the file assigned to it.
	 * If there are more than one node matching the given file, the first found will be returned.
	 * @param file	A file to be searched in this tree model.
	 * @return		The first node matching the given file, or {@code null}, if no node found.
	 */
	public @Nullable FileNode findNodeForFile(@NotNull VirtualFile file) {
		return findNodeInSubtree(
				FileNode.class,
				ROOT,
				(FileNode node) -> Objects.equals(file, node.getFile())
		);
	}


	/**
	 * Find a {@link FilesTreeNode} by the user object assigned to it.
	 * If there are more than one node matching the given object, the first found will be returned.
	 * @param object	An user object to be searched in this tree model.
	 * @return			The first node matching the given object, or {@code null}, if no node found.
	 */
	public <T> FilesTreeNode findNodeForUserObject(@NotNull T object) {
		return findNodeInSubtree(
				FilesTreeNode.class,
				ROOT,
				(FilesTreeNode node) -> Objects.equals(object, node.getUserObject())
		);
	}


	/**
	 * Find a {@link FilesTreeNode} by a custom condition.
	 * If there are more than one node matching the given object, the first found will be returned.
	 * @param cls		A class object describing the class of the node to be found.
	 * @param predicate	A predicate which implements the condition for the node to be found.
	 * @return			The first node matching the given predicate and class type, or {@code null}, if no node found.
	 */
	public <T extends FilesTreeNode> T findNode(
			@NotNull Class<T> cls,
			@NotNull Predicate<T> predicate
	) {
		return findNodeInSubtree(cls, ROOT, predicate);
	}


	/**
	 * Find a {@link FilesTreeNode} by a custom condition.
	 * If there are more than one node matching the given object, the first found will be returned.
	 * @param cls		A class object describing the class of the node to be found.
	 * @param parent	A tree node where to start searching.
	 * @param predicate	A predicate which implements the condition for the node to be found.
	 * @return			The first node matching the given predicate and class type, or {@code null}, if no node found.
	 */
	public <T extends FilesTreeNode> T findNodeInSubtree(
			@NotNull Class<T> cls,
			@NotNull FilesTreeNode parent,
			@NotNull Predicate<T> predicate
	) {
		Enumeration enumeration = parent.children();

		while(enumeration.hasMoreElements()) {
			Object o = enumeration.nextElement();
			if (o instanceof FilesTreeNode) {
				FilesTreeNode node = (FilesTreeNode)o;

				if (cls.isInstance(node)) {
					@SuppressWarnings("unchecked")
					T typedNode = (T) node;

					if (predicate.test(typedNode)) {
						return typedNode;
					}
				}

				T result = findNodeInSubtree(cls, node, predicate);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}
}
