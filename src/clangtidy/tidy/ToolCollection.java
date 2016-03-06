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

import clangtidy.tidy.tools.ModernizeLoopConvert;
import clangtidy.tidy.tools.SimpleTool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper class to retrieve a list of checks available in clang-tidy.
 */
public class ToolCollection {
	private ToolCollection() {
	}


	/**
	 * Request the list of all available tools applicable with clang-tidy.
	 * The list will be fetched asynchronously by executing clang-tidy -list-checks.
	 * @param consumer		A consumer to receive the list of tools.
	 */
	public static void requestAvailableTools(@NotNull Consumer<List<ToolController>> consumer) {
		new Thread(() -> {
			// todo: should perform clang-tidy -list-checks

			List<ToolController> transforms = new ArrayList<>();
			transforms.add(new SimpleTool("modernize-make-unique"));
			transforms.add(new SimpleTool("modernize-pass-by-value"));
			transforms.add(new SimpleTool("modernize-redundant-void-arg"));
			transforms.add(new SimpleTool("modernize-replace-auto-ptr"));
			transforms.add(new SimpleTool("modernize-shrink-to-fit"));
			transforms.add(new SimpleTool("modernize-use-auto"));
			transforms.add(new SimpleTool("modernize-use-default"));
			transforms.add(new SimpleTool("modernize-use-nullptr"));
			transforms.add(new SimpleTool("modernize-use-override"));
			transforms.add(new ModernizeLoopConvert());

			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			consumer.accept(transforms);
		}).start();
	}
}
