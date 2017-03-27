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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Helper class to apply fixes found by clang to the current project.
 */
public class FixProjectHelper {
	private Logger logger = Logger.getInstance(this.getClass());

	private Project						project;
	private List<FixFileEntry>			fixes;



	public static FixProjectHelper create(@NotNull Project project, @NotNull SourceFileSelection sourceFiles, @NotNull ScannerResult scannerResult) {
		ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
		Map<VirtualFile,FixFileEntry> fixesPerFile = new HashMap<>();

		for(Fix fix : scannerResult.getFixes()) {
			VirtualFile file = fix.findVirtualFile();
			FixFileEntry target;

			assert file != null;

			if (fixesPerFile.containsKey(file)) {
				target = fixesPerFile.get(file);
			}
			else {
				target = new FixFileEntry(file);
				fixesPerFile.put(file, target);
			}

			target.addFix(fix);
		}

		for(Issue issue : scannerResult.getIssues()) {
			VirtualFile file = issue.getSourceFile();
			FixFileEntry target;

			if (fixesPerFile.containsKey(file)) {
				target = fixesPerFile.get(file);
				target.addIssue(issue);
			}
		}

		for(FixFileEntry entry : fixesPerFile.values()) {
			if (sourceFiles.isInSelection(entry.getFile())) {
				entry.setScope(FixFileEntry.Scope.Selection);
				entry.setSelected(true);
			}
			else if (projectFileIndex.isInSource(entry.getFile())) {
				entry.setScope(FixFileEntry.Scope.Project);
				entry.setSelected(true);
			}
			else {
				entry.setScope(FixFileEntry.Scope.External);
				entry.setSelected(false);
			}
		}

		return new FixProjectHelper(project, fixesPerFile.values());
	}


	public FixProjectHelper(@NotNull Project project, @NotNull Collection<FixFileEntry> fixes) {
		this.project		= project;
		this.fixes			= new ArrayList<>(fixes.size());

		for(FixFileEntry entry : fixes) {
			this.fixes.add(entry);
		}

		Collections.sort(
				this.fixes,
				(FixFileEntry a, FixFileEntry b) -> a.getFile().toString().compareTo(b.getFile().toString())
		);
	}


	public Project getProject() {
		return project;
	}


	public void remove(FixFileEntry entry) {
		fixes.remove(entry);
	}


	public List<FixFileEntry> getFixes() {
		return Collections.unmodifiableList(fixes);
	}


	public List<FixFileEntry> getFixesSelected() {
		return fixes.stream().filter(FixFileEntry::isSelected).collect(Collectors.toList());
	}


	public int countFilesToBeApplied() {
		int count = 0;

		for(FixFileEntry entry : fixes) {
			if (!entry.isSelected()) {
				continue;
			}

			if (entry.getResult() != null) {
				continue;
			}

			++count;
		}

		return count;
	}
}
