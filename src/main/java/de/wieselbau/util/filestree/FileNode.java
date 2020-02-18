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

/**
 * An implementation of {@link FilesTreeNode} to represent a single file.
 */
public class FileNode extends FilesTreeNode {
	private @NotNull VirtualFile file;

	public FileNode(@NotNull VirtualFile file) {
		super(file);
		this.file = file;
	}


	@NotNull
	public VirtualFile getFile() {
		return file;
	}


	@Override
	public String getDisplayName() {
		return file.getName();
	}


	@Override
	public void customizeRenderer(ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
		renderer.setIcon(VirtualFilePresentation.getIcon(file));
		renderer.append(file.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
	}
}
