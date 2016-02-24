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
package clangtidy.util;

import clangtidy.tidy.CompileCommandsNotFoundException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to fix missing compile commands within the current cmake project.
 */
public class FixCompileCommandsNotFound {
	public static void fix(@NotNull Project project, @NotNull CompileCommandsNotFoundException e) {
		// todo: implement FixCompileCommandsNotFound
		// Currently we just notify the user.
		// In the future this helper should ask the user
		// and then add CMAKE_ENABLE_COMPILE_COMMANDS=ON automatically.
		NotificationFactory.notifyCompileCommandsNotFound(project);
	}
}
