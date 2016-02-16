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

/**
 * Represents a single issue found by clang-tidy.
 */
public class Issue {
	protected ProblemHighlightType type;

	protected String sourceFileName;
	protected int lineNumber;
	protected int lineColumn;

	protected String group;
	protected String message;


	public ProblemHighlightType getType() {
		return type;
	}

	public String getSourceFileName() {
		return sourceFileName;
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


	public String toString() {
		return
				type.toString() + ':'
			+	getSourceFileName() + ':' + getLineNumber() + ':' + getLineColumn()
			+	getMessage()
		;
	}
}
