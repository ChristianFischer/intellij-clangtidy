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

import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * An implementation of {@link FilesTreeNode} which represents a path element
 * of one or more subdirectories.
 */
public class DirectoryNode extends FilesTreeNode {
	private @NotNull VirtualFile directory;


	public DirectoryNode(@NotNull VirtualFile directory) {
		super(directory);
		assert directory.isDirectory();
		this.directory = directory;
	}


	@NotNull
	public VirtualFile getDirectory() {
		return directory;
	}


	@Nullable
	public DirectoryNode getParentDirectoryNode() {
		if (getParent() instanceof DirectoryNode) {
			return (DirectoryNode)getParent();
		}

		return null;
	}


	@NotNull
	public String getPathSegmentName() {
		DirectoryNode parent = getParentDirectoryNode();
		VirtualFile parentDirectory = parent!=null ? parent.getDirectory() : null;

		VirtualFile currentDirectory = getDirectory();
		String pathSegment = null;

		while(currentDirectory != null && !Objects.equals(currentDirectory, parentDirectory)) {
			if (pathSegment == null) {
				pathSegment = currentDirectory.getName();
			}
			else {
				pathSegment = currentDirectory.getName() + File.separator + pathSegment;
			}

			currentDirectory = currentDirectory.getParent();
		}

		return pathSegment;
	}


	@Override
	public String getDisplayName() {
		return getPathSegmentName();
	}


	@Override
	public int getSortPriority() {
		return -1;
	}


	@Override
	public void flatten() {
		DirectoryNode parentNode = getParentDirectoryNode();

		if (parentNode != null) {
			assert parentNode.getDirectory().equals(directory.getParent());

			if (parentNode.getChildCount() == 1) {
				assert parentNode.getChildAt(0) == this;

				FilesTreeNode parentParentNode = (FilesTreeNode)parentNode.getParent();
				int index = parentParentNode.getIndex(parentNode);

				parentParentNode.insert(this, index);
				parentParentNode.remove(parentNode);
			}
		}

		super.flatten();
	}


	@Override
	public void customizeRenderer(ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
		renderer.setIcon(VirtualFilePresentation.getIcon(directory));
		renderer.append(getPathSegmentName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
	}
}
