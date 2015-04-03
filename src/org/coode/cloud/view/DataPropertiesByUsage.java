package org.coode.cloud.view;

import java.util.HashSet;
import java.util.Set;

import org.coode.cloud.model.AbstractOWLCloudModel;
import org.coode.cloud.model.OWLCloudModel;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

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
public class DataPropertiesByUsage extends AbstractCloudView {

	private static final long serialVersionUID = -4543508379588523946L;

	@Override
    protected OWLCloudModel createModel() {
        return new DataPropertiesByUsage.PropertiesByUsageModel(getOWLModelManager());
    }

    @Override
    protected boolean isOWLDataPropertyView() {
        return true;
    }

    class PropertiesByUsageModel extends AbstractOWLCloudModel<OWLDataProperty> {

        protected PropertiesByUsageModel(OWLModelManager mngr) {
            super(mngr);
        }

        @Override
        public Set<OWLDataProperty> getEntities() {
            Set<OWLDataProperty> props = new HashSet<OWLDataProperty>();
            for (OWLOntology ont : getOWLModelManager().getActiveOntologies()) {
                props.addAll(ont.getDataPropertiesInSignature());
            }
            return props;
        }

        @Override
        public void activeOntologiesChanged(Set<OWLOntology> ontologies) {
        }

        @Override
        protected int getValueForEntity(OWLDataProperty entity) throws OWLException {
            int usage = 0;
            for (OWLOntology ont : getOWLModelManager().getActiveOntologies()) {
                usage += ont.getReferencingAxioms(entity, Imports.EXCLUDED)
                        .size();
            }
            return usage;
        }
    }
}
