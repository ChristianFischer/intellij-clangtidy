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
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


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



	public static FixProjectHelper create(@NotNull Project project, @NotNull ScannerResult scannerResult) {
		FixProjectHelper helper = new FixProjectHelper(project);

		for(Fix fix : scannerResult.getFixes()) {
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

		prepareFile(file, fixesPerFile);

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
								fixesPerFile.offset + fix.getTextRange().getStartOffset(),
								fixesPerFile.offset + fix.getTextRange().getEndOffset(),
								fix.getReplacement()
						);

						// offsets will have changed after applying other changes
						fixesPerFile.offset -= fix.getTextRange().getLength();
						fixesPerFile.offset += fix.getReplacement().length();
					}
				}
		);

		return true;
	}


	private void prepareFile(File file, PerFile fixesPerFile) {
		// ensure, all fixes are sorted in ascending order
		Collections.sort(
				fixesPerFile.fixes,
				(Fix a, Fix b) -> a.getTextRange().getStartOffset() - b.getTextRange().getStartOffset()
		);

		// since Intellij uses only \n for linebreaks, but clang-tidy is using the file's native
		// linebreak style for it's offsets, we have to convert them into \n linebreak offsets
		try(InputStream in = new FileInputStream(file)) {
			List<Integer> ignorableLineFeeds = new ArrayList<>();

			{
				int currentOffset = 0;
				int b;

				while((b = in.read()) != -1) {
					++currentOffset;

					if (b == '\r') {
						b = in.read();
						++currentOffset;

						if (b == -1) {
							break;
						}

						// after a combination of \r and \n, the \r has to be ignored
						if (b == '\n') {
							ignorableLineFeeds.add(currentOffset);
						}
					}
				}
			}

			for(Fix fix : fixesPerFile.fixes) {
				final int startOffset = fix.getTextRange().getStartOffset();
				final int endOffset   = fix.getTextRange().getEndOffset();

				long startOffsetCorrection = ignorableLineFeeds.stream().filter((Integer i) -> (i <= startOffset)).count();
				long endOffsetCorrection   = ignorableLineFeeds.stream().filter((Integer i) -> (i <= endOffset)).count();

				fix.setTextRange(TextRange.create(
						(int)(startOffset - startOffsetCorrection),
						(int)(endOffset   - endOffsetCorrection)
				));
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}
}
