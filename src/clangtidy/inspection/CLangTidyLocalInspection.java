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
package clangtidy.inspection;

import clangtidy.tidy.CompileCommandsNotFoundException;
import clangtidy.tidy.Issue;
import clangtidy.tidy.Scanner;
import clangtidy.tidy.ScannerResult;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.cidr.lang.OCLanguageKind;
import com.jetbrains.cidr.lang.psi.impl.OCFileImpl;
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

	@Nullable
	@Override
	public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
		ProblemDescriptor[] problems = null;

		if (isCppFile(file)) {
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
					VirtualFile issueFile = file.getVirtualFile().getFileSystem().findFileByPath(issue.getSourceFileName());

					if (issueFile != null) {
						Document document = FileDocumentManager.getInstance().getDocument(issueFile);

						if (document != null) {
							int lineNumber = issue.getLineNumber() - 1;
							int lineStart  = document.getLineStartOffset(lineNumber);
							int lineEnd    = document.getLineEndOffset(lineNumber);

							ProblemDescriptor problem = manager.createProblemDescriptor(
									file,
									TextRange.create(lineStart + issue.getLineColumn() - 1, lineEnd),
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
		catch (CompileCommandsNotFoundException | IOException e) {
			e.printStackTrace();
		}

		return problems;
	}

	@Nullable
	@Override
	protected URL getDescriptionUrl() {
		return super.getDescriptionUrl();
	}
}
