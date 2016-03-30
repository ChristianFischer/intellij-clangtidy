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

import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

/**
 * An exception which occurs, when the execution of the clang-tidy
 * scanner fails on a single file.
 * The exception carries the file where clang-tidy was executed on
 * and the error messages, which were printed by clang-tiry.
 */
public class ScannerExecutionException extends IOException {
	private VirtualFile		file;
	private String			log;

	public ScannerExecutionException(VirtualFile file, String log) {
		super("Failed to perform clang-tidy on '" + file.getPath() + "'");
		this.file	= file;
		this.log	= log;
	}


	public VirtualFile getFile() {
		return file;
	}

	public String getLog() {
		return log;
	}
}
