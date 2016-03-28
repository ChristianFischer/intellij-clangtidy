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
package clangtidy.tidy;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A helper class to run the clang-tidy executable and parse it's output.
 */
public class ProcessWrapper {
	private List<String>		command				= new ArrayList<>();
	private Consumer<String>	outputConsumer;
	private Consumer<String>	errorConsumer;


	public ProcessWrapper(@NotNull String exe) {
		command.add(exe);
	}


	public ProcessWrapper(@NotNull String exe, String...args) {
		this(exe);
		Collections.addAll(command, args);
	}


	public ProcessWrapper(@NotNull String exe, @NotNull List<String> args) {
		this(exe);
		command.addAll(args);
	}


	public void addArgument(@NotNull String argument) {
		command.add(argument);
	}


	public String getCommand() {
		return String.join(" ", command);
	}


	public void setOutputConsumer(@Nullable Consumer<String> outputConsumer) {
		this.outputConsumer = outputConsumer;
	}


	public void setErrorConsumer(@Nullable Consumer<String> errorConsumer) {
		this.errorConsumer = errorConsumer;
	}


	public boolean run() throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command);
		boolean success = false;
		Process process = pb.start();

		Thread outputHandler = OutputReader.fetch(
				process.getInputStream(),
				outputConsumer
		);

		Thread errorHandler = OutputReader.fetch(
				process.getErrorStream(),
				errorConsumer
		);

		try {
			int returnCode = process.waitFor();

			if (returnCode == 0) {
				success = true;
			}
		}
		catch(InterruptedException e) {
			Logger.getInstance(this.getClass()).error(e);
		}

		try {
			errorHandler.join();
		}
		catch(InterruptedException e) {
			Logger.getInstance(this.getClass()).error(e);
		}

		try {
			outputHandler.join();
		}
		catch(InterruptedException e) {
			Logger.getInstance(this.getClass()).error(e);
		}

		return success;
	}


	private static class OutputReader implements Runnable {
		private InputStream in;
		private Consumer<String> handler;

		static @NotNull Thread fetch(@NotNull InputStream in, @Nullable Consumer<String> handler) {
			Thread thread = new Thread(new OutputReader(in, handler));
			thread.start();
			return thread;
		}

		public OutputReader(@NotNull InputStream in, @Nullable Consumer<String> handler) {
			this.in = in;
			this.handler = handler;
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;

				while((line = reader.readLine()) != null) {
					if (handler != null) {
						handler.accept(line);
					}
				}
			}
			catch(IOException e) {
				Logger.getInstance(this.getClass()).error(e);
			}
		}
	}
}
