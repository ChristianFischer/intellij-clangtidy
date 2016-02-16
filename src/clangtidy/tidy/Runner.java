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
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A helper class to run the clang-tidy executable and parse it's output.
 */
public class Runner {
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
	protected List<VirtualFile>		sourcePaths;
	protected FixIssues				fixIssues = FixIssues.DontFix;
	private boolean					ready = false;

	protected List<Issue>			issues;


	protected Runner() {
		sourcePaths = new ArrayList<>();
		issues = new ArrayList<>();
	}


	public Runner(Project project) throws CompileCommandsNotFoundException {
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

		// todo: find out, which is the active configuration for the current project
		File configDir = cMakeWorkspace.getConfigurationGeneratedDir("Debug");
		if (configDir == null) {
			throw new CompileCommandsNotFoundException(cMakeWorkspace);
		}

		compileCommandsFile = new File(configDir.getAbsolutePath() + "/compile_commands.json");

		if (!compileCommandsFile.exists()) {
			throw new CompileCommandsNotFoundException(cMakeWorkspace);
		}

		ready = true;
	}


	public void addSourcePath(VirtualFile path) {
		if (path.isDirectory()) {
			VfsUtilCore.visitChildrenRecursively(
					path,
					new VirtualFileVisitor() {
						@Override
						public boolean visitFile(@NotNull VirtualFile file) {
							addSourcePath(file);

							return super.visitFile(file);
						}
					}
			);
		}
		else {
			sourcePaths.add(path);
		}
	}


	public void setFixIssues(FixIssues fixIssues) {
		this.fixIssues = fixIssues;
	}

	public FixIssues getFixIssues() {
		return fixIssues;
	}


	public List<Issue> getIssues() {
		return issues;
	}


	public void clearResults() {
		issues.clear();
	}


	public boolean run() {
		if (!ready) {
			throw new IllegalStateException("CLangTidy runner not properly configured");
		}

		if (sourcePaths.isEmpty()) {
			throw new IllegalStateException("No source paths given");
		}

		List<String> args = new ArrayList<>();
		args.add(Options.getCLangTidyExe());
		args.add("-p");
		args.add(compileCommandsFile.getParentFile().getAbsolutePath());
		args.add("-checks=*");

		switch(fixIssues) {
			case DontFix: {
				break;
			}

			case FixImmediately: {
				args.add("-fix");
				break;
			}

			case StoreFixes: {
				if (fixesTargetFile != null) {
					args.add("-export-fixes=\"" + fixesTargetFile.getPath() + "\"");
				}

				break;
			}
		}

		// add all source files
		args.addAll(sourcePaths.stream().map(VirtualFile::getPath).collect(Collectors.toList()));

		// clear any recent results before beginning
		clearResults();

		System.out.println("Run command: " + String.join(" ", args));

		ProcessBuilder pb = new ProcessBuilder(args);
		Process process = null;

		try {
			process = pb.start();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		if (process == null) {
			return false;
		}

		Thread outputHandler = OutputReader.fetch(
				process.getInputStream(),
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

						issue.sourceFileName	= m.group(1);
						issue.lineNumber		= Integer.parseInt(m.group(2));
						issue.lineColumn		= Integer.parseInt(m.group(3));
						issue.message			= m.group(5);
						issue.group				= m.group(6);

						issues.add(issue);
					}
				}
		);

		Thread errorHandler = OutputReader.fetch(
				process.getErrorStream(),
				(String line) -> System.err.println("CLangTidy: " + line)
		);

		try {
			process.waitFor();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}

		try {
			errorHandler.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}

		try {
			outputHandler.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}

		return process.exitValue() == 0;
	}


	private static class OutputReader implements Runnable {
		private InputStream in;
		private Consumer<String> handler;

		static @NotNull Thread fetch(@NotNull InputStream in, @NotNull Consumer<String> handler) {
			Thread thread = new Thread(new OutputReader(in, handler));
			thread.start();
			return thread;
		}

		public OutputReader(@NotNull InputStream in, @NotNull Consumer<String> handler) {
			this.in = in;
			this.handler = handler;
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;

				while((line = reader.readLine()) != null) {
					handler.accept(line);
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
