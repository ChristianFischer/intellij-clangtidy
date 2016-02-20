/**
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
package clangtidy.tidy;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Helper class to apply fixes found by clang to the current project.
 */
public class FixProjectHelper {
	private static class PerFile {
		PerFile() {
			fixes = new ArrayList<>();
			offset = 0;
		}

		List<Fix>		fixes;
		int				offset;
	}


	private Project						project;
	private Map<File,PerFile>			fixesPerFile;
	private List<File>					failedFiles;
	private List<Fix>					failedFixes;
	private int							totalFilesToFix;



	public static FixProjectHelper create(Project project, List<Fix> fixes) {
		FixProjectHelper helper = new FixProjectHelper(project);

		for(Fix fix : fixes) {
			File file = fix.getFile();
			PerFile target;

			if (helper.fixesPerFile.containsKey(file)) {
				target = helper.fixesPerFile.get(file);
			}
			else {
				target = new PerFile();
				helper.fixesPerFile.put(file, target);
			}

			target.fixes.add(fix);
		}

		helper.totalFilesToFix	= helper.fixesPerFile.size();

		return helper;
	}


	private FixProjectHelper(Project project) {
		this.project		= project;
		this.fixesPerFile	= new HashMap<>();
		this.failedFiles	= new ArrayList<>();
		this.failedFixes	= new ArrayList<>();
	}


	public Project getProject() {
		return project;
	}

	public int getTotalFilesToFix() {
		return totalFilesToFix;
	}

	public int getRemainingFilesToFix() {
		return fixesPerFile.size();
	}

	public boolean hasFixesToApply() {
		return !fixesPerFile.isEmpty();
	}


	public File getNextFileToApply() {
		if (!fixesPerFile.isEmpty()) {
			return fixesPerFile.keySet().iterator().next();
		}

		return null;
	}

	public void applyAll() {
		while(hasFixesToApply()) {
			applyNext();
		}
	}


	public boolean applyNext() {
		if (!fixesPerFile.isEmpty()) {
			return applyForFile(getNextFileToApply());
		}

		return false;
	}


	public boolean applyForFile(@NotNull File nextFile) {
		PerFile perFile = fixesPerFile.get(nextFile);
		fixesPerFile.remove(nextFile);

		boolean successful = apply(nextFile, perFile);

		if (!successful) {
			failedFiles.add(nextFile);
			failedFixes.addAll(perFile.fixes);
		}

		return successful;
	}


	private boolean apply(@NotNull File file, @NotNull PerFile fixesPerFile) {
		VirtualFile vfile = LocalFileSystem.getInstance().findFileByIoFile(file);
		if (vfile == null) {
			return false;
		}

		WriteCommandAction.runWriteCommandAction(
				project,
				"clang-tidy",
				null,
				() -> {
					Document document = FileDocumentManager.getInstance().getDocument(vfile);
					if (document == null) {
						return;
					}

					for(Fix fix : fixesPerFile.fixes) {
						document.replaceString(
								fixesPerFile.offset + fix.getOffset(),
								fixesPerFile.offset + fix.getOffset() + fix.getLength(),
								fix.getReplacement()
						);

						fixesPerFile.offset -= fix.getLength();
						fixesPerFile.offset += fix.getReplacement().length();
					}
				}
		);

		return true;
	}
}
