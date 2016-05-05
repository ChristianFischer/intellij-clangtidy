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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.PlatformIcons;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.tidy.ApplyFixesBackgroundTask;
import de.wieselbau.clion.clangtidy.tidy.FixFileEntry;
import de.wieselbau.clion.clangtidy.tidy.FixProjectHelper;
import de.wieselbau.util.filestree.FileNode;
import de.wieselbau.util.filestree.FilesTreeCellRenderer;
import de.wieselbau.util.filestree.FilesTreeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This dialog presents a list of all fixable files found by clang-tidy.
 * The user may select files which should be applied or apply single changes manually.
 */
public class ApplyResultsDialog extends DialogWrapper {
	private JPanel root;
	private JButton btMergeSelected;
	private JButton btApplySelected;
	private Tree listMergeableFiles;

	private Project				project;
	private FixProjectHelper	helper;


	private static class FixFileEntryNode extends FileNode {
		private FixFileEntry entry;

		public FixFileEntryNode(@NotNull FixFileEntry entry) {
			super(entry.getFile());
			this.entry = entry;
		}

		public FixFileEntry getEntry() {
			return entry;
		}

		@Override
		public void setChecked(boolean checked) {
			entry.setSelected(checked);
		}

		@Override
		public boolean isChecked() {
			return entry.isSelected();
		}

		@Override
		public void customizeRenderer(ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
			super.customizeRenderer(renderer, selected, expanded, hasFocus);

			renderer.append(
					" (" + String.valueOf(getEntry().getFixes().size()) + " Changes)",
					SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
			);
		}
	}


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
		listMergeableFiles = new CheckboxTree();
		listMergeableFiles.addTreeSelectionListener(this::onListSelectionChanged);
		listMergeableFiles.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	}


	private void initContent() {
		FilesTreeModel model = new FilesTreeModel();

		// add project files
		{
			List<FixFileEntry> projectEntries = helper
					.getFixes().stream()
					.filter(entry -> entry.getScope() != FixFileEntry.Scope.External)
					.collect(Collectors.toList())
			;

			if (!projectEntries.isEmpty()) {
				model.addFiles(
						projectEntries,
						FixFileEntryNode::new,
						model.createModule(PlatformIcons.PROJECT_ICON, project.getName())
				);
			}
		}

		// add external files
		{
			List<FixFileEntry> externalEntries = helper
					.getFixes().stream()
					.filter(entry -> entry.getScope() == FixFileEntry.Scope.External)
					.collect(Collectors.toList())
			;

			if (!externalEntries.isEmpty()) {
				model.addFiles(
						externalEntries,
						FixFileEntryNode::new,
						model.createModule(PlatformIcons.LIBRARY_ICON, "External files")
				);
			}
		}

		model.flatten();

		listMergeableFiles.setModel(model);
		listMergeableFiles.setRootVisible(false);

		listMergeableFiles.setCellRenderer(new CheckboxTree.CheckboxTreeCellRenderer(true, true) {
				@Override
				public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					super.customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
					FilesTreeCellRenderer.defaultCustomizeFilesTreeCellRenderer(tree, getTextRenderer(), value, selected, expanded, leaf, row, hasFocus);
				}
		});

		// expand all nodes of the tree
		for(int i=0; i<listMergeableFiles.getRowCount(); i++) {
			listMergeableFiles.expandRow(i);
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
		final FixFileEntry entry = getCurrentlySelectedFixEntry();
		assert entry != null;

		if (entry != null) {
			MergeFixesHelper.merge(helper, entry, null);
		}
	}


	protected void onButtonApplySelected() {
		FixFileEntry entry = getCurrentlySelectedFixEntry();
		assert entry != null;

		if (entry != null) {
			FixFileEntry.Result result = entry.apply(project);

			switch (result) {
				case Failed: {
					NotificationFactory.notifyScanFailedOnFile(helper.getProject(), entry.getFile());
					break;
				}
			}
		}
	}


	protected void onListSelectionChanged(TreeSelectionEvent e) {
		boolean hasSelection = getCurrentlySelectedFixEntry() != null;
		btMergeSelected.setEnabled(hasSelection);
		btApplySelected.setEnabled(hasSelection);
	}


	private FixFileEntry getCurrentlySelectedFixEntry() {
		FixFileEntryNode[] nodes = listMergeableFiles.getSelectedNodes(FixFileEntryNode.class, null);
		if (nodes.length > 0) {
			assert nodes.length == 1;

			return nodes[0].getEntry();
		}

		return null;
	}
}
