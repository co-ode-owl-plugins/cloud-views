package org.coode.cloud.test;

import org.coode.cloud.model.AbstractCloudModel;
import org.coode.cloud.ui.CloudSwingComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
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
public class TestCloud {

    public static CloudSwingComponent<String> cloud;

    public static AbstractCloudModel<String> model = new AbstractCloudModel<String>(){

        public void dataChanged() {
        }

        public Set<String> getEntities() {
            System.out.println("TestCloud.getEntities");
            Set<String> strings = new HashSet<String>();
            strings.add("Monkey");
            strings.add("Turn");
            strings.add("DoodahChimps");
            return strings;
        }

        public String getEntity(String text) {
            return text;
        }

        protected int calculateValue(String entity) {
            return entity.length() + 100;
        }
    };

    public static void main(String[] args) {
        cloud = new CloudSwingComponent<String>(model);
        JFrame f = new JFrame("Test Cloud");
        f.getContentPane().setPreferredSize(new Dimension(100, 100));
        f.getContentPane().setLayout(new BorderLayout());
        f.getContentPane().add(new JScrollPane(cloud), BorderLayout.CENTER);
        f.getContentPane().add(new JButton(new AbstractAction("reload"){

        	private static final long serialVersionUID = -17394883027977876L;

			public void actionPerformed(ActionEvent actionEvent) {
                cloud.doLayout(true);
            }
        }), BorderLayout.NORTH);
        f.validate();
        f.setVisible(true);
        f.addWindowListener(new WindowAdapter(){
            public void windowClosed(WindowEvent windowEvent) {
                System.exit(0);
            }
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
    }
}
