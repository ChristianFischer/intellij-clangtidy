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

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Stores a single change result provided by clang-tidy.
 */
public class Fix {
	private Issue		issue;
	private VirtualFile file;
	private TextRange	range;
	private String		replacement;


	public Fix(@NotNull VirtualFile file, @NotNull TextRange range, @NotNull String replacement) {
		this.file			= file;
		this.range			= range;
		this.replacement	= replacement;
	}


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

	public VirtualFile getFile() {
		return file;
	}

	public void setTextRange(TextRange range) {
		this.range = range;
	}

	public TextRange getTextRange() {
		return range;
	}

	public String getReplacement() {
		return replacement;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Fix) {
			Fix other = (Fix)obj;

			if (
					Objects.equals(this.file,        other.file)
				&&	Objects.equals(this.issue,       other.issue)
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
