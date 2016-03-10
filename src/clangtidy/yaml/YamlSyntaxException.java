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

import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;

/**
 * Exception for parser errors in yaml files.
 */
public class YamlSyntaxException extends IOException {
	private static String generateMessage(File file, StreamTokenizer tokenizer) {
		String message =
				file != null
			?	"Unknown symbol at " + file.getPath() + ':' + tokenizer.lineno() + ": "
			:	"Unknown symbol at line " + tokenizer.lineno() + ": "
		;

		switch(tokenizer.ttype) {
			case StreamTokenizer.TT_WORD: {
				message += tokenizer.sval;
				break;
			}

			case StreamTokenizer.TT_NUMBER: {
				message += tokenizer.nval;
				break;
			}

			default: {
				if (tokenizer.ttype > 0) {
					message += "'" + ((char)tokenizer.ttype) + "'";
					message += " (" + tokenizer.ttype + ")";
				}
				else {
					message += tokenizer.ttype;
				}

				break;
			}
		}

		return message;
	}


	public YamlSyntaxException(File file, StreamTokenizer tokenizer) {
		super(generateMessage(file, tokenizer));
	}
}
