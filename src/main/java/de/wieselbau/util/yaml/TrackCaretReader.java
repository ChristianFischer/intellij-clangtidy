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

package de.wieselbau.util.yaml;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;


/**
 * An utility class which wraps any Reader to track the
 * current caret position while reading.
 */
public class TrackCaretReader extends Reader {
	private Reader parent;

	private int		line		= 1;
	private int		column		= 0;

	private boolean	cr			= false;


	public TrackCaretReader(Reader parent) {
		if (parent == null) {
			throw new NullPointerException();
		}

		this.parent = parent;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	@Override
	public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
		int ret = parent.read(cbuf, off, len);

		for(int i=0; i<len; i++) {
			char c = cbuf[off + i];
			testCharacter(c);
		}

		return ret;
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}


	private void testCharacter(char c) {
		switch(c) {
			case '\r': {
				cr = true;
				column = 0;
				++line;

				break;
			}

			case '\n': {
				if (!cr) {
					column = 0;
					++line;
				}

				cr = false;

				break;
			}

			default: {
				cr = false;
				++column;

				break;
			}
		}
	}
}
