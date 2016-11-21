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

package de.wieselbau.clion.clangtidy.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.cidr.lang.OCLanguageKind;
import com.jetbrains.cidr.lang.psi.impl.OCFileImpl;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.tidy.CompileCommandsNotFoundException;
import de.wieselbau.clion.clangtidy.tidy.Issue;
import de.wieselbau.clion.clangtidy.tidy.Scanner;
import de.wieselbau.clion.clangtidy.tidy.ScannerResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs clang-tidy on a given file and provides the issues found by the tool via inspection UI.
 */
public class CLangTidyLocalInspection extends LocalInspectionTool {
	protected static boolean isCppFile(@NotNull PsiFile file) {
		if (file instanceof OCFileImpl) {
			OCFileImpl ocfile = (OCFileImpl)file;
			OCLanguageKind kind = ocfile.getKind();

			return kind.isCpp() || kind.isObjC();
		}

		return false;
	}


	protected static boolean isSaved(@NotNull PsiFile file) {
		VirtualFile virtualFile = file.getVirtualFile();
		Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);

		if (document != null) {
			FileEditorManager fileEditorManager = FileEditorManager.getInstance(file.getProject());

			if (fileEditorManager.isFileOpen(virtualFile)) {
				for(FileEditor editor : fileEditorManager.getEditors(virtualFile)) {
					if (editor.isModified()) {
						return false;
					}
				}

				return true;
			}
			else {
				// no editor opened, so data should be saved.
				return true;
			}
		}

		return false;
	}


	@Nullable
	@Override
	public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
		ProblemDescriptor[] problems = null;

		if (
				isCppFile(file)
		//	&&	isSaved(file)
		) {
			problems = checkCppFile(file, manager, isOnTheFly);
		}

		return problems;
	}


	private ProblemDescriptor[] checkCppFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
		ProblemDescriptor[] problems = null;

		try {
			VirtualFile vfile = file.getVirtualFile();

			Scanner runner = new Scanner(file.getProject());
			ScannerResult result = new ScannerResult();
			boolean success = runner.runOnFiles(vfile, result);

			List<ProblemDescriptor> problemsList = new ArrayList<>();

			if (success && result.hasIssues()) {
				for(Issue issue : result.getIssues()) {
					VirtualFile issueFile = file.getVirtualFile().getFileSystem().findFileByPath(issue.getSourceFile().getPath());

					if (issueFile != null) {
						Document document = FileDocumentManager.getInstance().getDocument(issueFile);

						if (document != null) {
							int lineNumber = issue.getLineNumber() - 1;
							int lineColumn = issue.getLineColumn() - 1;

							if (lineNumber >= document.getLineCount()) {
								continue;
							}

							int lineStart  = document.getLineStartOffset(lineNumber);
							int lineEnd    = document.getLineEndOffset(lineNumber);

							TextRange range = TextRange.create(
									Math.max(lineStart, lineStart + lineColumn),
									lineEnd
							);

							ProblemDescriptor problem = manager.createProblemDescriptor(
									file,
									range,
									issue.getMessage(),
									issue.getType(),
									isOnTheFly
							);

							problemsList.add(problem);
						}
					}
				}

				if (!problemsList.isEmpty()) {
					problems = problemsList.toArray(new ProblemDescriptor[problemsList.size()]);
				}
			}
		}
		catch (CompileCommandsNotFoundException e) {
			NotificationFactory.notifyCompileCommandsNotFound(file.getProject(), e.getCMakeWorkspace());
		}
		catch (IOException e) {
			Logger.getInstance(this.getClass()).error(e);
		}

		return problems;
	}

	@Nullable
	@Override
	protected URL getDescriptionUrl() {
		return super.getDescriptionUrl();
	}
}
