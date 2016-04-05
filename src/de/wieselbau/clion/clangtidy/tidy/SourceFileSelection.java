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

package de.wieselbau.clion.clangtidy.tidy;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.jetbrains.cidr.cpp.cmake.model.CMakeModel;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.lang.OCLanguageKind;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * SourceFileSelection stores the files selected by the user to be processed.
 */
public class SourceFileSelection {
	private CMakeWorkspace				cMakeWorkspace;
	private Set<String>					validFileExtension;

	private List<VirtualFile>			selectedFiles;
	private List<VirtualFile>			selectedCompilableFiles;
	private List<VirtualFile>			selectedDirectories;


	public SourceFileSelection(@NotNull CMakeWorkspace cMakeWorkspace) {
		this.cMakeWorkspace = cMakeWorkspace;

		CMakeModel model = cMakeWorkspace.getModel();
		if (model == null) {
			throw new NullPointerException("CMakeModel not valid");
		}

		this.validFileExtension	= new HashSet<>();
		for(Map.Entry<String,OCLanguageKind> entry : model.getFileExtensions().entrySet()) {
			validFileExtension.add(entry.getKey().toLowerCase());
		}

		selectedFiles				= new ArrayList<>();
		selectedCompilableFiles		= new ArrayList<>();
		selectedDirectories			= new ArrayList<>();
	}


	public void addFile(VirtualFile path) {
		if (path.isDirectory()) {
			selectedDirectories.add(path);

			VfsUtilCore.visitChildrenRecursively(
					path,
					new VirtualFileVisitor() {
						@Override
						public boolean visitFile(@NotNull VirtualFile file) {
							if (!file.isDirectory()) {
								addFile(file);
							}

							return super.visitFile(file);
						}
					}
			);
		}
		else {
			if (isFileCompileable(path)) {
				selectedCompilableFiles.add(path);
			}

			selectedFiles.add(path);
		}
	}


	public boolean isFileCompileable(@NotNull VirtualFile file) {
		String extension = file.getExtension();
		if (extension != null) {
			return validFileExtension.contains(extension.toLowerCase());
		}

		return false;
	}


	public boolean isInSelection(@NotNull VirtualFile file) {
		return selectedFiles.contains(file);
	}


	public List<VirtualFile> getFilesToProcess() {
		return Collections.unmodifiableList(selectedCompilableFiles);
	}
}
