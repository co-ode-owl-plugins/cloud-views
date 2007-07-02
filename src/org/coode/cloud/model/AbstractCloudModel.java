package org.coode.cloud.model;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
public abstract class AbstractCloudModel<O> implements CloudModel<O> {

    private Map<O, Integer> entityValueMap = new HashMap<O, Integer>();

    private int min;
    private int max;

    private List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    protected AbstractCloudModel() {
        reload();
    }

    protected void reload() {
            min = Integer.MAX_VALUE;
            max = 0;

            entityValueMap.clear();

            for (O entity : getEntities()) {
                int value = calculateValue(entity);
                min = Math.min(min, value);
                max = Math.max(max, value);
                entityValueMap.put(entity, value);
            }

            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l : listeners) {
                l.stateChanged(e);
            }
    }

    public abstract Set<O> getEntities();

    public final Set<O> getEntities(int threshold) {

        threshold = normalize(threshold);

        Set<O> result = new HashSet<O>();

        for (O entity : entityValueMap.keySet()) {
            if (entityValueMap.get(entity) >= threshold) {
                result.add(entity);
            }
        }
        return result;
    }

    public String getRendering(O entity) {
        return entity.toString();
    }

    protected abstract int calculateValue(O entity);

    public final int getValue(O entity) {
        return entityValueMap.get(entity);
    }

    public final int getMin() {
        return min;
    }

    public final int getMax() {
        return max;
    }

    public final int getRange() {
        return max - min;
    }

    private int normalize(int threshold) {
        int range = max - min;
        threshold = min + (range * threshold) / 100;
        return threshold;
    }

    public void dispose() {
        listeners.clear();
    }

    public final void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public final void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    public Comparator<O> getComparator() {
        return new Comparator<O>() {
            public int compare(O entity, O entity1) {
                // we want to reverse the score comparison, to show biggest first
                return entityValueMap.get(entity1).compareTo(entityValueMap.get(entity));
            }
        };
    }
}
