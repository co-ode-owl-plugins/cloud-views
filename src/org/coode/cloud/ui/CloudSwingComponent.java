package org.coode.cloud.ui;

import org.coode.cloud.model.CloudModel;
import org.coode.cloud.view.SelectionListener;
import org.coode.cloud.ui.ModifiedFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.*;

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
public class CloudSwingComponent<O> extends JPanel implements CloudComponent<O> {

	private static final long serialVersionUID = -6892845908165209765L;

	private static final Color BG_COLOUR = Color.WHITE;
    private static final Color SELECTION_COLOR = Color.BLUE;
    private static final String DEFAULT_FONT = "SansSerif";

    // capped maximum size of the font used to display entities
    private static final int MAX_SIZE = 64;

    private CloudModel<O> model;

    // cache for the label components
    private Map<O, JComponent> labels = new HashMap<O, JComponent>();

    // cache for the entities
    private java.util.List<O> entities;

    private O currentSelection;

    // whether the values are normalised before display
    // (this has the effect of spreading the range of sizes out across the whole spectrum)
    private boolean normalise = false;
    private boolean inverted = false;

    private boolean layoutRequired = false;
    private boolean completeRedrawRequired = false;

    private Comparator<? super O> comparator;

    private int threshold = 0;
    private int zoom = 0;

    private Set<SelectionListener<O>> listeners = new HashSet<SelectionListener<O>>();

    private MouseListener navListener = new MouseAdapter(){
        public void mouseClicked(MouseEvent mouseEvent) {
            O entity = model.getEntity(((JLabel)mouseEvent.getComponent()).getText());
            notifySelectionChanged(entity);
        }
    };

    public CloudSwingComponent(CloudModel<O> model) {
        super(new ModifiedFlowLayout());
        this.model = model;
        setBackground(BG_COLOUR);
        doLayout(true);
    }

    public void doLayout(boolean recreateLabels) {
        if (recreateLabels) {
            completeRedrawRequired = true;
        }

        if (isShowing()) {
            if (entities == null){
                entities = new ArrayList<O>(model.getEntities(threshold));
                Collections.sort(entities, comparator);
            }

            if (completeRedrawRequired) {
                removeAll();
                for (O entity : entities) {
                    add(getLabel(entity));
                }
            }
            else {
                for (O entity : entities) {
                    updateLabel(getLabel(entity), model.getValue(entity));
                }
            }

            if (currentSelection != null) {
                JComponent label = labels.get(currentSelection);
                if (label != null) {
                    label.setForeground(SELECTION_COLOR);
                }
            }

            getParent().validate();

            scrollToSelected();
            layoutRequired = false;
            completeRedrawRequired = false;
        }
        else {
            layoutRequired = true;
        }
    }

    public void paint(Graphics graphics) {
        if (layoutRequired){
            doLayout(true);
        }
        super.paint(graphics);
    }


    /**
     * Recreate the view from the model - eg if the threshold or model have changed
     */
    public final void refill(int threshold) {
        this.threshold = threshold;
        entities = null;
        doLayout(true);
    }

    public final void refill(){
        refill(getThreshold());
    }

    public void clearLabelCache(){
        for (JComponent label : labels.values()){
            label.removeMouseListener(navListener);
        }
        labels.clear();
    }

    private void updateLabel(JComponent c, int score) {
        c.setFont(getFont(score));
        c.setForeground(getColor(score));
    }

    private Color getColor(int value) {
        int score;
        if (normalise) {
            int relativeScore = value - model.getMin();
            int scoreRange = model.getRange();
            score = 50 + ((relativeScore * 205) / scoreRange);
        }
        else {
            score = Math.min(255, 50 + (zoom * value / 2));
        }
        if (!inverted) {
            score = 255 - score;
        }
        return new Color(score, score, score);
    }

    private Font getFont(int value) {
        int size;
        if (normalise) {
            int displayMin = zoom;
            int displayRange = MAX_SIZE - displayMin;
            int scoreRange = model.getRange();
            int relativeScore = value - model.getMin();
            size = displayMin + ((relativeScore * displayRange) / scoreRange);
        }
        else {
            size = Math.min(MAX_SIZE, zoom + (value / 2));
        }

        if (size > MAX_SIZE) {
            throw new RuntimeException("ERROR, OVER MAX SIZE: " + size);
        }

        return new Font(DEFAULT_FONT, Font.PLAIN, size);
    }

    private JComponent getLabel(O entity) {
        JComponent c = labels.get(entity);
        if (c == null) {
            c = new JLabel(model.getRendering(entity));
            c.addMouseListener(navListener);
            labels.put(entity, c);
        }
        int score = model.getValue(entity);
        updateLabel(c, score);
        c.setToolTipText(model.getRendering(entity) + ": " + score);
        return c;
    }

    public void setSelection(O newSelection) {
        JComponent label = labels.get(newSelection);
        if (label != null) {
            label.setForeground(SELECTION_COLOR);
        }

        if (currentSelection != null) {
            label = labels.get(currentSelection);
            if (label != null) {
                label.setForeground(getColor(model.getValue(currentSelection)));
            }
        }

        currentSelection = newSelection;

        scrollToSelected();
    }

    public O getSelection(){
        return currentSelection;
    }

    private void scrollToSelected() {
        if (currentSelection != null) {
            JComponent c = labels.get(currentSelection);
            if (c != null) {
                scrollRectToVisible(c.getBounds());
            }
        }
    }

    public void setComparator(Comparator<? super O> comparator) {
        this.comparator = comparator;
        if (entities != null){
            Collections.sort(entities, comparator);
        }
    }

    public Comparator<? super O> getComparator(){
        return comparator;
    }

    public boolean isNormalised() {
        return normalise;
    }

    public void setNormalise(boolean normalise) {
        this.normalise = normalise;
    }

    public JComponent getComponent() {
        return this;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getZoom(){
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
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

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        Graphics2D g2 = (Graphics2D) g;

        Dimension d = getSize(); //get size of document
        double panelWidth = d.width; //width in pixels
        double panelHeight = d.height; //height in pixels

        double pageHeight = pf.getImageableHeight(); //height of printer page
        double pageWidth = pf.getImageableWidth(); //width of printer page

        double scale = pageWidth / panelWidth;
        int totalNumPages = (int) Math.ceil(scale * panelHeight / pageHeight);

        //  make sure not print empty pages
        if (pageIndex >= totalNumPages) {
            return Printable.NO_SUCH_PAGE;
        }
        else {
            //  for faster printing, turn off double buffering
            RepaintManager currentManager = RepaintManager.currentManager(this);
            currentManager.setDoubleBufferingEnabled(false);

            //  shift Graphic to line up with beginning of print-imageable region
            g2.translate(pf.getImageableX(), pf.getImageableY());

            //  shift Graphic to line up with beginning of next page to print
            g2.translate(0f, -pageIndex * pageHeight);

            //  scale the page so the width fits...
            g2.scale(scale, scale);

            paint(g2); //repaint the page for printing

            currentManager.setDoubleBufferingEnabled(true);

            return Printable.PAGE_EXISTS;
        }
    }

    public void addSelectionListener(SelectionListener<O> l){
        listeners.add(l);
    }

    public void removeSelectionListener(SelectionListener<O> l){
        listeners.remove(l);
    }

    private void notifySelectionChanged(O entity) {
        for (SelectionListener<O> l : listeners){
            l.selectionChanged(entity);
        }
    }

    public boolean requiresRedraw() {
        return layoutRequired;
    }
}
