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

import clangtidy.tidy.ApplyFixesBackgroundTask;
import clangtidy.tidy.CompileCommandsNotFoundException;
import clangtidy.tidy.Scanner;
import clangtidy.tidy.ScannerBackgroundTask;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;

/**
 * Implementation of the action, which starts the refactoring provided by clang-tidy.
 */
public class CLangModernizeAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = getEventProject(event);

		if (project != null) {
			try {
				CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
				VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());

				// save all open documents
				ApplicationManager.getApplication().saveAll();

				if (files != null && cMakeWorkspace != null) {
					Scanner scanner = new Scanner(project);
					scanner.setFixIssues(Scanner.FixIssues.StoreFixes);

					ScannerBackgroundTask task = new ScannerBackgroundTask(project, scanner);

					for(VirtualFile file : files) {
						task.addFile(file);
					}

					task.setOnSuccessCallback((Scanner sc) -> {
						if (sc.getFixes().isEmpty()) {
							Notification notification = new Notification(
									"groupDisplayId",
									"clang-tidy",
									"clang-tidy finished without finding any issues.",
									NotificationType.INFORMATION
							);

							notification.notify(project);
						}
						else {
							ApplyFixesBackgroundTask.start(project, scanner.getFixes());
						}
					});

					task.queue();
				}
			}
			catch (CompileCommandsNotFoundException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void update(AnActionEvent event) {
		boolean isAvailable = false;

		Project project = getEventProject(event);
		if (project != null) {
			CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
			if (cMakeWorkspace != null) {
				VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(event.getDataContext());
				if (files != null && files.length != 0) {
					isAvailable = true;
				}
			}
		}

		event.getPresentation().setEnabled(isAvailable);
	}
}
