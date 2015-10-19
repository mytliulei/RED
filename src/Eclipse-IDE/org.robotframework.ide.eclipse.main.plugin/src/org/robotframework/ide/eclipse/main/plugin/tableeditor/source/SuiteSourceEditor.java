/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.GotoLineAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.StatusLineContributionItem;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotEditorSources;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditorActionBarContributor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.handler.ToggleBreakpointHandler;

public class SuiteSourceEditor extends TextEditor {

    private static final String SOURCE_PART_CONTEXT_ID = "org.robotframework.ide.eclipse.tableeditor.sources.context";

    @Inject
    @Named(RobotEditorSources.SUITE_FILE_MODEL)
    protected RobotSuiteFile fileModel;

    private SuiteSourceEditorFoldingSupport foldingSupport;

    public SourceViewer getViewer() {
        return (SourceViewer) getSourceViewer();
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();

        setSourceViewerConfiguration(new SuiteSourceEditorConfiguration(this));
        setDocumentProvider(new SuiteSourceDocumentProvider());
    }

    @Override
    protected void createActions() {
        super.createActions();

        final GotoLineAction gotoAction = new GotoLineAction(this);
        gotoAction.setActionDefinitionId(ITextEditorActionConstants.GOTO_LINE);
        setAction(ITextEditorActionConstants.GOTO_LINE, gotoAction);
    }

    @Override
    protected void doSetInput(final IEditorInput input) throws CoreException {
        super.doSetInput(input);
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);
        final ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();

        installProjectionAndFolding(viewer);
        new SuiteSourceCurrentCellHighlighter(fileModel.getFile(), viewer.getDocument()).install(viewer);
        new SuiteSourceOccurrenceMarksHighlighter(fileModel.getFile(), viewer.getDocument()).install(viewer);
        installBreakpointTogglingOnDoubleClick();
        installStatusBarUpdater(viewer);

        activateContext();
    }

    @Override
    protected ISourceViewer createSourceViewer(final Composite parent, final IVerticalRuler ruler, final int styles) {
        final ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(),
                styles);
        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);
        return viewer;
    }

    @Override
    protected boolean isEditorInputIncludedInContextMenu() {
        return false;
    }

    @Override
    protected void installTabsToSpacesConverter() {
        // we will install our own edit strategy for tabs converting
    }

    private void installProjectionAndFolding(final ProjectionViewer viewer) {
        // turn projection mode on
        new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors()).install();
        viewer.doOperation(ProjectionViewer.TOGGLE);
        foldingSupport = new SuiteSourceEditorFoldingSupport(viewer.getProjectionAnnotationModel());
    }

    private void installStatusBarUpdater(final ProjectionViewer viewer) {
        viewer.getTextWidget().addCaretListener(new CaretListener() {

            @Override
            public void caretMoved(final CaretEvent event) {
                updateLineLocationStatusBar(event.caretOffset);
                updateLineDelimitersStatus();
            }
        });
    }

    private void updateLineLocationStatusBar(final int caretPostion) {
        try {
            final IDocument document = getDocument();
            int lineNumber = document.getLineOfOffset(caretPostion);
            final int columnNumber = caretPostion - document.getLineOffset(lineNumber) + 1;
            lineNumber++;

            final StatusLineContributionItem find = (StatusLineContributionItem) getEditorSite().getActionBars()
                    .getStatusLineManager()
                    .find(ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION);
            find.setText(lineNumber + ":" + columnNumber);
        } catch (final BadLocationException e) {
            RedPlugin.logError("Unable to get position in source editor in order to update status bar", e);
        }
    }

    private void updateLineDelimitersStatus() {
        final IDocument document = getDocument();

        final StatusLineContributionItem find = (StatusLineContributionItem) getEditorSite().getActionBars()
                .getStatusLineManager()
                .find(RobotFormEditorActionBarContributor.DELIMITERS_INFO_ID);
        String delimiter = DocumentUtilities.getDelimiter(document);
        find.setText("\r\n".equals(delimiter) ? "CR+LF" : "LF");
    }
	
    private void installBreakpointTogglingOnDoubleClick() {
        getVerticalRuler().getControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(final MouseEvent event) {
                try {
                    final IFile file = (IFile) getEditorInput().getAdapter(IResource.class);
                    final int line = computeBreakpointLineNumber(event.y);
                    ToggleBreakpointHandler.E4ToggleBreakpointHandler.toggle(file, line);
                } catch (final CoreException e) {
                    RedPlugin.logError("Unable to toggle breakpoint", e);
                }
            }
        });
    }

    private int computeBreakpointLineNumber(final int eventY) {
        final StyledText text = getSourceViewer().getTextWidget();
        final int lineHeight = text.getLineHeight();
        final int line = (int) Math.round((eventY / (double) lineHeight)) + getSourceViewer().getTopIndex();

        final int lineBottomPixel = text.getLinePixel(line) - lineHeight;
        if (eventY < lineBottomPixel) {
            return line - 1;
        } else if ((eventY - lineBottomPixel) > lineHeight) {
            return line + 1;
        } else {
            return line;
        }
    }

    private void activateContext() {
        final IContextService service = getSite().getService(IContextService.class);
        service.activateContext(SOURCE_PART_CONTEXT_ID);
    }

    @Override
    public void setFocus() {
        super.setFocus();

        getSourceViewer().getTextWidget().setCaretOffset(0);
    }

    @Override
    public void doSave(final IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public IDocument getDocument() {
        return getDocumentProvider().getDocument(getEditorInput());
    }

    RobotSuiteFile getFileModel() {
        return fileModel;
    }

    SuiteSourceEditorFoldingSupport getFoldingSupport() {
        return foldingSupport;
    }

    /**
     * Returns line number of cursor position.
     * 
     * @return Line number indexed from 1
     */
    public int getCurrentLine() {
        final StyledText text = getSourceViewer().getTextWidget();
        return text.getLineAtOffset(text.getSelection().x) + 1;
    }

    /**
     * Returns line number from ruler activity.
     * 
     * @return Line number indexed from 1
     */
    public int getLineFromRulerActivity() {
        return getVerticalRuler().getLineOfLastMouseButtonActivity() + 1;
    }
}
