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

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects the result of the {@link Scanner}.
 */
public class ScannerResult {
	private List<VirtualFile>	filesFailed		= new ArrayList<>();
	private List<Issue>			issues			= new ArrayList<>();
	private List<Fix>			fixes			= new ArrayList<>();


	public ScannerResult() {
	}


	public void addFailedFile(VirtualFile file) {
		filesFailed.add(file);
	}

	public boolean hasFailedFiles() {
		return !filesFailed.isEmpty();
	}

	public @NotNull List<VirtualFile> getFailedFiles() {
		return Collections.unmodifiableList(filesFailed);
	}


	public void addIssue(Issue issue) {
		issues.add(issue);
	}

	public boolean hasIssues() {
		return !issues.isEmpty();
	}

	public @NotNull List<Issue> getIssues() {
		return Collections.unmodifiableList(issues);
	}


	public void addFix(Fix fix) {
		fixes.add(fix);
	}

	public boolean hasFixes() {
		return !fixes.isEmpty();
	}

	public @NotNull List<Fix> getFixes() {
		return Collections.unmodifiableList(fixes);
	}
}
