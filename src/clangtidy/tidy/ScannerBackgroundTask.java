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

import clangtidy.util.NotificationFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Background task to scan for fixes within a given file set.
 */
public class ScannerBackgroundTask extends Task.Modal {
	public final static String TITLE	= "clang-tidy";

	private Project			project;
	private Scanner			scanner;
	private ScannerResult	scannerResult;
	private boolean			cancelled;

	private SourceFileSelection	files;
	private BiConsumer<Scanner, ScannerResult> onSuccessCallback;


	public ScannerBackgroundTask(@NotNull Project project, @NotNull Scanner scanner) {
		super(project, TITLE, true);

		this.project		= project;
		this.scanner		= scanner;
		this.scannerResult	= new ScannerResult();
		this.cancelled		= false;
	}


	public void setSourceFiles(SourceFileSelection files) {
		this.files = files;
	}


	public void setOnSuccessCallback(BiConsumer<Scanner, ScannerResult> onSuccessCallback) {
		this.onSuccessCallback = onSuccessCallback;
	}


	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		indicator.setFraction(0.0);
		indicator.setText("starting...");

		List<VirtualFile> filesToProcess = files.getFilesToProcess();

		int filesTotal		= filesToProcess.size();
		int filesProcessed	= 0;

		for(VirtualFile file : filesToProcess) {
			indicator.setText(file.getPath());
			boolean successful;

			try {
				successful = scanner.runOnFiles(file, scannerResult);
			}
			catch (ScannerExecutionException e) {
				NotificationFactory.notifyScanFailedOnFile(project, e);
				successful = false;
			}
			catch(IOException e) {
				Logger.getInstance(this.getClass()).error(e);
				successful = false;
			}

			if (!successful) {
			//	NotificationFactory.notifyScanFailedOnFile(project, file);
				scannerResult.addFailedFile(file);
			}

			if (cancelled) {
				break;
			}

			++filesProcessed;
			indicator.setFraction(1.0 * filesProcessed / filesTotal);
		}
	}


	@Override
	public void onCancel() {
		cancelled = true;
		super.onCancel();
	}


	@Override
	public void onSuccess() {
		super.onSuccess();

		if (!cancelled) {
			onSuccessCallback.accept(scanner, scannerResult);
		}
	}
}
