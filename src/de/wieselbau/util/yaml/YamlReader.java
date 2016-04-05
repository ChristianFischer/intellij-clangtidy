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

package de.wieselbau.util.yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for reading YAML files.
 *
 * If you found this ony by googling for yaml parsers, you should know, this
 * is a very simple one and optimized for yaml files created by clang-tidy.
 * It is very limited and not compatible with the full yaml specification.
 * You have been warned, have fun!
 */
public class YamlReader {
	private File				file			= null;
	private InputStreamReader	reader			= null;
	private StreamTokenizer		tokenizer		= null;
	private Object				rootObject		= null;


	private static String KEY_INTRO		= "---";
	private static String KEY_OUTRO		= "...";


	public YamlReader(File file) throws IOException {
		rootObject = read(file);
	}

	public YamlReader(InputStream in) throws IOException {
		rootObject = read(in);
	}


	public Object getRootObject() {
		return rootObject;
	}


	private Object read(File file) throws IOException {
		this.file = file;

		InputStream in = new FileInputStream(file);
		return read(in);
	}


	private Object read(InputStream in) throws IOException {
		Object data = null;

		reader = new InputStreamReader(in);

		tokenizer = new StreamTokenizer(reader);
		tokenizer.resetSyntax();
		tokenizer.eolIsSignificant(true);
		tokenizer.slashSlashComments(false);
		tokenizer.slashStarComments(false);
		tokenizer.parseNumbers();
		tokenizer.ordinaryChar('.'); // dot required for outro mark
		tokenizer.whitespaceChars('\0', ' ');
		tokenizer.commentChar('#');
		tokenizer.wordChars('a', 'z');
		tokenizer.wordChars('A', 'Z');
		tokenizer.wordChars('0', '9');
		tokenizer.wordChars('_', '_');
		tokenizer.wordChars('-', '-');
		tokenizer.wordChars('/', '/');
		tokenizer.wordChars('.', '.');
		tokenizer.wordChars(',', ',');
		tokenizer.wordChars(128 + 32, 255);

		try {
			readIntro();
			data = readData();
			readOutro();
		}
		finally {
			in.close();

			tokenizer = null;
			reader = null;
		}

		return data;
	}


	protected Object readData() throws IOException {
		Object data = null;

		int next = tokenizer.nextToken();
		tokenizer.pushBack();

		switch(next) {
			case '-': {
				data = readList();
				break;
			}

			case StreamTokenizer.TT_WORD: {
				if (KEY_OUTRO.equals(tokenizer.sval)) {
					tokenizer.pushBack();
					break;
				}
				else {
					data = readMap();
				}

				break;
			}

			case '.': {
				// first character of outro
				tokenizer.pushBack();
				break;
			}
		}

		return data;
	}


	private void readIntro() throws IOException {
		expect(KEY_INTRO);
		expect(StreamTokenizer.TT_EOL);
	}


	private void readOutro() throws IOException {
		expect(KEY_OUTRO);

		// ignore all linebreaks after outro mark
		while(true) {
			if (!(ignoreToken(StreamTokenizer.TT_EOL))) break;
		}

		expect(StreamTokenizer.TT_EOF);
	}


	private List<Object> readList() throws IOException {
		List<Object> list = new ArrayList<>();

		parserLoop: do {
			switch(tokenizer.nextToken()) {
				case '-': {
					Object element = readData();
					list.add(element);
					break;
				}

				case StreamTokenizer.TT_WORD: {
					if (tokenizer.sval.equals(KEY_OUTRO)) {
						tokenizer.pushBack();
						break parserLoop;
					}

					// fall through
				}

				default: {
					tokenizer.pushBack();
					break parserLoop;
				}
			}
		}
		while(true);

		return list;
	}


	private final static int StateNone		= 0;
	private final static int StateKey		= 1;
	private final static int StateValue		= 1;
	protected Map<String,Object> readMap() throws IOException {
		Map<String,Object> map = new HashMap<>();

		int currentState = StateNone;
		String currentKey = null;
		Object currentValue = null;

		parserLoop: do {
			switch(tokenizer.nextToken()) {
				case StreamTokenizer.TT_WORD: {
					switch(currentState) {
						case StateNone: {
							if (tokenizer.sval.equals(KEY_OUTRO)) {
								tokenizer.pushBack();
								break parserLoop;
							}

							currentKey = tokenizer.sval;
							currentState = StateKey;
							expect(':');

							break;
						}

						case StateKey: {
							currentState = StateValue;
							currentValue = tokenizer.sval;
							break;
						}

						default: {
							unexpectedState();
							break;
						}
					}

					break;
				}

				case '\'': {
					if (currentState == StateKey) {
						currentState = StateValue;
						currentValue = readQuotedString('\'', false);
					}
					else {
						unexpectedState();
					}

					break;
				}

				case '"': {
					if (currentState == StateKey) {
						currentState = StateValue;
						currentValue = readQuotedString('"', true);
					}
					else {
						unexpectedState();
					}

					break;
				}

				case StreamTokenizer.TT_NUMBER: {
					if (currentState == StateKey) {
						currentState = StateValue;
						currentValue = tokenizer.nval;
					}
					else {
						unexpectedState();
					}

					break;
				}

				case StreamTokenizer.TT_EOL: {
					if (currentKey != null) {
						if (currentValue == null) {
							currentValue = readData();
						}

						map.put(currentKey, currentValue);
						currentKey = null;
						currentValue = null;
						currentState = StateNone;
					}

					break;
				}

				case StreamTokenizer.TT_EOF: {
					break parserLoop;
				}

				case '.':
				case '-': {
					tokenizer.pushBack();
					break parserLoop;
				}

				default: {
					unexpectedState();
					break;
				}
			}
		}
		while(true);

		return map;
	}


	protected String readQuotedString(int quoteToken, boolean handleBackslash) throws IOException {
		StringBuilder sb = new StringBuilder();

		do {
			int token = reader.read();
			if (token == quoteToken && quoteToken != -1) {
				break;
			}

			switch(token) {
				case '\\': {
					if (handleBackslash) {
						int next = reader.read();
						switch(next) {
							case 'b':	sb.append('\b');	break;
							case 'f':	sb.append('\f');	break;
							case 'n':	sb.append('\n');	break;
							case 'r':	sb.append('\r');	break;
							case 't':	sb.append('\t');	break;
							case '"':	sb.append('"');		break;
							case '\'':	sb.append('\'');	break;
							case '\\':	sb.append('\\');	break;

							default: {
								unexpectedState();
								break;
							}
						}
					}
					else {
						sb.append('\\');
					}

					break;
				}

				default: {
					sb.append((char)token);
					break;
				}
			}
		}
		while(true);

		return sb.toString();
	}



	protected boolean ignoreToken(int expectedToken) throws IOException {
		int token = tokenizer.nextToken();
		if (token != expectedToken) {
			tokenizer.pushBack();
			return false;
		}

		return true;
	}


	protected void expect(String expectedString) throws IOException {
		if (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
			if (!tokenizer.sval.equals(expectedString)) {
				unexpectedState();
			}
		}
		else {
			tokenizer.pushBack();

			for(int i=0; i<expectedString.length(); i++) {
				char expectedChar = expectedString.charAt(i);
				expect(expectedChar);
			}
		}
	}


	private void expect(int expectedToken) throws IOException {
		int token = tokenizer.nextToken();
		if (token != expectedToken) {
			unexpectedState();
		}
	}


	private void unexpectedState() throws YamlSyntaxException {
		throw new YamlSyntaxException(file, tokenizer);
	}
}
