/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.dialogs.AbstractMavenDialog;


/**
 * A simple dialog allowing the selection of Maven projects and subfolders containing POMs.
 */
public class MavenProjectSelectionDialog extends AbstractMavenDialog {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectSelectionDialog.class);

  protected static final String DIALOG_SETTINGS = MavenProjectSelectionDialog.class.getName();

  protected static final long SEARCH_DELAY = 500L; //in milliseconds

  private FilteredTree filteredTree;

  private boolean useCheckboxTree;

  /** Creates a dialog instance. */
  public MavenProjectSelectionDialog(Shell parent, boolean useCheckboxTree) {
    this(parent);
    this.useCheckboxTree = useCheckboxTree;
  }

  /** Creates a dialog instance. */
  public MavenProjectSelectionDialog(Shell parent) {
    super(parent, DIALOG_SETTINGS);

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle(Messages.projectSelectionDialogTitle);
  }

  /** Produces the result of the selection. */
  protected void computeResult() {
    if(useCheckboxTree) {
      List<Object> result = new ArrayList<Object>();
      collectCheckedItems(getViewer().getTree().getItems(), result);
      setResult(result);
    } else {
      setResult(((IStructuredSelection) getViewer().getSelection()).toList());
    }
  }

  private void collectCheckedItems(TreeItem[] items, List<Object> list) {
    for(TreeItem item : items) {
      if(item.getChecked()) {
        Object data = item.getData();
        if(data != null) {
          list.add(data);
        }
      }
      collectCheckedItems(item.getItems(), list);
    }
  }

  /** Creates the dialog controls. */
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    filteredTree = new FilteredTree(composite, SWT.BORDER | (useCheckboxTree ? SWT.CHECK : 0), new PatternFilter(),
        true);
    filteredTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    getViewer().setContentProvider(new MavenContainerContentProvider());
    getViewer().setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    getViewer().setInput(ResourcesPlugin.getWorkspace());

    getViewer().addDoubleClickListener(event -> okPressed());

    return composite;
  }

  protected void okPressed() {
    super.okPressed();
  }

  protected TreeViewer getViewer() {
    return filteredTree.getViewer();
  }

  /** The content provider class. */
  protected static class MavenContainerContentProvider implements ITreeContentProvider {

    /** Returns the children of the parent node. */
    public Object[] getChildren(Object parent) {
      if(parent instanceof IWorkspace) {
        IProject[] projects = ((IWorkspace) parent).getRoot().getProjects();

        List<IProject> children = new ArrayList<IProject>();
        for(IProject project : projects) {
          try {
            if(project.isOpen() && project.hasNature(IMavenConstants.NATURE_ID)) {
              children.add(project);
            }
          } catch(CoreException e) {
            log.error("Error checking project: " + e.getMessage(), e);
          }
        }
        return children.toArray();
      } else if(parent instanceof IContainer) {
        IContainer container = (IContainer) parent;
        if(container.isAccessible()) {
          try {
            List<IResource> children = new ArrayList<IResource>();
            IResource[] members = container.members();
            for(int i = 0; i < members.length; i++ ) {
              if(members[i] instanceof IContainer
                  && ((IContainer) members[i]).exists(new Path(IMavenConstants.POM_FILE_NAME))) {
                children.add(members[i]);
              }
            }
            return children.toArray();
          } catch(CoreException e) {
            log.error("Error checking container: " + e.getMessage(), e);
          }
        }
      }
      return new Object[0];
    }

    /** Returns the parent of the given element. */
    public Object getParent(Object element) {
      if(element instanceof IResource) {
        return ((IResource) element).getParent();
      }
      return null;
    }

    /** Returns true if the element has any children. */
    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    /** Disposes of any resources used. */
    public void dispose() {
    }

    /** Handles the input change. */
    public void inputChanged(Viewer viewer, Object arg1, Object arg2) {
    }

    /** Returns the elements of the given root. */
    public Object[] getElements(Object element) {
      return getChildren(element);
    }
  }
}
