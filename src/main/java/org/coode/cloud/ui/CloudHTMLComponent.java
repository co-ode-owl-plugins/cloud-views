package org.coode.cloud.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.RepaintManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.log4j.Logger;
import org.coode.cloud.model.CloudModel;
import org.coode.cloud.view.SelectionListener;

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
public class CloudHTMLComponent<O> extends JEditorPane implements CloudComponent<O> {

	private static final long serialVersionUID = -4479351166624892469L;

	protected final Logger logger = Logger.getLogger(this.getClass());

	protected CloudModel<O> model;

    private O currentSelection;

    private boolean layoutRequired = false;

    private Set<SelectionListener<O>> listeners = new HashSet<>();

    private HTMLEditorKit eKit = new HTMLEditorKit();

    protected CloudHTMLRenderer<O> renderer;

    protected PrintWriter w; // for the renderer thread to write into

    private HyperlinkListener linkListener = new HyperlinkListener(){
        @Override
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                notifySelectionChanged(model.getEntity(event.getDescription())); // messy - just uses the rendered text
            }
        }
    };

    public CloudHTMLComponent(CloudModel<O> model) {

        this.model = model;

        renderer = new CloudHTMLRenderer<>(model);

        setEditable(false);

        addHyperlinkListener(linkListener);

        doLayout(true);
    }

    @Override
    public void doLayout(boolean recreateLabels) {
        if (isShowing()) {
            if (currentSelection != null) {
                renderer.setSelection(currentSelection);
            }

            Runnable generateHTML = getRendererProcess();

            if (generateHTML != null){
                try (PipedReader r = new PipedReader()){
                    w = new PrintWriter(new PipedWriter(r));
                    new Thread(generateHTML).start();
                    HTMLDocument htmlDoc = (HTMLDocument)eKit.createDefaultDocument();
                    setContentType("text/html");
                    eKit.read(r, htmlDoc, 0);
                    setDocument(htmlDoc);
                } catch (Exception e) {
                    logger.error(e);
                }
            }

            getParent().validate();

            scrollToSelected();
            layoutRequired = false;
        }
        else {
            layoutRequired = true;
        }
    }

    @Override
    public void clearLabelCache() {
        //@@TODO implement
    }

    /**
     * Creates a rendering thread for the view
     * @return a Runnable
     */
    private Runnable getRendererProcess() {
        return new Runnable(){
            @Override
            public void run() {
                try {
                    renderer.render(w);
                }
                catch (IOException e) {
                    logger.error(e);
                }
                w.close();
            }
        };
    }

    @Override
    public void paint(Graphics graphics) {
        if (layoutRequired){
            doLayout(true);
        }
        super.paint(graphics);
    }

    public CloudHTMLRenderer<O> getRenderer(){
        return renderer;
    }

    /**
     * Recreate the view from the model - eg if the threshold or model have changed
     */
    @Override
    public final void refill(int threshold) {
        renderer.setThreshold(threshold);
        doLayout(true);
    }

    @Override
    public final void refill(){
        refill(renderer.getThreshold());
    }

    @Override
    public void setSelection(O newSelection) {
        currentSelection = newSelection;
        renderer.setSelection(newSelection);
        refill(); //@@TODO could be more efficient and find the element to replace
        scrollToSelected();
    }

    @Override
    public O getSelection(){
        return currentSelection;
    }

    private void scrollToSelected() {
        // @@TODO implement
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(300, 0);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
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

    @Override
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

    @Override
    public void addSelectionListener(SelectionListener<O> l){
        listeners.add(l);
    }

    @Override
    public void removeSelectionListener(SelectionListener<O> l){
        listeners.remove(l);
    }

    protected void notifySelectionChanged(O entity) {
        for (SelectionListener<O> l : listeners){
            l.selectionChanged(entity);
        }
    }

//////////// wrap CloudHTMLRenderer

    @Override
    public boolean requiresRedraw() {
        return layoutRequired;
    }

    @Override
    public int getZoom() {
        return renderer.getZoom();
    }

    @Override
    public void setZoom(int value) {
        renderer.setZoom(value);
    }

    @Override
    public Comparator<? super O> getComparator() {
        return renderer.getComparator();
    }

    @Override
    public void setComparator(Comparator<? super O> comparator) {
        renderer.setComparator(comparator);
    }

    @Override
    public boolean isNormalised() {
        return renderer.getNormalise();
    }

    @Override
    public void setNormalise(boolean normalise) {
        renderer.setNormalise(normalise);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public int getThreshold() {
        return renderer.getThreshold();
    }
}
