package org.coode.cloud.view;

import java.util.HashSet;
import java.util.Set;

import org.coode.cloud.model.AbstractOWLCloudModel;
import org.coode.cloud.model.OWLCloudModel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.inference.ReasonerStatus;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

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
public class IndividualsByInferredRelationCount extends AbstractCloudView<OWLNamedIndividual> {

	private static final long serialVersionUID = -234550577214825167L;

	@Override
    protected OWLCloudModel<OWLNamedIndividual> createModel() {
        return new IndividualsByRelationCountModel(getOWLModelManager());
    }

    @Override
    protected boolean isOWLIndividualView() {
        return true;
    }

    class IndividualsByRelationCountModel extends AbstractOWLCloudModel<OWLNamedIndividual> {

        protected IndividualsByRelationCountModel(OWLModelManager mngr) {
            super(mngr);
        }

        @Override
        public Set<OWLNamedIndividual> getEntities() {
            Set<OWLNamedIndividual> entities = new HashSet<>();
            for (OWLOntology ont : getOWLModelManager().getActiveOntologies()) {
                entities.addAll(ont.getIndividualsInSignature());
            }
            return entities;
        }

        @Override
        public void activeOntologiesChanged(Set<OWLOntology> ontologies) {
        }

        @Override
        protected int getValueForEntity(OWLNamedIndividual entity) {
            int usage = 0;
            OWLReasoner r = getOWLModelManager().getReasoner();
            
            if (getOWLModelManager().getOWLReasonerManager().getReasonerStatus() == ReasonerStatus.INITIALIZED) {
                for (OWLOntology ontology : r.getRootOntology().getImportsClosure()) {
                    for (OWLObjectProperty p : ontology.getObjectPropertiesInSignature()) {
                        usage += r.getObjectPropertyValues(entity, p).getFlattened().size();
                    }
                    for (OWLDataProperty p : ontology.getDataPropertiesInSignature()) {
                        usage += r.getDataPropertyValues(entity, p).size();
                    }
                }
            }
            return usage;
        }
    }
}