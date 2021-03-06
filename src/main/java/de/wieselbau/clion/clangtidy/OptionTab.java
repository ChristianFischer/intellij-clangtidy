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

package de.wieselbau.clion.clangtidy;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;

/**
 * UI for the plugin's options dialog in the settings UI.
 */
public class OptionTab implements Configurable {

	private JTextField txtCLangTidyPath;
	private JButton btCLangTidySelect;
	private JPanel contentPane;
	private boolean modified = false;


	public OptionTab() {
		btCLangTidySelect.addActionListener(this::onBtCLangTidySelectClicked);
		txtCLangTidyPath.getDocument().addDocumentListener(onDocumentChangedListener);
	}


	@Nls
	@Override
	public String getDisplayName() {
		return "clang-tidy";
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		reset();

		return contentPane;
	}

	private void setModified() {
		modified = true;
	}

	@Override
	public boolean isModified() {
		return modified;
	}

	@Override
	public void apply() throws ConfigurationException {
		Options.setCLangTidyExe(txtCLangTidyPath.getText());
		modified = false;
	}

	@Override
	public void reset() {
		txtCLangTidyPath.setText(Options.getCLangTidyExe());
	}

	@Override
	public void disposeUIResources() {
		txtCLangTidyPath.getDocument().removeDocumentListener(onDocumentChangedListener);
	}



	void onBtCLangTidySelectClicked(ActionEvent e) {
		FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);

		if (SystemInfo.isWindows) {
			fileChooserDescriptor.withFileFilter((VirtualFile file) -> "exe".equalsIgnoreCase(file.getExtension()));
		}

		VirtualFile file = FileChooser.chooseFile(
				fileChooserDescriptor,
				OptionTab.this.contentPane,
				null,
				LocalFileSystem.getInstance().findFileByPath(txtCLangTidyPath.getText())
		);

		if (file != null) {
			txtCLangTidyPath.setText(file.getPath());
			setModified();
		}
	}


	private DocumentListener onDocumentChangedListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {
			setModified();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			setModified();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setModified();
		}
	};
}
