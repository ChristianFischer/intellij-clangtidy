/*
 * Copyright (C) 2017
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

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import de.wieselbau.util.yaml.YamlReader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class to parse results from text input.
 */
public class ScannerResultUtil {
	//language=RegExp
	protected final static Pattern LINE_ISSUE_PATTERN
			= Pattern.compile("^(.*):(\\d+):(\\d+):\\s*(warning|error):(.*)\\s*\\[(.*)\\]$");

	ScannerResult	result;


	public ScannerResultUtil(ScannerResult result) {
		this.result = result;
	}


	public boolean parseIssue(String line) {
		Matcher m = LINE_ISSUE_PATTERN.matcher(line);
		if (m.matches()) {
			Issue issue = new Issue();

			String type = m.group(4);
			if ("error".equals(type)) {
				issue.type = ProblemHighlightType.ERROR;
			}
			else {
				issue.type = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
			}

			String path				= m.group(1);
			issue.sourceFile		= LocalFileSystem.getInstance().findFileByPath(path);
			issue.lineNumber		= Integer.parseInt(m.group(2));
			issue.lineColumn		= Integer.parseInt(m.group(3));
			issue.message			= m.group(5);
			issue.group				= m.group(6);

			if (result != null) {
				result.addIssue(issue);
			}

			return true;
		}

		return false;
	}


	public void readFixesList(@NotNull File yamlFile) {
		try {
			Object yaml = new YamlReader(yamlFile).getRootObject();
			if (yaml != null && yaml instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,Object> map = (Map<String,Object>)yaml;

				if (map.containsKey("Replacements")) {
					Object replacements = map.get("Replacements");

					if (replacements != null && replacements instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> list = (List<Object>)replacements;

						for(Object o : list) {
							if (o instanceof Map) {
								@SuppressWarnings("unchecked")
								Fix fix = parseFix((Map<String,Object>)o);

								if (fix != null) {
									result.addFix(fix);
								}
							}
						}
					}
				}
			}
		}
		catch (IOException e) {
			Logger.getInstance(this.getClass()).error(e);
		}
	}


	private Fix parseFix(Map<String,Object> map) {
		Object fileName		= map.get("FilePath");
		Object length		= map.get("Length");
		Object offset		= map.get("Offset");
		Object replacement	= map.get("ReplacementText");

		if (
				fileName	!= null
			&&	replacement	!= null
			&&	length		!= null	&& length instanceof Number
			&&	offset		!= null && offset instanceof Number
		) {
			int iOffset = ((Number)offset).intValue();
			int iLength = ((Number)length).intValue();

			return new Fix(
					new File(fileName.toString()),
					TextRange.create(iOffset, iOffset + iLength),
					replacement.toString()
			);
		}

		return null;
	}
}
