package org.coode.cloud.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.coode.cloud.model.OWLCloudModel;
import org.coode.cloud.ui.CloudComponent;
import org.coode.cloud.ui.CloudHTMLRenderer;
import org.coode.cloud.ui.CloudSwingComponent;
import org.protege.editor.core.FileUtils;
import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.ui.util.Icons;
import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.core.ui.view.DisposableAction;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.model.selection.OWLSelectionModel;
import org.protege.editor.owl.ui.renderer.OWLEntityRendererListener;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.AbstractOWLSelectionViewComponent;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

/*
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
/**
 * Author: Nick Drummond<br>
 * http://www.cs.man.ac.uk/~drummond<br>
 * <br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Sep 26, 2006<br>
 * <br>
 * <p/>
 */
public abstract class AbstractCloudView<O extends OWLEntity>
        extends AbstractOWLSelectionViewComponent {

    private static final long serialVersionUID = -2657880217389567784L;
    private static final String ZOOM_LABEL = "Zoom in or out of the view";
    private static final String FILTER_LABEL = "Filter out low ranked results";
    private static final int MAX_ZOOM_MIN_SIZE = 24;
    private static final int MIN_ZOOM_MIN_SIZE = 1;
    // for ordering of the entities
    protected Comparator<? super O> alphaComparator;
    protected Comparator<O> scoreComparator;
    protected OWLCloudModel<O> model;
    protected CloudComponent<O> cloudComponent;
    private JComponent sliderPanel;
    protected JSlider thresholdSlider;
    protected JSlider zoomSlider;
    // set to true if an update was attempted, but failed (when not showing)
    protected boolean updateViewRequired = false; 
    private ChangeListener thresholdSliderListener = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.refill(thresholdSlider.getValue());
        }
    };
    private ChangeListener zoomSliderListener = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.setZoom(zoomSlider.getValue());
            cloudComponent.doLayout(false);
        }
    };
    // listen to the model, which updates based on
    private ChangeListener modelChangeListener = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.clearLabelCache();
            cloudComponent.refill();
        }
    };
    private OWLModelManagerListener modelManagerListener = new OWLModelManagerListener() {

        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            if (event.getType() == EventType.ENTITY_RENDERER_CHANGED
                    || event.getType() == EventType.ENTITY_RENDERING_CHANGED) {
                cloudComponent.clearLabelCache();
                cloudComponent.doLayout(true);
            }
        }
    };
    private OWLEntityRendererListener rendererListener = new OWLEntityRendererListener() {

        @Override
        public void renderingChanged(OWLEntity entity,
                OWLModelManagerEntityRenderer renderer) {
            cloudComponent.clearLabelCache();
            cloudComponent.doLayout(true);
        }
    };
    private HierarchyListener componentHierarchyListener = new HierarchyListener() {

        @Override
        public void hierarchyChanged(HierarchyEvent hierarchyEvent) {
            model.setSync(isShowing());
            if (isShowing()) {
                if (updateViewRequired) {
                    updateView();
                }
                if (cloudComponent.requiresRedraw()) {
                    cloudComponent.doLayout(true);
                }
            }
        }
    };
    private DisposableAction sortAction = new DisposableAction(
            "Sort (switch between alphabetic and value order)",
            Icons.getIcon("sort.ascending.png")) {

        private static final long serialVersionUID = 7188950338724315982L;

        @Override
        public void dispose() {}

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (cloudComponent.getComparator() == scoreComparator) {
                cloudComponent.setComparator(alphaComparator);
            } else {
                cloudComponent.setComparator(scoreComparator);
            }
            cloudComponent.doLayout(true);
        }
    };
    private DisposableAction normaliseAction = new DisposableAction(
            "Stretch (emphasize the differences between labels)",
            new ImageIcon(AbstractCloudView.class.getResource("stretch.png"))) {

        private static final long serialVersionUID = -2057721261201305569L;

        @Override
        public void dispose() {}

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            cloudComponent.setNormalise(!cloudComponent.isNormalised());
            cloudComponent.doLayout(false);
        }
    };
    private DisposableAction exportAction = new DisposableAction("Export",
            Icons.getIcon("project.save.gif")) {

        private static final long serialVersionUID = -7892830118347300564L;

        @Override
        public void dispose() {}

        @Override
        public void actionPerformed(ActionEvent event) {
            handleExport();
        }
    };
    private DisposableAction printAction = new DisposableAction("Print",
            new ImageIcon(AbstractCloudView.class.getResource("Print16.gif"))) {

        private static final long serialVersionUID = -4874739064378311243L;

        @Override
        public void dispose() {}

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            printContent();
        }
    };
    private SelectionListener<O> selectionListener = new SelectionListener<O>() {

        @Override
        public void selectionChanged(O selection) {
            getOWLWorkspace().getOWLSelectionModel()
                    .setSelectedEntity(selection);
        }
    };

    @Override
    public void initialiseView() {
        setLayout(new BorderLayout(6, 6));
        model = createModel();
        model.dataChanged(); // ?? needed
        model.setSync(true); // this will force a reload the first time
        cloudComponent = new CloudSwingComponent<>(model);
        cloudComponent.addSelectionListener(selectionListener);
        cloudComponent.setZoom(4);
        alphaComparator = getOWLModelManager().getOWLObjectComparator();
        scoreComparator = model.getComparator();
        cloudComponent
                .setComparator(getOWLModelManager().getOWLObjectComparator());
        sliderPanel = createSliderPanel();
        add(sliderPanel, BorderLayout.NORTH);
        final JScrollPane scroller = new JScrollPane(
                cloudComponent.getComponent());
        scroller.getViewport().setBackground(Color.WHITE);
        add(scroller, BorderLayout.CENTER);
        addAction(sortAction, "D", "A");
        addAction(normaliseAction, "D", "B");
        addAction(exportAction, "E", "A");
        addAction(printAction, "E", "B");
        // add listeners
        getOWLModelManager().getOWLEntityRenderer()
                .addListener(rendererListener);
        getOWLModelManager().addListener(modelManagerListener);
        model.addChangeListener(modelChangeListener);
        addHierarchyListener(componentHierarchyListener);
    }

    protected final JComponent getSliderPanel() {
        return sliderPanel;
    }

    protected CloudComponent<O> getCloudComponent() {
        return cloudComponent;
    }

    private JComponent createSliderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        // zoom slider
        JPanel zoomPanel = new JPanel();
        zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.LINE_AXIS));
        zoomSlider = new JSlider(MIN_ZOOM_MIN_SIZE, MAX_ZOOM_MIN_SIZE);
        zoomSlider.setValue(cloudComponent.getZoom());
        zoomSlider.addChangeListener(zoomSliderListener);
        zoomSlider.setToolTipText(ZOOM_LABEL);
        zoomPanel.add(new JLabel(Icons.getIcon("zoom.out.png")));
        zoomPanel.add(zoomSlider);
        zoomPanel.add(new JLabel(Icons.getIcon("zoom.in.png")));
        // threshold slider
        JPanel thresholdPanel = new JPanel();
        thresholdPanel
                .setLayout(new BoxLayout(thresholdPanel, BoxLayout.LINE_AXIS));
        thresholdSlider = new JSlider(0, 100);
        thresholdSlider.setValue(cloudComponent.getThreshold());
        thresholdSlider.addChangeListener(thresholdSliderListener);
        thresholdSlider.setToolTipText(FILTER_LABEL);
        thresholdPanel.add(new JLabel(Icons.getIcon("filter.remove.png")));
        thresholdPanel.add(thresholdSlider);
        thresholdPanel.add(new JLabel(Icons.getIcon("filter.add.png")));
        panel.add(zoomPanel);
        panel.add(thresholdPanel);
        return panel;
    }

    protected abstract OWLCloudModel<O> createModel();

    @Override
    protected final OWLObject updateView() {
        if (isShowing()) {
            cloudComponent.setSelection((O) getSelectedEntity());
            updateViewRequired = false;
        } else {
            updateViewRequired = true;
        }
        return cloudComponent.getSelection();
    }

    private OWLEntity getSelectedEntity() {
        OWLSelectionModel selectionModel = getOWLWorkspace()
                .getOWLSelectionModel();
        if (isOWLClassView()) {
            return selectionModel.getLastSelectedClass();
        } else if (isOWLObjectPropertyView()) {
            return selectionModel.getLastSelectedObjectProperty();
        } else if (isOWLDataPropertyView()) {
            return selectionModel.getLastSelectedDataProperty();
        } else if (isOWLIndividualView()) {
            return selectionModel.getLastSelectedIndividual();
        } else {
            return selectionModel.getSelectedEntity();
        }
    }

    @Override
    public void disposeView() {
        cloudComponent.removeSelectionListener(selectionListener);
        model.removeChangeListener(modelChangeListener);
        model.dispose();
        zoomSlider.removeChangeListener(zoomSliderListener);
        thresholdSlider.removeChangeListener(thresholdSliderListener);
        getOWLModelManager().getOWLEntityRenderer()
                .removeListener(rendererListener);
        getOWLModelManager().removeListener(modelManagerListener);
        removeHierarchyListener(componentHierarchyListener);
    }

    protected void handleExport() {
        Set<String> extensions = new HashSet<>();
        extensions.add("html");
        String fileName = "cloud.html";
        File f = UIUtil.saveFile(
                SwingUtilities.getAncestorOfClass(Window.class, this),
                "Save cloud to", null, extensions, fileName);
        if (f != null) {
            f.getParentFile().mkdirs();
            try (FileWriter out = new FileWriter(f);
                    BufferedWriter bufferedWriter = new BufferedWriter(out);) {
                CloudHTMLRenderer<O> ren = new CloudHTMLRenderer<>(model);
                ren.setComparator(cloudComponent.getComparator());
                ren.setNormalise(cloudComponent.isNormalised());
                ren.setThreshold(cloudComponent.getThreshold());
                ren.setZoom(cloudComponent.getZoom());
                ren.render(bufferedWriter);
                out.close();
                FileUtils.showFile(f);
            } catch (IOException e) {
                ProtegeApplication.getErrorLog()
                        .handleError(Thread.currentThread(), e);
            }
        }
    }

    protected void printContent() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(cloudComponent);
        if (printJob.printDialog()) {
            try {
                printJob.print();
            } catch (PrinterException pe) {
                ProtegeApplication.getErrorLog()
                        .handleError(Thread.currentThread(), pe);
            }
        }
    }

    class MyPanel extends JPanel implements Scrollable {

        private static final long serialVersionUID = 2109739017191220178L;

        public MyPanel(LayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(300, 0);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle rectangle, int i,
                int i1) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle rectangle, int i,
                int i1) {
            return 20;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
