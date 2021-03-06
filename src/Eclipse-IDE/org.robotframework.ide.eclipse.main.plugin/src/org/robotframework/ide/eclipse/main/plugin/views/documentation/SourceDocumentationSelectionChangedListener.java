/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.views.documentation;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.rf.ide.core.testdata.model.IDocumentationHolder;
import org.rf.ide.core.testdata.model.RobotFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.RobotDocument;

public class SourceDocumentationSelectionChangedListener {

    private final DocumentationView view;

    private RobotSuiteFile suiteFile;

    private IDocument document;

    private IRegion currentRegion;

    private final AtomicBoolean isEditing = new AtomicBoolean();

    private static final Pattern POSSIBLE_KEYWORD_PATTERN = Pattern.compile("^[A-Za-z].*");

    private final DocViewDelayedUpdateJob delayedUpdateJob = new DocViewDelayedUpdateJob(
            "Documentation View Delayed Update Job");

    SourceDocumentationSelectionChangedListener(final DocumentationView view) {
        this.view = view;
    }

    public void positionChanged(final IDocument document, final RobotSuiteFile suiteFile, final IRegion region,
            final boolean isEditing) {

        if (delayedUpdateJob.getState() == Job.SLEEPING) {
            delayedUpdateJob.cancel();
        }

        this.document = document;
        this.suiteFile = suiteFile;
        this.currentRegion = region;
        this.isEditing.set(isEditing);

        delayedUpdateJob.schedule(DocViewDelayedUpdateJob.DELAY);
    }

    class DocViewDelayedUpdateJob extends Job {

        public static final int DELAY = 700;

        public DocViewDelayedUpdateJob(final String name) {
            super(name);
            setSystem(true);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            RobotFile linkedFile = suiteFile.getLinkedElement();
            if (linkedFile == null && document != null) {
                try {
                    linkedFile = ((RobotDocument) document).getNewestModel();
                } catch (final InterruptedException e) {
                    // linked file will be null then
                }
            }

            if (linkedFile != null) {
                final int lineSelected;
                try {
                    lineSelected = (document.getLineOfOffset(currentRegion.getOffset()) + 1);
                } catch (final BadLocationException e1) {
                    // shouldn't happen
                    e1.printStackTrace();
                    return Status.OK_STATUS;
                }

                final Optional<IDocumentationHolder> docSettingToShow = linkedFile.getParent()
                        .findDocumentation(currentRegion.getOffset(), lineSelected);

                if (docSettingToShow.isPresent()) {
                    if (isEditing.get()) {
                        view.resetCurrentlyDisplayedElement();
                    }
                    view.showDocumentation(docSettingToShow.get(), suiteFile);

                } else if (shouldTryToFindKeywordDoc()) {
                    try {
                        final String textFromCurrentRegion = document.get(currentRegion.getOffset(),
                                currentRegion.getLength());
                        if (isPossibleKeyword(textFromCurrentRegion)) {
                            view.showLibdoc(textFromCurrentRegion, suiteFile);
                        }
                    } catch (final BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }

            return Status.OK_STATUS;
        }

        private boolean shouldTryToFindKeywordDoc() {
            return currentRegion != null && view.hasShowLibdocEnabled() && !isEditing.get();
        }

        private boolean isPossibleKeyword(final String text) {
            return POSSIBLE_KEYWORD_PATTERN.matcher(text).matches();
        }
    }

}
