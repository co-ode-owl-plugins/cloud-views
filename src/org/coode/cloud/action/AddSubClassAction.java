package org.coode.cloud.action;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.entity.OWLEntityCreationSet;
import org.protege.editor.owl.ui.OWLIcons;
import org.protege.editor.owl.ui.view.OWLSelectionViewAction;
import org.semanticweb.owl.model.*;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
public class AddSubClassAction extends OWLSelectionViewAction {

    private OWLEditorKit eKit;

    public AddSubClassAction(OWLEditorKit eKit) {
        super("Add subclass", OWLIcons.getIcon("class.add.sub.png"));
        this.eKit = eKit;
    }

    public void dispose() {
    }

    public void actionPerformed(ActionEvent actionEvent) {
            OWLClass cls = eKit.getWorkspace().getOWLSelectionModel().getLastSelectedClass();
            if (cls == null) {
                return;
            }
            OWLEntityCreationSet<OWLClass> creationSet = eKit.getWorkspace().createOWLClass();
            if (creationSet == null) {
                return;
            }
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>(creationSet.getOntologyChanges());
            // Add the required change to add the subclass relationship

            final OWLDataFactory df = eKit.getModelManager().getOWLDataFactory();
            OWLAxiom axiom = df.getOWLSubClassAxiom(creationSet.getOWLEntity(), cls);

            changes.add(new AddAxiom(eKit.getModelManager().getActiveOntology(), axiom));

            eKit.getModelManager().applyChanges(changes);

            // Select the new class
            eKit.getWorkspace().getOWLSelectionModel().setSelectedEntity(creationSet.getOWLEntity());
    }

    public void updateState() {
        OWLClass selection = eKit.getWorkspace().getOWLSelectionModel().getLastSelectedClass();
        if (selection == null) {
            setEnabled(false);
        }
        else {
                OWLClass thing = eKit.getModelManager().getOWLDataFactory().getOWLThing();
                setEnabled(!selection.equals(thing));
        }
    }
}
