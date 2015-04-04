package org.coode.cloud.view;

import org.coode.cloud.model.AbstractClassCloudModel;
import org.coode.cloud.model.OWLCloudModel;
import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;

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
public class ClassesByDepth extends AbstractClassCloudView {

	private static final long serialVersionUID = 4523839568986281278L;

	protected OWLCloudModel createModel() {
        return new ClassesByDepth.ClassesByDepthModel(getOWLModelManager());
    }

    class ClassesByDepthModel extends AbstractClassCloudModel {

        public ClassesByDepthModel(OWLModelManager mngr) {
            super(mngr);
        }

        protected int getValueForEntity(OWLClass entity) throws OWLException {
            Set<OWLClass> ancestors = getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getAncestors(entity);
            return ancestors.size();
        }

        public void activeOntologiesChanged(Set<OWLOntology> activeOntologies) throws OWLException {
        }
    }
}
