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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to create notifications for several events.
 */
public class NotificationFactory {
	public final static String GroupId	= "clang-tidy";


	private NotificationFactory() {}


	public static void notifyCompileCommandsNotFound(@NotNull Project project) {
		Notification notification = new Notification(
				GroupId,
				"Missing compile_commands.json",
				"clang-tidy did not found a compile_commands.json file in your CMake project.<br/>"
				 + "Please add <code><b>CMAKE_ENABLE_COMPILE_COMMANDS=ON</b></code>"
				 + " to your CMake configuration and rebuild your project",
				NotificationType.ERROR
		);

		notification.notify(project);
	}


	public static void notifyNoFilesSelected(@NotNull Project project) {
		Notification notification = new Notification(
				GroupId,
				"No compilable files selected.",
				"Please select at least one C, C++ or ObjectiveC source file to process.",
				NotificationType.INFORMATION
		);

		notification.notify(project);
	}


	public static void notifyScanFailedOnFile(@NotNull Project project, @NotNull VirtualFile file) {
		Notification notification = new Notification(
				GroupId,
				"clang-tidy failed on file",
				file.getPath(),
				NotificationType.ERROR
		);

		notification.notify(project);
	}


	public static void notifyResultNoFixesFound(@NotNull Project project) {
		Notification notification = new Notification(
				GroupId,
				"clang-tidy",
				"clang-tidy finished without finding any issues.",
				NotificationType.INFORMATION
		);

		notification.notify(project);
	}


	public static void notifyFailedToApplyFixesOnFile(@NotNull Project project, @NotNull VirtualFile file) {
		Messages.showErrorDialog(
				project,
				"Failed to apply fixes on file '" + file.getPath() + "'",
				"Failed to apply fixes on file"
		);
	}
}
