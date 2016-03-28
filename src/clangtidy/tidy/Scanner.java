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

import clangtidy.Options;
import clangtidy.util.FixCompileCommandsUtil;
import clangtidy.util.properties.PropertiesContainer;
import clangtidy.util.properties.PropertyInstance;
import clangtidy.yaml.YamlReader;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class to run the clang-tidy executable and parse it's output.
 */
public class Scanner {
	//language=RegExp
	protected final static Pattern LINE_ISSUE_PATTERN
			= Pattern.compile("^(.*):(\\d+):(\\d+):\\s*(warning|error):(.*)\\s*\\[(.*)\\]$");

	/**
	 * Determine if refactoring operations should be applied.
	 */
	public enum FixIssues {
		/** Don't fix any issues found. Code will not be changed. */
		DontFix,

		/** All issues will be fixed if possible when clang-tidy is running. */
		FixImmediately,

		/** Code will not be changed, but a list of fixes will be stored. */
		StoreFixes,
	}

	protected CMakeWorkspace		cMakeWorkspace;
	protected File					compileCommandsFile;
	protected File					fixesTargetFile;
	protected FixIssues				fixIssues = FixIssues.DontFix;
	protected List<ToolController>	tools;
	private boolean					ready = false;


	protected Scanner() {
		tools		= new ArrayList<>();
	}


	public Scanner(Project project) throws CompileCommandsNotFoundException {
		this();

		CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
		if (cMakeWorkspace != null) {
			initWithCMake(cMakeWorkspace);
		}
		else {
			throw new IllegalArgumentException("Invalid project type");
		}
	}


	protected void initWithCMake(CMakeWorkspace cMakeWorkspace) throws CompileCommandsNotFoundException {
		this.cMakeWorkspace = cMakeWorkspace;

		if (this.cMakeWorkspace == null) {
			throw new IllegalArgumentException("Parameter CMakeWorkspace is null");
		}

		// determine where to store fixes
		fixesTargetFile = new File(cMakeWorkspace.getProjectGeneratedDir() + "/clang-tidy.yaml");

		// todo: find out, which is the active configuration for the current project, or use __default__ directory
		File configDir = cMakeWorkspace.getConfigurationGeneratedDir("Debug");
		if (configDir == null) {
			throw new CompileCommandsNotFoundException(cMakeWorkspace);
		}

		compileCommandsFile = new File(configDir.getAbsolutePath() + "/compile_commands.json");

		if (!compileCommandsFile.exists()) {
			throw new CompileCommandsNotFoundException(cMakeWorkspace);
		}

		if (FixCompileCommandsUtil.needsToFixWindowsPaths(compileCommandsFile)) {
			FixCompileCommandsUtil.fixWindowsPaths(compileCommandsFile);
		}

		ready = true;
	}


	public void setFixIssues(FixIssues fixIssues) {
		this.fixIssues = fixIssues;
	}

	public FixIssues getFixIssues() {
		return fixIssues;
	}


	public void addTool(@NotNull ToolController tool) {
		tools.add(tool);
	}



	private void addToolsConfig(@NotNull ProcessWrapper process) {
		StringBuilder checksString = null;
		StringBuilder configString = null;

		for(ToolController tool : tools) {
			PropertiesContainer propertiesContainer = tool.getProperties();

			if (checksString == null) {
				checksString = new StringBuilder();
			}
			else {
				checksString.append(',');
			}

			checksString.append(tool.getName());

			for(PropertyInstance property : propertiesContainer.getProperties()) {
				if (configString == null) {
					configString = new StringBuilder();
				}
				else {
					configString.append(',').append(' ');
				}

				try {
					String propertyName  = property.getDescriptor().getName();
					String propertyValue = property.getAsString();

					configString.append('{');
					configString.append("key: ");
					configString.append('\'').append(tool.getName()).append('.').append(propertyName).append('\'');
					configString.append(',').append(' ');
					configString.append("value: ");
					configString.append('\'').append(propertyValue).append('\'');
					configString.append('}');
				}
				catch (InvocationTargetException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

		if (checksString != null) {
			process.addArgument("-checks=-*," + checksString.toString());
		}
		else {
			process.addArgument("-checks=*");
		}

		if (configString != null) {
			process.addArgument("-config={CheckOptions: [ " + configString.toString() + " ]}");
		}

	//	process.addArgument("-dump-config");
	}


	public boolean runOnFiles(@NotNull VirtualFile file, ScannerResult result) throws IOException {
		if (!ready) {
			throw new IllegalStateException("CLangTidy runner not properly configured");
		}

		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		if (file.isDirectory()) {
			throw new IOException("File is a directory.");
		}

		ProcessWrapper process = new ProcessWrapper(Options.getCLangTidyExe());
		process.addArgument("-p");
		process.addArgument(compileCommandsFile.getParentFile().getAbsolutePath());
		process.addArgument("-header-filter=.*");

		addToolsConfig(process);

		switch(fixIssues) {
			case DontFix: {
				break;
			}

			case FixImmediately: {
				process.addArgument("-fix");
				break;
			}

			case StoreFixes: {
				if (fixesTargetFile != null) {
					process.addArgument("-export-fixes=" + fixesTargetFile.getPath().replace('\\', '/'));
				}

				break;
			}
		}

		// add all source files
		process.addArgument(file.getPath());
		final boolean[] readingFileFailed = new boolean[]{ false };
		boolean success = false;

		process.setOutputConsumer(
				(String line) -> {
					System.out.println("Tidy: " + line);

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
					}
					else if (line.startsWith("error: error reading")) {
						// failed to read an input file
						// this may be a hint to an issue on windows, where paths with backslash separators
						// within the compile_commands.json are not recognized
						readingFileFailed[0] = true;
					}
				}
		);

		process.setErrorConsumer(
				(String line) -> System.err.println("CLangTidy: " + line)
		);

		try {
			success = process.run();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		if (
				fixIssues == FixIssues.StoreFixes
			&&	result != null
			&&	fixesTargetFile.exists()
		) {
			readFixesList(fixesTargetFile, result);
			fixesTargetFile.delete();
		}

		return success && !readingFileFailed[0];
	}


	protected void readFixesList(@NotNull File yamlFile, @NotNull ScannerResult result) {
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
			e.printStackTrace();
		}
	}


	private static Fix parseFix(Map<String,Object> map) {
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
					LocalFileSystem.getInstance().findFileByPath(fileName.toString()),
					TextRange.create(iOffset, iOffset + iLength),
					replacement.toString()
			);
		}

		return null;
	}
}
