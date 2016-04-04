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

import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link TreeModel} managing various elements with separators in their names.
 * These separators are used to extract groups from the element's names to group
 * all elements by their prefix.
 */
public class ListToolsTreeDataModel<T> implements TreeModel {
	private static char[] SEPARATORS = { '.', '-', '/' };

	private static int findFirstSeparator(String string) {
		int result = -1;

		for(char separator : SEPARATORS) {
			int index = string.indexOf(separator);
			if (index != -1) {
				if (result == -1 || result > index) {
					result = index;
				}
			}
		}

		return result;
	}


	/**
	 * Represents a single entry within a {@link ListToolsTreeDataModel}.
	 */
	public static class Entry<T> extends CheckedTreeNode {
		private List<Entry<T>>		children		= new ArrayList<>();
		private Entry<T>			parent;
		private String				name;
		private String				displayName;
		private T					item;


		private Entry(@NotNull String name) {
			setName(name);
			parent = null;
		}

		public Entry(@NotNull Entry<T> parent, @NotNull String name) {
			setName(name);
			this.parent	= parent;
		}

		public Entry(@NotNull Entry<T> parent, @NotNull String name, @NotNull T item) {
			this(parent, name);
			setUserObject(item);
			this.item	= item;
		}


		public void setName(@NotNull String name) {
			this.name	= name;

			if (name.length() > 1 && findFirstSeparator(name) == (name.length() - 1)) {
				displayName = name.substring(0, name.length() - 1);
			}
			else {
				displayName = name;
			}
		}

		public @NotNull String getName() {
			return name;
		}

		public @NotNull String getDisplayName() {
			return displayName;
		}

		public @NotNull String getFullPath() {
			String path = name;

			if (parent != null) {
				path = parent.getFullPath() + path;
			}

			return path;
		}

		public T getItem() {
			return item;
		}


		private @NotNull Entry<T> add(T item, String name) {
			int separator = findFirstSeparator(name);
			if (separator == -1) {
				Entry<T> entry = new Entry<>(this, name, item);
				children.add(entry);
				return entry;
			}
			else {
				String nextPathElement = name.substring(0, separator + 1);
				String remainingPath   = name.substring(separator + 1);
				return findOrCreateEntry(nextPathElement).add(item, remainingPath);
			}
		}


		private void foldEntries(boolean force) {
			boolean foldedAtLeastOneItem = false;

			// force folding all children, if there's at least one leaf node
			// this helps reducing unwanted folders like 'use/auto', 'use/default'...
			if (this.hasLeafNodes() && !isRoot()) {
				force = true;
			}

			for(int i=0; i<children.size(); i++) {
				Entry<T> child = children.get(i);
				child.foldEntries(force);
				boolean fold = false;

				if (child.getChildCount() > 0 && child.item == null) {
					if (child.getChildCount() == 1) {
						fold = true;
					}

					if (force) {
						fold = true;
					}
				}

				if (fold) {
					children.remove(i);

					for(Entry<T> cchild : child.children) {
						cchild.setName(child.getName() + cchild.getName());
						children.add(i, cchild);
						++i;
					}

					foldedAtLeastOneItem = true;
					--i;
				}
			}

			if (foldedAtLeastOneItem) {
				this.foldEntries(force);
			}
		}


		private @NotNull Entry<T> findOrCreateEntry(@NotNull String name) {
			for(Entry<T> child : children) {
				if (name.equals(child.name)) {
					return child;
				}
			}

			Entry<T> entry = new Entry<T>(this, name);
			children.add(entry);

			return entry;
		}


		public int getChildCount() {
			return children.size();
		}

		@Override
		public Entry<T> getChildAt(int childIndex) {
			return children.get(childIndex);
		}

		@Override
		public Entry<T> getParent() {
			return parent;
		}

		@Override
		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}

		@Override
		public boolean isLeaf() {
			return children.size() == 0;
		}

		public boolean hasLeafNodes() {
			for(Entry<T> child : children) {
				if (child.isLeaf()) {
					return true;
				}
			}

			return false;
		}

		@Override
		public Enumeration children() {
			return Collections.enumeration(children);
		}

		@Override
		public String toString() {
			return getDisplayName();
		}
	}


	private Entry<T>			root		= new Entry<>("");
	private List<Entry<T>>		entries		= new ArrayList<>();



	private Entry<T> findEntry(Object object) {
		if (object instanceof Entry) {
			@SuppressWarnings("unchecked")
			Entry<T> entry = (Entry<T>)object;
			return entry;
		}

		return null;
	}


	/**
	 * Adds a new entry.
	 *
	 * @param item The item associated with the new entry.
	 * @param name The item's name including all it's group names.
	 * @return The {@link Entry} which was created.
	 */
	public @NotNull Entry<T> add(@NotNull T item, @NotNull String name) {
		Entry<T> entry = root.add(item, name);
		entries.add(entry);
		return entry;
	}


	/**
	 * Reduces all unnecessary groups.
	 * For example: groups with only one single child will be merged with their child.
	 */
	public void foldEntries() {
		root.foldEntries(false);
	}


	/**
	 * Get all entries in this model.
	 * This includes all leafs with items of this model, but no group nodes.
	 * @return An unmodifiable list of entries.
	 */
	public List<Entry<T>> getEntries() {
		return Collections.unmodifiableList(entries);
	}


	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public Object getChild(Object parent, int index) {
		Entry entry = findEntry(parent);
		if (entry != null) {
			return entry.getChildAt(index);
		}

		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		Entry entry = findEntry(parent);
		if (entry != null) {
			return entry.getChildCount();
		}

		return 0;
	}

	@Override
	public boolean isLeaf(Object node) {
		Entry entry = findEntry(node);
		if (entry != null) {
			return entry.getChildCount() == 0;
		}

		return true;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {

	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		Entry parentEntry = findEntry(parent);
		Entry childEntry  = findEntry(child);

		if (parentEntry != null && childEntry != null) {
			return parentEntry.getIndex(childEntry);
		}

		return 0;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {

	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {

	}
}
