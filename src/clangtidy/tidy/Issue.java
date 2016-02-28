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

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Objects;

/**
 * Represents a single issue found by clang-tidy.
 */
public class Issue {
	protected ProblemHighlightType type;

	protected VirtualFile sourceFile;
	protected int lineNumber;
	protected int lineColumn;

	protected String group;
	protected String message;


	public ProblemHighlightType getType() {
		return type;
	}

	public VirtualFile getSourceFile() {
		return sourceFile;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getLineColumn() {
		return lineColumn;
	}

	public String getGroup() {
		return group;
	}

	public String getMessage() {
		return message;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Issue) {
			Issue other = (Issue)obj;

			if (
					Objects.equals(this.type,       other.type)
				&&	Objects.equals(this.sourceFile, other.sourceFile)
				&&	Objects.equals(this.group,      other.group)
				&&	Objects.equals(this.message,    other.message)
				&&	this.lineNumber	== other.lineNumber
				&&	this.lineColumn == other.lineColumn
			) {
				return true;
			}
		}

		return false;
	}


	@Override
	public String toString() {
		return
				type.toString() + ':'
			+	getSourceFile().getPath() + ':' + getLineNumber() + ':' + getLineColumn()
			+	getMessage()
		;
	}
}
