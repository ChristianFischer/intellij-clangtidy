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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Background task to apply any fixes found by clang-tidy to the current project.
 */
public class ApplyFixesBackgroundTask extends Task.Modal {
	public final static String TITLE	= "clang-tidy: applying fixes";

	private Project				project;
	private SourceFileSelection	sourceFileSelection;
	private ScannerResult		scannerResult;
	private FixProjectHelper	helper;



	public static void start(@NotNull Project project, @NotNull SourceFileSelection selection, @NotNull ScannerResult scannerResult) {
		ProgressManager.getInstance().run(new ApplyFixesBackgroundTask(project, selection, scannerResult));
	}


	public static void start(@NotNull FixProjectHelper helper) {
		ProgressManager.getInstance().run(new ApplyFixesBackgroundTask(helper));
	}


	public ApplyFixesBackgroundTask(@NotNull Project project, @NotNull SourceFileSelection selection, @NotNull ScannerResult scannerResult) {
		super(project, TITLE, false);
		this.project				= project;
		this.sourceFileSelection	= selection;
		this.scannerResult			= scannerResult;
	}


	public ApplyFixesBackgroundTask(@NotNull FixProjectHelper helper) {
		super(helper.getProject(), TITLE, false);
		this.project		= helper.getProject();
		this.helper			= helper;
	}


	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setFraction(0.0);
		indicator.setText("preparing");

		if (helper == null) {
			helper = FixProjectHelper.create(project, sourceFileSelection, scannerResult);
		}

		while(helper.hasFixesToApply()) {
			VirtualFile nextFile = helper.getNextFileToApply();
			indicator.setText(nextFile.getPath());

			helper.applyForFile(nextFile);

			int filesTotal   = helper.getTotalFilesToFix();
			int filesApplied = helper.getTotalFilesToFix() - helper.getRemainingFilesToFix();
			indicator.setFraction(1.0 * filesApplied / filesTotal);
		}

		indicator.setText("Done");
	}
}
