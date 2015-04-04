package org.coode.cloud.model;

import java.util.List;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;

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
public abstract class AbstractOWLCloudModel<O extends OWLEntity> extends AbstractCloudModel<O> implements OWLCloudModel<O> {

    private OWLModelManager mngr;

    private boolean sync = false; // whether the model updates on ontology changes
    private boolean refreshNeeded = true;
    private boolean dataChanged = true;

    // listen to any changes in the ontology
    private OWLOntologyChangeListener ontChangeListener = new OWLOntologyChangeListener() {
        @Override
        public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
            reload();
        }
    };

    // listen to whether the currently active ontology has changed
    private OWLModelManagerListener activeOntologyListener = new OWLModelManagerListener() {
        @Override
        public void handleChange(OWLModelManagerChangeEvent event) {
            if (event.getType() == EventType.ACTIVE_ONTOLOGY_CHANGED ||
                event.getType() == EventType.ONTOLOGY_VISIBILITY_CHANGED) {
                dataChanged();
                reload();
            }
        }
    };

    protected AbstractOWLCloudModel(OWLModelManager mngr) {
        super();

        this.sync = false;
        this.mngr = mngr;

        // @@TODO change this to a transactionListener when implemented by Matthew
        mngr.addOntologyChangeListener(ontChangeListener);
        mngr.addListener(activeOntologyListener);
    }

    protected final OWLModelManager getOWLModelManager() {
        return mngr;
    }

    @Override
    public final void dataChanged() {
        try {
            activeOntologiesChanged(mngr.getActiveOntologies());
        }
        catch (OWLException e) {
            Logger.getLogger(getClass()).error(e);
        }
    }

    @Override
    protected final int calculateValue(O entity){
        try {
            return getValueForEntity(entity);
        }
        catch (OWLException e) {
            Logger.getLogger(getClass()).error(e);
        }
        return 0;
    }

    protected abstract int getValueForEntity(O entity) throws OWLException;

    @Override
    public String getRendering(O entity) {
        return mngr.getRendering(entity);
    }

    @Override
    public O getEntity(String text) {
        return (O)mngr.getOWLEntityFinder().getOWLEntity(text);
    }

    @Override
    public void dispose() {
        super.dispose();
        mngr.removeOntologyChangeListener(ontChangeListener);
        mngr.removeListener(activeOntologyListener);
    }

    @Override
    protected void reload() {
        if (sync) {
            super.reload();
            refreshNeeded = false;
        }
        else {
            refreshNeeded = true;
        }
    }

    @Override
    public void setSync(boolean synchronise) {
        if (synchronise != sync) {
            sync = synchronise;
            if (sync) { // only reload if the ontology has changed (if reload has been tried)
                if (dataChanged) {
                    handleDataChanged();
                }
                if (refreshNeeded) {
                    reload();
                }
            }
        }
    }

    private void handleDataChanged() {
        if (sync) {
            dataChanged();
            dataChanged = false;
        }
        else {
            dataChanged = true;
        }
    }
}
