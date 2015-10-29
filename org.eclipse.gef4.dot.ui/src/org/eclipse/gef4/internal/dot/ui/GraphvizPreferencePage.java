/*******************************************************************************
 * Copyright (c) 2014 Fabian Steeg (hbz), and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg (hbz) - initial API & implementation
 *     Tamas Miklossy (itemis AG) - Refactoring of preferences (bug #446639)
 *
 *******************************************************************************/
package org.eclipse.gef4.internal.dot.ui;

import java.io.File;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.gef4.internal.dot.parser.ui.internal.DotActivator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.Preferences;

/**
 * Store and access the path to the 'dot' executable in the preference store.
 * The path can be set by the user, using a file selection dialog. The selected
 * location is stored in the bundle's preferences and available from there after
 * the initial setting.
 */
public class GraphvizPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private static final String DOT_SELECT_SHORT = DotUiMessages.GraphvizPreference_0;
	private static final String DOT_SELECT_LONG = DotUiMessages.GraphvizPreference_1;
	private static final String INVALID_DOT_EXECUTABLE = DotUiMessages.GraphvizPreference_2;
	private static final String INVALID_GRAPHVIZ_CONF = DotUiMessages.GraphvizPreference_3;
	private static final String GRAPHVIZ_CONF_HINT = DotUiMessages.GraphvizPreference_4;

	public static final String DOT_PATH_PREF_KEY = "dotpath"; //$NON-NLS-1$

	public static boolean isGraphvizConfigured() {
		return getDotPathFromPreferences().length() != 0;
	}

	public static void showGraphvizConfigurationDialog() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		new GraphvizConfigurationDialog(shell).open();
	}

	public static String getDotPathFromPreferences() {
		String dotExecutablePath = dotUiPrefs().get(DOT_PATH_PREF_KEY, ""); //$NON-NLS-1$
		if (dotExecutablePath.isEmpty()) {
			return "";//$NON-NLS-1$
		}
		String dotDirPath = new File(dotExecutablePath).getParent();
		return dotDirPath;
	}

	private static boolean isValidDotExecutable(String path) {
		File file = new File(path);
		return file.getName().equals("dot") || file.getName().equals("dot.exe"); //$NON-NLS-1$//$NON-NLS-2$
	}

	private static Preferences dotUiPrefs() {
		String qualifier = DotActivator.getInstance().getBundle()
				.getSymbolicName();
		Preferences preferences = ConfigurationScope.INSTANCE
				.getNode(qualifier);
		return preferences;
	}

	@Override
	public void init(IWorkbench workbench) {
		String qualifier = DotActivator.getInstance().getBundle()
				.getSymbolicName();
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(
				ConfigurationScope.INSTANCE, qualifier);
		setPreferenceStore(preferenceStore);

		setDescription(DOT_SELECT_LONG);

	}

	@Override
	protected void createFieldEditors() {
		FileFieldEditor fileFieldEditor = new FileFieldEditor(DOT_PATH_PREF_KEY,
				DOT_SELECT_SHORT, true, FileFieldEditor.VALIDATE_ON_KEY_STROKE,
				getFieldEditorParent()) {

			{
				setErrorMessage(INVALID_DOT_EXECUTABLE);
			}

			@Override
			public boolean isValid() {
				boolean isValid = super.isValid();
				String currentValue = getStringValue();
				if (!currentValue.isEmpty()) {
					isValid = isValid && isValidDotExecutable(currentValue);
				}
				return isValid;
			}

			@Override
			protected void refreshValidState() {
				super.refreshValidState();
				if (!isValid()) {
					showErrorMessage(getErrorMessage());
				}
				checkState();
			}

			@Override
			public boolean doCheckState() {
				String currentValue = getStringValue();
				if (!currentValue.isEmpty()) {
					return isValidDotExecutable(currentValue);
				}
				return super.doCheckState();
			}

		};
		addField(fileFieldEditor);
	}

	public static class GraphvizConfigurationDialog extends MessageDialog {

		public GraphvizConfigurationDialog(Shell parentShell) {
			super(parentShell, INVALID_GRAPHVIZ_CONF, null, GRAPHVIZ_CONF_HINT,
					WARNING, new String[] { IDialogConstants.CANCEL_LABEL }, 0);
		}

		@Override
		protected Control createMessageArea(Composite composite) {
			// prevent creation of messageLabel by super implementation
			String linkText = message;
			message = null;
			super.createMessageArea(composite);
			message = linkText;

			Link messageLink = new Link(composite, SWT.WRAP);
			messageLink.setText(message);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
					.grab(true, false).applyTo(messageLink);
			messageLink.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					Shell shell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					PreferenceDialog pref = PreferencesUtil
							.createPreferenceDialogOn(shell,
									"org.eclipse.gef4.internal.dot.ui.GraphvizPreferencePage", //$NON-NLS-1$
									null, null);
					if (pref != null) {
						close();
						pref.open();
					}
				}
			});
			return composite;
		}

	}

}