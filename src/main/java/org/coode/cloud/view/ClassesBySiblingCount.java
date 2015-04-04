package org.coode.cloud.view;

import java.util.HashSet;
import java.util.Set;

import org.coode.cloud.model.AbstractClassCloudModel;
import org.coode.cloud.model.OWLCloudModel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

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
public class ClassesBySiblingCount extends AbstractClassCloudView {

	private static final long serialVersionUID = 4293731689667274587L;

	@Override
    protected OWLCloudModel<OWLClass> createModel() {
        return new ClassesBySiblingCount.ClassesBySiblingCountModel(getOWLModelManager());
    }

    class ClassesBySiblingCountModel extends AbstractClassCloudModel {

        protected ClassesBySiblingCountModel(OWLModelManager mngr) {
            super(mngr);
        }

        @Override
        protected int getValueForEntity(OWLClass entity) {
            Set<OWLClass> siblings = new HashSet<>();
            OWLObjectHierarchyProvider<OWLClass> hierarchyProvider = getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            for (OWLClass parent : hierarchyProvider.getParents(entity)) {
                siblings.addAll(hierarchyProvider.getChildren(parent));
            }
            return siblings.size();
        }

        @Override
        public void activeOntologiesChanged(Set<OWLOntology> activeOntologies) {
        }
    }
}
