package org.coode.cloud.view;

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
import org.protege.editor.owl.ui.renderer.OWLEntityRenderer;
import org.protege.editor.owl.ui.renderer.OWLEntityRendererListener;
import org.protege.editor.owl.ui.view.AbstractOWLSelectionViewComponent;
import org.protege.editor.owl.ui.OWLObjectComparator;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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
 * http://www.cs.man.ac.uk/~drummond<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Sep 26, 2006<br><br>
 * <p/>
 */
public abstract class AbstractCloudView<O extends OWLEntity> extends AbstractOWLSelectionViewComponent {

    private static final String ZOOM_LABEL = "Zoom in or out of the view";
    private static final String FILTER_LABEL = "Filter out low ranked results";

    private static final int MAX_ZOOM_MIN_SIZE = 24;
    private static final int MIN_ZOOM_MIN_SIZE = 1;

    // for ordering of the entities
    private Comparator<? super O> alphaComparator;
    private Comparator<O> scoreComparator;

    private OWLCloudModel<O> model;

    private CloudComponent<O> cloudComponent;
    private JComponent sliderPanel;
    private JSlider thresholdSlider;
    private JSlider zoomSlider;

    private boolean updateViewRequired = false; // set to true if an update was attempted, but failed (when not showing)

    private ChangeListener thresholdSliderListener = new ChangeListener() {
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.refill(thresholdSlider.getValue());
        }
    };

    private ChangeListener zoomSliderListener = new ChangeListener() {
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.setZoom(zoomSlider.getValue());
            cloudComponent.doLayout(false);
        }
    };

    // listen to the model, which updates based on
    private ChangeListener modelChangeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent changeEvent) {
            cloudComponent.clearLabelCache();
            cloudComponent.refill();
        }
    };

    private OWLModelManagerListener modelManagerListener = new OWLModelManagerListener() {
        public void handleChange(OWLModelManagerChangeEvent event) {
            if (event.getType() == EventType.ENTITY_RENDERER_CHANGED ||
                event.getType() == EventType.ENTITY_RENDERING_CHANGED) {
                cloudComponent.clearLabelCache();
                cloudComponent.doLayout(true);
            }
        }
    };

    private OWLEntityRendererListener rendererListener = new OWLEntityRendererListener() {
        public void renderingChanged(OWLEntity entity, OWLEntityRenderer renderer) {
            cloudComponent.clearLabelCache();
            cloudComponent.doLayout(true);
        }
    };

    private HierarchyListener componentHierarchyListener = new HierarchyListener() {
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

    private DisposableAction sortAction =
            new DisposableAction("Sort (switch between alphabetic and value order)",
                                 Icons.getIcon("sort.ascending.png")) {
                public void dispose() {
                }

                public void actionPerformed(ActionEvent actionEvent) {
                    if (cloudComponent.getComparator() == scoreComparator) {
                        cloudComponent.setComparator(alphaComparator);
                    }
                    else {
                        cloudComponent.setComparator(scoreComparator);
                    }
                    cloudComponent.doLayout(true);
                }
            };

    private DisposableAction normaliseAction =
            new DisposableAction("Stretch (emphasize the differences between labels)",
                                 new ImageIcon(AbstractCloudView.class.getResource("stretch.png"))) {
                public void dispose() {
                }

                public void actionPerformed(ActionEvent actionEvent) {
                    cloudComponent.setNormalise(!cloudComponent.isNormalised());
                    cloudComponent.doLayout(false);
                }
            };


    private DisposableAction exportAction = new DisposableAction("Export",
                                                                 Icons.getIcon("project.save.gif")){

        public void dispose() {
        }


        public void actionPerformed(ActionEvent event) {
            handleExport();
        }
    };


    private DisposableAction printAction =
            new DisposableAction("Print",
                                 new ImageIcon(AbstractCloudView.class.getResource("Print16.gif"))){
                public void dispose() {
                }

                public void actionPerformed(ActionEvent actionEvent) {
                    printContent();
                }
            };

    private SelectionListener<O> selectionListener = new SelectionListener<O>(){
        public void selectionChanged(O selection) {
            getOWLWorkspace().getOWLSelectionModel().setSelectedEntity(selection);
        }
    };


    public void initialiseView() throws Exception {

        setLayout(new BorderLayout(6, 6));

        model = createModel();
        model.dataChanged(); // ?? needed
        model.setSync(true); // this will force a reload the first time

        cloudComponent = new CloudSwingComponent<O>(model);
        cloudComponent.addSelectionListener(selectionListener);
        cloudComponent.setZoom(4);

        alphaComparator = getOWLModelManager().getOWLObjectComparator();
        scoreComparator = model.getComparator();
        cloudComponent.setComparator(getOWLModelManager().getOWLObjectComparator());

        sliderPanel = createSliderPanel();
        add(sliderPanel, BorderLayout.NORTH);
        final JScrollPane scroller = new JScrollPane(cloudComponent.getComponent());
        scroller.getViewport().setBackground(Color.WHITE);
        add(scroller, BorderLayout.CENTER);

        addAction(sortAction, "D", "A");
        addAction(normaliseAction, "D", "B");
        addAction(exportAction, "E", "A");
        addAction(printAction, "E", "B");

        // add listeners
        getOWLModelManager().getOWLEntityRenderer().addListener(rendererListener);
        getOWLModelManager().addListener(modelManagerListener);
        model.addChangeListener(modelChangeListener);
        addHierarchyListener(componentHierarchyListener);
    }

    protected final JComponent getSliderPanel(){
        return sliderPanel;
    }

    protected CloudComponent getCloudComponent() {
        return cloudComponent;
    }

    private JComponent createSliderPanel() {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));

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
        thresholdPanel.setLayout(new BoxLayout(thresholdPanel, BoxLayout.LINE_AXIS));
        thresholdSlider = new JSlider(0, 100);
        thresholdSlider.setValue(cloudComponent.getThreshold());
        thresholdSlider.addChangeListener(thresholdSliderListener);
        thresholdSlider.setToolTipText(FILTER_LABEL);
        thresholdPanel.add(new JLabel(Icons.getIcon("filter.remove.png")));
        thresholdPanel.add(thresholdSlider);
        thresholdPanel.add(new JLabel(Icons.getIcon("filter.add.png")));

        sliderPanel.add(zoomPanel);
        sliderPanel.add(thresholdPanel);

        return sliderPanel;
    }

    protected abstract OWLCloudModel<O> createModel();

    protected final OWLObject updateView() {
        if (isShowing()) {
            cloudComponent.setSelection(getSelectedEntity());
            updateViewRequired = false;
        }
        else {
            updateViewRequired = true;
        }

        return (O)cloudComponent.getSelection();
    }

    private O getSelectedEntity() {
        if (isOWLClassView()) {
            return (O)getOWLWorkspace().getOWLSelectionModel().getLastSelectedClass();
        }
        else if (isOWLObjectPropertyView()) {
            return (O)getOWLWorkspace().getOWLSelectionModel().getLastSelectedObjectProperty();
        }
        else if (isOWLDataPropertyView()) {
            return (O)getOWLWorkspace().getOWLSelectionModel().getLastSelectedDataProperty();
        }
        else if (isOWLIndividualView()) {
            return (O)getOWLWorkspace().getOWLSelectionModel().getLastSelectedIndividual();
        }
        else {
            return (O)getOWLWorkspace().getOWLSelectionModel().getSelectedEntity();
        }
    }

    public void disposeView() {
        cloudComponent.removeSelectionListener(selectionListener);

        model.removeChangeListener(modelChangeListener);
        model.dispose();

        zoomSlider.removeChangeListener(zoomSliderListener);
        thresholdSlider.removeChangeListener(thresholdSliderListener);
        getOWLModelManager().getOWLEntityRenderer().removeListener(rendererListener);
        getOWLModelManager().removeListener(modelManagerListener);

        removeHierarchyListener(componentHierarchyListener);
    }


    private void handleExport() {
        try {
            Set<String> extensions = new HashSet<String>();
            extensions.add("html");
            String fileName = "cloud.html";
            File f = UIUtil.saveFile((Window) SwingUtilities.getAncestorOfClass(Window.class, this),
                                     "Save cloud to",
                                     extensions,
                                     fileName);
            if (f != null) {
                f.getParentFile().mkdirs();
                FileWriter out = new FileWriter(f);
                BufferedWriter bufferedWriter = new BufferedWriter(out);

                CloudHTMLRenderer<O> ren = new CloudHTMLRenderer<O>(model);
                ren.setComparator(cloudComponent.getComparator());
                ren.setNormalise(cloudComponent.isNormalised());
                ren.setThreshold(cloudComponent.getThreshold());
                ren.setZoom(cloudComponent.getZoom());
                ren.render(bufferedWriter);

                out.close();

                FileUtils.showFile(f);
            }
        }
        catch (IOException e) {
            ProtegeApplication.getErrorLog().handleError(Thread.currentThread(), e);
        }
    }

    private void printContent() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(cloudComponent);
        if (printJob.printDialog()){
            try {
                printJob.print();
            }
            catch(PrinterException pe) {
                ProtegeApplication.getErrorLog().handleError(Thread.currentThread(), pe);
            }
        }
    }

    class MyPanel extends JPanel implements Scrollable {
        public MyPanel(LayoutManager layoutManager) {
            super(layoutManager);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(300, 0);
        }

        public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
            return 10;
        }

        public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
            return 20;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
