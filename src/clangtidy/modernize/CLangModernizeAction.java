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

import clangtidy.tidy.CompileCommandsNotFoundException;
import clangtidy.tidy.Runner;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
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

				if (files != null && cMakeWorkspace != null) {
					Runner runner = new Runner(project);
					runner.setFixIssues(Runner.FixIssues.FixImmediately);

					for(VirtualFile file : files) {
						runner.addSourcePath(file);
					}

					runner.run();
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
