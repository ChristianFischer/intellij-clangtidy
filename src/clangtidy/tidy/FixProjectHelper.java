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
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * Helper class to apply fixes found by clang to the current project.
 */
public class FixProjectHelper {
	private static class PerFile {
		enum Scope {
			Selection,
			Project,
			External,
		}

		PerFile() {
			issues = new ArrayList<>();
			fixes  = new ArrayList<>();
			offset = 0;
		}

		Scope			scope;
		List<Issue>		issues;
		List<Fix>		fixes;
		int				offset;
	}


	private Project						project;
	private Map<VirtualFile,PerFile>	fixesPerFile;
	private List<VirtualFile>			failedFiles;
	private List<Fix>					failedFixes;
	private int							totalFilesToFix;



	public static FixProjectHelper create(@NotNull Project project, @NotNull SourceFileSelection sourceFiles, @NotNull ScannerResult scannerResult) {
		ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
		FixProjectHelper helper = new FixProjectHelper(project);

		for(Fix fix : scannerResult.getFixes()) {
			VirtualFile file = fix.getFile();
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

		for(Issue issue : scannerResult.getIssues()) {
			VirtualFile file = issue.getSourceFile();
			PerFile target;

			if (helper.fixesPerFile.containsKey(file)) {
				target = helper.fixesPerFile.get(file);
				target.issues.add(issue);
			}
		}

		for(Map.Entry<VirtualFile,PerFile> entry : helper.fixesPerFile.entrySet()) {
			VirtualFile file    = entry.getKey();
			PerFile     perFile = entry.getValue();

			if (sourceFiles.isInSelection(file)) {
				perFile.scope = PerFile.Scope.Selection;
			}
			else if (projectFileIndex.isInSource(file)) {
				perFile.scope = PerFile.Scope.Project;
			}
			else {
				perFile.scope = PerFile.Scope.External;
			}
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


	public VirtualFile getNextFileToApply() {
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


	public boolean applyForFile(@NotNull VirtualFile nextFile) {
		PerFile perFile = fixesPerFile.get(nextFile);
		fixesPerFile.remove(nextFile);

		boolean successful = apply(nextFile, perFile);

		if (!successful) {
			failedFiles.add(nextFile);
			failedFixes.addAll(perFile.fixes);
		}

		return successful;
	}


	private boolean apply(@NotNull VirtualFile file, @NotNull PerFile fixesPerFile) {
		prepareFile(file, fixesPerFile);

		if (fixesPerFile.scope == PerFile.Scope.External) {
			return false;
		}

		WriteCommandAction.runWriteCommandAction(
				project,
				"clang-tidy",
				null,
				() -> {
					Document document = FileDocumentManager.getInstance().getDocument(file);
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


	private void prepareFile(VirtualFile file, PerFile fixesPerFile) {
		// ensure, all fixes are sorted in ascending order
		Collections.sort(
				fixesPerFile.fixes,
				(Fix a, Fix b) -> a.getTextRange().getStartOffset() - b.getTextRange().getStartOffset()
		);

		// since Intellij uses only \n for linebreaks, but clang-tidy is using the file's native
		// linebreak style for it's offsets, we have to convert them into \n linebreak offsets
		try(InputStream in = file.getInputStream()) {
			List<Integer> ignorableLineFeeds = new ArrayList<>();
			List<Integer> lineOffsets        = new ArrayList<>();

			{
				int currentOffset = 0;
				int lastByte      = -1;
				int b;

				lineOffsets.add(0);

				while((b = in.read()) != -1) {
					++currentOffset;

					if (b == '\n') {
						lineOffsets.add(currentOffset);

						// after a combination of \r and \n, the \r has to be ignored
						if (lastByte == '\r') {
							ignorableLineFeeds.add(currentOffset);
						}
					}

					lastByte = b;
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

				// try to assign issues to each fix
				for(Issue issue : fixesPerFile.issues) {
					if (lineOffsets.size() >= issue.getLineNumber()) {
						int lineOffset = lineOffsets.get(issue.getLineNumber() - 1);
						int offset     = lineOffset + issue.getLineColumn() - 1;

						if (fix.getTextRange().getStartOffset() == offset) {
							fix.setIssue(issue);
						}
					}
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}
}
