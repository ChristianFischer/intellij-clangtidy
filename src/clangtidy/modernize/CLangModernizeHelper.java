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
package clangtidy.modernize;

import clangtidy.tidy.*;
import clangtidy.util.FixCompileCommandsNotFound;
import clangtidy.util.NotificationFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class, which starts the refactoring provided by clang-tidy.
 */
public class CLangModernizeHelper {

	private Project					project;
	private CMakeWorkspace			cMakeWorkspace;
	private SourceFileSelection		sourceFiles;


	public CLangModernizeHelper(@NotNull Project project) {
		this.project = project;
		cMakeWorkspace = CMakeWorkspace.getInstance(project);

		if (cMakeWorkspace == null) {
			throw new IllegalArgumentException("Missing CMake Workspace");
		}
	}


	public boolean start(VirtualFile[] files) {
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

		Scanner scanner;
		try {
			scanner = new Scanner(project);
			scanner.setFixIssues(Scanner.FixIssues.StoreFixes);
		}
		catch(CompileCommandsNotFoundException e) {
			FixCompileCommandsNotFound.fix(project, e);
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


	private void onScannerFinished(Scanner scanner) {
		if (scanner.getFixes().isEmpty()) {
			NotificationFactory.notifyResultNoFixesFound(project);
		}
		else {
			ApplyFixesBackgroundTask.start(project, scanner.getFixes());
		}
	}
}
