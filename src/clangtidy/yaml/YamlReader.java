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
package clangtidy.yaml;

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
	private StreamTokenizer		tokenizer		= null;
	private Object				rootObject		= null;


	private static String KEY_INTRO		= "---";
	private static String KEY_OUTRO		= "...";


	public YamlReader(File file) throws IOException {
		rootObject = read(file);
	}


	public Object getRootObject() {
		return rootObject;
	}


	private Object read(File file) throws IOException {
		Object data = null;

		this.file = file;

		InputStream in = new FileInputStream(file);
		InputStreamReader reader = new InputStreamReader(in);

		tokenizer = new StreamTokenizer(reader);
		tokenizer.eolIsSignificant(true);
		tokenizer.slashSlashComments(true);
		tokenizer.slashStarComments(true);
		tokenizer.ordinaryChar('.');
		tokenizer.commentChar('#');
		tokenizer.quoteChar('\'');
		tokenizer.wordChars('_', '_');

		try {
			expect(KEY_INTRO);
			expect(StreamTokenizer.TT_EOL);

			data = readData();
		}
		finally {
			in.close();

			tokenizer = null;
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
					expect(KEY_OUTRO);
					expect(StreamTokenizer.TT_EOF);
					break;
				}
				else {
					data = readMap();
				}

				break;
			}

			case '.': {
				expect('.');
				expect('.');
				expect('.');
				ignoreToken(StreamTokenizer.TT_EOL);
				expect(StreamTokenizer.TT_EOF);
				tokenizer.pushBack();
				break;
			}
		}

		return data;
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
						currentValue = tokenizer.sval;
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

						if (currentValue != null) {
							map.put(currentKey, currentValue);
							currentKey = null;
							currentValue = null;
							currentState = StateNone;
						}
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



	protected void ignoreToken(int expectedToken) throws IOException {
		int token = tokenizer.nextToken();
		if (token != expectedToken) {
			tokenizer.pushBack();
		}
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
