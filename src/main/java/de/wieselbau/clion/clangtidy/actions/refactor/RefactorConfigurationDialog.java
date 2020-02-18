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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.tree.TreeUtil;
import de.wieselbau.clion.clangtidy.Options;
import de.wieselbau.clion.clangtidy.tidy.ToolCollection;
import de.wieselbau.clion.clangtidy.tidy.ToolController;
import de.wieselbau.util.properties.PropertyInstance;
import de.wieselbau.util.properties.ui.PropertiesTable;
import de.wieselbau.util.properties.ui.PropertiesTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI class to provide configuration for clang-tidy based refactoring.
 */
public class RefactorConfigurationDialog extends DialogWrapper {
	private JPanel root;
	private CheckboxTree listTools;
	private JBTable tToolProperties;
	private JPanel toolsConfigContainer;
	private JTextPane txtCurrentDescription;
	private JEditorPane txtDocumentationLink;

	private ListToolsTreeDataModel<ToolController> listToolsModel;
	private TreeSpeedSearch listToolsSpeedSearch;

	private ToolController			currentTool;
	private PropertiesTableModel	currentToolProperties;

	private ToolController[]		selectedTools;

	private Project					project;


	public RefactorConfigurationDialog(@Nullable Project project) {
		super(project);
		setTitle("Select Checks to Be Applied");

		this.project	= project;

		init();
		initContent();
	}


	private void createUIComponents() {
		listTools = new CheckboxTree();
		listTools.setCellRenderer(new ListToolsTreeCellRenderer());
		listTools.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		listTools.addTreeSelectionListener(this::onToolSelected);

		listToolsSpeedSearch = new TreeSpeedSearch(
				listTools,
				ListToolsTreeDataModel::EntryToString
		);

		TreeUtil.installActions(listTools);

		tToolProperties = new PropertiesTable();
		tToolProperties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tToolProperties.getSelectionModel().addListSelectionListener(this::onToolPropertySelected);

		// customize the empty text to give users a hint to select a check on the left side
		tToolProperties.getEmptyText().setText("Select any check on the left side to view it's properties.");
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return root;
	}


	protected void initContent() {
		setCurrentDescriptionText(null);

		txtDocumentationLink.addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
					Desktop.getDesktop().browse(event.getURL().toURI());
				}
				catch (IOException | URISyntaxException e) {
					Logger.getInstance(this.getClass()).error(e);
				}
			}
		});

		listTools.setPaintBusy(true);
		listTools.setModel(null);
		ToolCollection.requestAvailableTools(availableTransforms -> {
			Collections.sort(
					availableTransforms,
					(ToolController a, ToolController b) -> a.getName().compareTo(b.getName())
			);

			// restore defaults on all tools
			for(ToolController tool : availableTransforms) {
				tool.onRestoreDefaults();
			}

			EventQueue.invokeLater(() -> {
				setData(availableTransforms);
				listTools.setPaintBusy(false);
			});
		});
	}


	protected void setData(@NotNull List<ToolController> tools) {
		List<ListToolsTreeDataModel.Entry<ToolController>> entries = new ArrayList<>(tools.size());
		listToolsModel = new ListToolsTreeDataModel<>();

		for(ToolController ctrl : tools) {
			entries.add(listToolsModel.add(ctrl, ctrl.getName()));
		}

		listToolsModel.foldEntries();

		listTools.setModel(listToolsModel);

		for(ListToolsTreeDataModel.Entry<ToolController> entry : entries) {
			ToolController tool = entry.getItem();
			boolean enabled = Options.isToolEnabled(tool);
			listTools.setNodeState(entry, enabled);

			// groups with checked nodes should be expanded by default
			if (enabled) {
				listTools.expandPath(new TreePath(entry.getParent().getPath()));
			}
		}
	}


	protected void onToolSelected(TreeSelectionEvent e) {
		ListToolsTreeDataModel.Entry[] selection = listTools.getSelectedNodes(ListToolsTreeDataModel.Entry.class, null);

		if (selection.length == 1) {
			@SuppressWarnings("unchecked")
			ListToolsTreeDataModel.Entry<ToolController> entry = selection[0];
			setSelectedTool(entry.getItem());
		}
		else {
			setSelectedTool(null);
		}
	}


	protected void onToolPropertySelected(ListSelectionEvent e) {
		if (currentToolProperties != null) {
			int index = tToolProperties.getSelectedRow();

			if (index >= 0 && index < currentToolProperties.getRowCount()) {
				PropertyInstance property = currentToolProperties.getProperty(index);
				setCurrentDescriptionText(property.getDescriptor().getDescription());
			}
			else {
				setCurrentDescriptionText(null);
			}
		}
	}


	protected void setSelectedTool(ToolController tool) {
		if (currentTool != tool) {
			currentTool = tool;

			if (tool != null) {
				currentToolProperties = new PropertiesTableModel(tool.getProperties());
				tToolProperties.setModel(currentToolProperties);
				setCurrentDescriptionText(currentTool.getDescription());

				toolsConfigContainer.revalidate();
				toolsConfigContainer.repaint();
			}
		}
	}


	public void setCurrentDescriptionText(String text) {
		if (text != null && !text.isEmpty()) {
			txtCurrentDescription.setText(text);
			txtCurrentDescription.setVisible(true);
		}
		else {
			txtCurrentDescription.setText("");
			txtCurrentDescription.setVisible(false);
		}
	}


	public int countSelectedTools() {
		ToolController[] selection = listTools.getCheckedNodes(ToolController.class, null);
		return selection.length;
	}


	public ToolController[] getSelectedTools() {
		assert selectedTools != null; // should queried only after this dialog was closed
		return selectedTools;
	}


	private void collectSelectedTools() {
		List<ToolController> selectedTools = new ArrayList<>();

		for(ListToolsTreeDataModel.Entry<ToolController> entry : listToolsModel.getEntries()) {
			ToolController tool    = entry.getItem();
			boolean        enabled = entry.isChecked();

			if (tool != null) {
				Options.setToolEnabled(tool, enabled);
				tool.OnConfigAccepted();
			}

			if (enabled) {
				selectedTools.add(tool);
			}
		}

		this.selectedTools = selectedTools.toArray(new ToolController[selectedTools.size()]);
	}


	@Nullable
	@Override
	protected ValidationInfo doValidate() {
		ValidationInfo info = super.doValidate();
		if (info != null) {
			return info;
		}

		if (countSelectedTools() == 0) {
			return new ValidationInfo("No tools selected");
		}

		return null;
	}


	@Override
	protected void doOKAction() {
		collectSelectedTools();
		super.doOKAction();
	}
}
