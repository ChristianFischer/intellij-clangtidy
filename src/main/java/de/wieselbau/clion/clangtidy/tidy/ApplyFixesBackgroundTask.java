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

package de.wieselbau.clion.clangtidy.tidy;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Background task to apply any fixes found by clang-tidy to the current project.
 */
public class ApplyFixesBackgroundTask extends Task.Modal {
	public final static String TITLE	= "clang-tidy: applying fixes";

	/**
	 * Provides a notification when a specific entry was applied
	 */
	public interface OnAppliedCallback {
		void onApplied(@NotNull FixFileEntry entry, @Nullable FixFileEntry.Result result);
	}

	private Project				project;
	private List<FixFileEntry>	entriesToApply;
	private OnAppliedCallback	onAppliedCallback;



	public static void start(@NotNull Project project, @NotNull List<FixFileEntry> entriesToApply) {
		ProgressManager.getInstance().run(new ApplyFixesBackgroundTask(project, entriesToApply, null));
	}


	public static void start(@NotNull Project project, @NotNull List<FixFileEntry> entriesToApply, @Nullable OnAppliedCallback callback) {
		ProgressManager.getInstance().run(new ApplyFixesBackgroundTask(project, entriesToApply, callback));
	}


	public ApplyFixesBackgroundTask(@NotNull Project project, @NotNull List<FixFileEntry> entriesToApply, @Nullable OnAppliedCallback callback) {
		super(project, TITLE, false);
		this.project				= project;
		this.entriesToApply			= Collections.unmodifiableList(entriesToApply);
		this.onAppliedCallback		= callback;
	}


	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setFraction(0.0);
		indicator.setText("preparing");

		int filesTotal   = entriesToApply.size();
		int filesApplied = 0;

		for(FixFileEntry entry : entriesToApply) {
			indicator.setText(entry.getFile().getPath());
			FixFileEntry.Result result = entry.apply(project);
			++filesApplied;

			if (onAppliedCallback != null) {
				onAppliedCallback.onApplied(entry, result);
			}

			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			indicator.setFraction(1.0 * filesApplied / filesTotal);
		}

		indicator.setText("Done");

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
