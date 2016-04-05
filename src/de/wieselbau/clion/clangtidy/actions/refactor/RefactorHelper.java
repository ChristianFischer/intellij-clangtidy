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

package de.wieselbau.clion.clangtidy.actions.refactor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.Options;
import de.wieselbau.clion.clangtidy.tidy.*;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class, which starts the refactoring provided by clang-tidy.
 */
public class RefactorHelper {

	private Project					project;
	private CMakeWorkspace			cMakeWorkspace;
	private SourceFileSelection sourceFiles;


	public RefactorHelper(@NotNull Project project) {
		this.project = project;
		cMakeWorkspace = CMakeWorkspace.getInstance(project);

		if (cMakeWorkspace == null) {
			throw new IllegalArgumentException("Missing CMake Workspace");
		}
	}


	public boolean start(VirtualFile[] files) {
		if (!Options.isCLangTidyReady()) {
			NotificationFactory.notifyCLangTidyNotConfigured(project);
			return false;
		}

		if (files == null || files.length == 0) {
			NotificationFactory.notifyNoFilesSelected(project);
			return false;
		}

		Scanner scanner;
		try {
			scanner = new Scanner(project);
			scanner.setFixIssues(Scanner.FixIssues.StoreFixes);
		}
		catch(CompileCommandsNotFoundException e) {
			FixCompileCommandsUtil.askToFixMissingCompileCommands(project, e);
			return false;
		}

		return configureAndStartScanner(scanner, files);
	}


	public boolean configureAndStartScanner(Scanner scanner, VirtualFile[] files) {
		RefactorConfigurationDialog dialog = new RefactorConfigurationDialog(project);
		boolean success = dialog.showAndGet();

		if (success) {
			for(ToolController tool : dialog.getSelectedTools()) {
				scanner.addTool(tool);
			}

			success = startScanner(scanner, files);
		}

		return success;
	}


	public boolean startScanner(Scanner scanner, VirtualFile[] files) {
		if (files == null || files.length == 0) {
			NotificationFactory.notifyNoFilesSelected(project);
			return false;
		}

		sourceFiles = new SourceFileSelection(cMakeWorkspace);
		for(VirtualFile file : files) {
			sourceFiles.addFile(file);
		}

		if (sourceFiles.getFilesToProcess().isEmpty()) {
			NotificationFactory.notifyNoFilesSelected(project);
			return false;
		}

		return startScanner(scanner);
	}


	private boolean startScanner(Scanner scanner) {
		// save all open documents
		ApplicationManager.getApplication().saveAll();

		ScannerBackgroundTask task = new ScannerBackgroundTask(project, scanner);
		task.setSourceFiles(sourceFiles);
		task.setOnSuccessCallback(this::onScannerFinished);
		task.queue();

		return true;
	}


	private void onScannerFinished(Scanner scanner, ScannerResult result) {
		if (result.hasFailedFiles()) {
			final String FilesFailedTitle = "Error Reading Files";

			if (result.getFailedFiles().size() >= sourceFiles.getFilesToProcess().size()) {
				Messages.showErrorDialog(
						project,
						"clang-tidy failed to execute on all input files.",
						FilesFailedTitle
				);
			}
			else {
				StringBuilder sb = new StringBuilder();
				sb.append("clang-tidy failed on ");
				sb.append(result.getFailedFiles().size());
				sb.append(" of ");
				sb.append(sourceFiles.getFilesToProcess().size());
				sb.append(" input files.\n");

				if (result.hasFixes()) {
					sb.append("Continue to apply ");
					sb.append(result.getFixes().size());
					sb.append(" fixes?");

					int rc = Messages.showYesNoDialog(
							project,
							sb.toString(),
							FilesFailedTitle,
							Messages.getErrorIcon()
					);

					if (rc == Messages.YES) {
						onPreviewFixes(result);
					}
				}
				else {
					sb.append("No fixes were found");

					Messages.showErrorDialog(
							project,
							sb.toString(),
							FilesFailedTitle
					);
				}
			}
		}
		else {
			if (result.hasFixes()) {
				onPreviewFixes(result);
			}
			else {
				NotificationFactory.notifyResultNoFixesFound(project);
			}
		}
	}


	private void onPreviewFixes(ScannerResult result) {
		final FixProjectHelper helper = FixProjectHelper.create(project, sourceFiles, result);

		ApplyResultsDialog dialog = new ApplyResultsDialog(project, helper);
		dialog.show();
	}


	private void onStartFixingIssues(ScannerResult result) {
		ApplyFixesBackgroundTask.start(project, sourceFiles, result);
	}


	private void onStartFixingIssues(@NotNull FixProjectHelper helper) {
		ApplyFixesBackgroundTask.start(helper);
	}


}
