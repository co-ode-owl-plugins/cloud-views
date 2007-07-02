package org.coode.cloud.ui;

import org.coode.cloud.model.CloudModel;

import java.io.Writer;
import java.io.IOException;
import java.util.*;
import java.awt.*;

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
public class CloudHTMLRenderer<O> {

    private static final String SELECTION_COLOR = "#0000FF";

    // capped maximum size of the font used to display entities
    private static final int MAX_SIZE = 64;

    private Comparator<? super O> comparator;

    private java.util.List<O> entities;
    
    private CloudModel<O> model;

    private int threshold = 0;
    private int zoom = 0;
    private boolean normalise = false;
    private boolean inverted = false;
    private O currentSelection;

    public CloudHTMLRenderer(CloudModel<O> model){
        this.model = model;
    }

    public void render(Writer writer) throws IOException {

        entities = new ArrayList<O>(model.getEntities(threshold));

        Collections.sort(entities, comparator);

        writer.write("<html><head></head><body style='text-align: center;'>");

        for (O entity : entities){
            writer.write(getLabel(entity) + " ");
        }

        writer.write("</body></html>");

        writer.flush();
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

    private int getFontSize(int value) {
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

        return size;
    }

    private String getLabel(O entity) {
        String name = model.getRendering(entity);
        int score = model.getValue(entity);

        String colour = SELECTION_COLOR;
        if (!entity.equals(currentSelection)){
            final String rgb = Integer.toHexString(getColor(score).getRGB());
            colour = "#" + rgb.substring(2, rgb.length());
        }
        int size = getFontSize(score);

        // @@TODO set title text (for popup of value)

        return "<a href='" + name +
               "' style='color: " + colour +
               "; font-size: " + size +
               "' title='" + score +
               "'>" + name + "</a>";
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

    public boolean getNormalise() {
        return normalise;
    }

    public void setNormalise(boolean normalise) {
        this.normalise = normalise;
    }

    public int getZoom(){
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setSelection(O currentSelection) {
        this.currentSelection = currentSelection;
    }
}
