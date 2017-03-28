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

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores a single change result provided by clang-tidy.
 */
public class Fix {
	private String				diagnosticName;
	private Issue				issue;
	private List<Change>		changes;


	public static class Change {
		private File		file;
		private TextRange	range;
		private String		original;
		private String		replacement;


		public Change(@NotNull File file, @NotNull TextRange range, @NotNull String replacement) {
			this.file			= file;
			this.range			= range;
			this.replacement	= replacement;
		}


		public File getFile() {
			return file;
		}

		public void setTextRange(TextRange range) {
			this.range = range;
		}

		public TextRange getTextRange() {
			return range;
		}

		public void setOriginal(String original) {
			this.original = original;
		}

		public String getOriginal() {
			return original;
		}

		public String getReplacement() {
			return replacement;
		}


		public VirtualFile findVirtualFile() {
			return LocalFileSystem.getInstance().findFileByIoFile(file);
		}


		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Change) {
				Change other = (Change)obj;

				if (
						Objects.equals(this.file,        other.file)
					&&	Objects.equals(this.range,       other.range)
					&&	Objects.equals(this.replacement, other.replacement)
				) {
					return true;
				}
			}

			return false;
		}


		@Override
		public String toString() {
			return
					getFile().getPath() + '@' + getTextRange()
				+	" => '" + getReplacement() + "'"
			;
		}
	}


	public Fix(@NotNull File file, @NotNull TextRange range, @NotNull String replacement) {
		changes = Collections.singletonList(new Change(file, range, replacement));
	}


	public String getDiagnosticName() {
		return diagnosticName;
	}


	public List<Change> getChanges() {
		return Collections.unmodifiableList(changes);
	}


	/*
	public void setIssue(Issue issue) {
		if (!Objects.equals(issue.getSourceFile(), this.file)) {
			throw new IllegalArgumentException("File names do not match");
		}
		else {
			this.issue = issue;
		}
	}

	public Issue getIssue() {
		return issue;
	}
	*/



	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Fix) {
			Fix other = (Fix)obj;

			if (!Objects.equals(this.diagnosticName, other.diagnosticName)) {
				return false;
			}

			if (!Objects.equals(this.issue, other.issue)) {
				return false;
			}

			if (changes.size() != other.changes.size()) {
				return false;
			}

			for(int i=changes.size(); --i>=0;) {
				if (!Objects.equals(changes.get(i), other.changes.get(i))) {
					return false;
				}
			}

			return true;
		}

		return false;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (diagnosticName != null) {
			sb.append(diagnosticName);
			sb.append(" { ");

			boolean first = true;
			for(Change change : changes) {
				if (first) {
					first = false;
				}
				else {
					sb.append(", ");
				}

				sb.append(change.toString());
			}
		}
		else {
			assert changes.size() == 1;
			sb.append(changes.get(0).toString());
		}

		return sb.toString();
	}
}
