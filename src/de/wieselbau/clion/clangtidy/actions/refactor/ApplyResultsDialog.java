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

package de.wieselbau.clion.clangtidy.actions.refactor;

import com.intellij.diff.merge.MergeResult;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.tidy.ApplyFixesBackgroundTask;
import de.wieselbau.clion.clangtidy.tidy.FixFileEntry;
import de.wieselbau.clion.clangtidy.tidy.FixProjectHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

/**
 * This dialog presents a list of all fixable files found by clang-tidy.
 * The user may select files which should be applied or apply single changes manually.
 */
public class ApplyResultsDialog extends DialogWrapper {
	private JPanel root;
	private JButton btMergeSelected;
	private JButton btApplySelected;
	private CheckBoxList<FixFileEntry> listMergeableFiles;

	private Project				project;
	private FixProjectHelper	helper;


	public ApplyResultsDialog(@NotNull Project project, @NotNull FixProjectHelper helper) {
		super(helper.getProject());

		this.project = project;
		this.helper  = helper;

		init();
		initContent();

		setTitle("Clang-Tidy Results");

		btMergeSelected.addActionListener(e -> onButtonMergeSelected());
		btApplySelected.addActionListener(e -> onButtonApplySelected());
		onListSelectionChanged(null);
	}


	private void createUIComponents() {
		listMergeableFiles = new CheckBoxList<>();
		listMergeableFiles.addListSelectionListener(this::onListSelectionChanged);
		listMergeableFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listMergeableFiles.setCheckBoxListListener((int index, boolean value) -> {
			FixFileEntry perFile = listMergeableFiles.getItemAt(index);
			if (perFile != null) {
				perFile.setSelected(value);
			}
		});

	}


	private void initContent() {
		for(FixFileEntry entry : helper.getFixes()) {
			StringBuilder description = new StringBuilder();
			description.append(entry.getFile().getPath());
			description.append(' ').append('(');
			description.append(entry.getFixes().size()).append(' ').append("Changes");
			description.append(')');

			listMergeableFiles.addItem(
					entry,
					description.toString(),
					entry.isSelected()
			);
		}
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return root;
	}


	@Override
	protected void doOKAction() {
		super.doOKAction();

		if (helper.countFilesToBeApplied() > 0) {
			ApplyFixesBackgroundTask.start(helper);
		}
	}


	protected void onButtonMergeSelected() {
		final int index = listMergeableFiles.getSelectedIndex();
		assert index > -1;

		if (index != -1) {
			final FixFileEntry entry = listMergeableFiles.getItemAt(index);
			assert entry != null;

			MergeFixesHelper.merge(helper, entry, result -> {
				if (result == MergeResult.RESOLVED) {
					entry.setResult(FixFileEntry.Result.Successful);
					removeListItem(index);
				}
			});
		}
	}


	protected void onButtonApplySelected() {
		int index = listMergeableFiles.getSelectedIndex();
		assert index > -1;

		if (index != -1) {
			FixFileEntry entry = listMergeableFiles.getItemAt(index);
			assert entry != null;

			FixFileEntry.Result result = entry.apply(project);

			switch (result) {
				case Successful: {
					removeListItem(index);
					break;
				}

				case Failed: {
					NotificationFactory.notifyScanFailedOnFile(helper.getProject(), entry.getFile());
					break;
				}

				case Skipped: {
					removeListItem(index);
					break;
				}
			}
		}
	}


	protected void onListSelectionChanged(ListSelectionEvent e) {
		boolean hasSelection = listMergeableFiles.getSelectedValue() != null;
		btMergeSelected.setEnabled(hasSelection);
		btApplySelected.setEnabled(hasSelection);
	}


	protected void removeListItem(int index) {
		((DefaultListModel)listMergeableFiles.getModel()).remove(index);
	}
}
