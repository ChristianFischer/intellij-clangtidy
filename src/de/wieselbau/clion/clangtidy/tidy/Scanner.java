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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeResolveConfiguration;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContextUtil;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.Options;
import de.wieselbau.util.properties.PropertiesContainer;
import de.wieselbau.util.properties.PropertyInstance;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to run the clang-tidy executable and parse it's output.
 */
public class Scanner {
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

	protected Project				project;
	protected CMakeWorkspace		cMakeWorkspace;
	protected File					fixesTargetFile;
	protected FixIssues				fixIssues = FixIssues.DontFix;
	protected List<ToolController>	tools;
	private boolean					ready = false;


	protected Scanner() {
		tools		= new ArrayList<>();
	}


	public Scanner(Project project) throws
			IOException
	{
		this();

		this.project = project;

		CMakeWorkspace cMakeWorkspace = CMakeWorkspace.getInstance(project);
		if (cMakeWorkspace != null) {
			initWithCMake(cMakeWorkspace);
		}
		else {
			throw new IllegalArgumentException("Invalid project type");
		}
	}


	protected void initWithCMake(CMakeWorkspace cMakeWorkspace) throws
			IOException
	{
		this.cMakeWorkspace = cMakeWorkspace;

		if (this.cMakeWorkspace == null) {
			throw new IllegalArgumentException("Parameter CMakeWorkspace is null");
		}

		// determine where to store fixes
		fixesTargetFile = File.createTempFile("clang-tidy-", ".yaml");
		Logger.getInstance(this.getClass()).info("Storing results in: " + fixesTargetFile);

		ready = true;
	}


	private File findCompileCommandsForFile(@NotNull VirtualFile file) throws CompileCommandsNotFoundException {
		File compileCommandsFile = ApplicationManager.getApplication().runReadAction((Computable<File>) () -> {
			OCResolveConfiguration configuration = OCInclusionContextUtil.getActiveConfiguration(file, project);

			File configDir = null;

			if (configuration instanceof CMakeResolveConfiguration) {
				configDir = ((CMakeResolveConfiguration) configuration).getConfiguration().getConfigurationGenerationDir();
			}

			if (configDir == null) {
				return null;
			}

			return new File(configDir.getAbsolutePath() + "/compile_commands.json");
		});

		if (!compileCommandsFile.exists()) {
			throw new CompileCommandsNotFoundException(cMakeWorkspace);
		}

		if (FixCompileCommandsUtil.needsToFixWindowsPaths(compileCommandsFile)) {
			FixCompileCommandsUtil.fixWindowsPaths(compileCommandsFile);
		}

		return compileCommandsFile;
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
					Logger.getInstance(this.getClass()).error(e);
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


	public boolean runOnFiles(@NotNull VirtualFile file, ScannerResult result) throws
			CompileCommandsNotFoundException,
			IOException
	{
		final ScannerResultUtil resultUtil = new ScannerResultUtil(result);

		if (!ready) {
			throw new IllegalStateException("CLangTidy runner not properly configured");
		}

		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		if (file.isDirectory()) {
			throw new IOException("File is a directory.");
		}

		// find compile commands
		File compileCommandsFile = findCompileCommandsForFile(file);

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
					Log.clangtidy.debug(line);

					resultUtil.parseIssue(line);

					if (line.startsWith("error: error reading")) {
						// failed to read an input file
						// this may be a hint to an issue on windows, where paths with backslash separators
						// within the compile_commands.json are not recognized
						readingFileFailed[0] = true;
					}
				}
		);

		final StringBuilder errorLog = new StringBuilder();
		process.setErrorConsumer(
				line -> {
					Log.clangtidy.warn(line);
					errorLog.append(line).append('\n');
				}
		);

		try {
			Log.clangtidy.info("Run command: " + process.getCommand());
			success = process.run();
		}
		catch(IOException e) {
			Logger.getInstance(this.getClass()).error(e);
		}

		if (
				fixIssues == FixIssues.StoreFixes
			&&	result != null
			&&	fixesTargetFile.exists()
		) {
			resultUtil.readFixesList(fixesTargetFile);
			fixesTargetFile.delete();
		}

		if (!success && errorLog.length() != 0) {
			throw new ScannerExecutionException(
					file,
					errorLog.toString()
			);
		}

		// the scan was successful, so on next fail, the notification will be shown again
		NotificationFactory.resetCompileCommandsNotFoundNotification();

		return success && !readingFileFailed[0];
	}
}
