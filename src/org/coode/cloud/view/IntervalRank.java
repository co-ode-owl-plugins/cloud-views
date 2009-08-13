package org.coode.cloud.view;

import org.coode.cloud.model.AbstractClassCloudModel;
import org.coode.cloud.model.OWLCloudModel;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;

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
 * Created by IntelliJ IDEA.
 * User: candidasa
 * Date: Sep 4, 2006
 * Time: 4:04:16 PM
 * Calculates the Interval Rank of all classes in an ontology
 * Algorithm: from Ralph Freese (2004) automated Lattice Drawing in Concept Lattices, ICFCA 04, LNAI v. 2961, pp. 112-127
 * <p/>
 * rank(a) = height(a) - depth (a) + M
 */
public class IntervalRank extends AbstractClassCloudView {

    protected OWLCloudModel createModel() {
        return new IntervalRankModel(getOWLModelManager());
    }


    class IntervalRankModel extends AbstractClassCloudModel {
        private int m;  //maximal height
        private int maxRank = 0;    //maximal IntervalRank
        protected OWLObjectHierarchyProvider provider;
        protected HashMap<OWLClass, Integer> heights;
        protected HashMap<OWLClass, Integer> depths;
        protected HashMap<OWLClass, Integer> intervalRanks;
        LinkedList<OWLClass>[] reverseIntervalRanks;    //list of all class in each

        HashSet<OWLClass> alreadyReturned = new HashSet<OWLClass>();  //list of all classes already returned to the current user

        protected IntervalRankModel(OWLModelManager mngr) {
            super(mngr);
        }

        public Set<OWLClass> getEntities() {
            return intervalRanks.keySet();
        }

        public void activeOntologiesChanged(Set<OWLOntology> ontologies) throws OWLException {
            provider = getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            heights =       new HashMap<OWLClass, Integer>();
            depths =        new HashMap<OWLClass, Integer>();
            intervalRanks = new HashMap<OWLClass, Integer>();
            computeHeights(getOWLModelManager().getActiveOntology());   //build data structure of heights and depths
            computeIntervalRank();  //calcuates the interval rank of each class in the ontology
        }

        protected int getValueForEntity(OWLClass entity) throws OWLException {
            return intervalRanks.get(entity);
        }

        /**
         * recursively descend into the class hierarchy, counting height as we go down (and recording it as we descend)
         * also, as we return, return the maximal number of subclasses (depth) as we go up. Not the maximal height = M.
         */
        private void computeHeights(OWLOntology ontology) throws OWLException {
            OWLClass thing = getOWLModelManager().getOWLDataFactory().getOWLThing();

            recursiveHeights(thing, -1);
        }

        private int recursiveHeights(OWLClass cls, int height) {
            int currentHeight = height + 1;
            if (currentHeight > m) m = currentHeight;
            int currentDepth = 0;   //leaf class (depth = 0) by default, unless we learn otherwise from the children iterator below

            Set<OWLClass> children = provider.getChildren(cls);   //gets the direct children of the given class
            if (children.size() > 0) {
                for (OWLClass childClass : children) {
                    int depth = recursiveHeights(childClass, currentHeight);
                    if (depth > currentDepth)
                        currentDepth = depth; //keep the largest number of children deep this class has
                }

                currentDepth++; //increase the depth by one, since this is the superclass of the maximal depth of the children
            }

            //store the two results
            heights.put(cls, currentHeight);
            depths.put(cls, currentDepth);

            return currentDepth;
        }

        /**
         * Does the simple calculation to generate the IntervalRanks for each class in the ontology
         */
        private void computeIntervalRank() {
            Iterator<OWLClass> recordedClassesIterator = heights.keySet().iterator();

            while (recordedClassesIterator.hasNext()) {
                OWLClass checkClass = recordedClassesIterator.next();
                Integer rank = heights.get(checkClass) - depths.get(checkClass) + m;    //algorithm for computing intervalRank
                if (rank > maxRank) maxRank = rank;
                intervalRanks.put(checkClass, rank);
            }

            //reverse lookup data structure for interval rank
            reverseIntervalRanks = new LinkedList[maxRank + 1];
            for (int i = 0; i < reverseIntervalRanks.length; i++) {
                reverseIntervalRanks[i] = new LinkedList<OWLClass>(); //init all lists
            }

            //restart iterator and build reverse data structure
            recordedClassesIterator = heights.keySet().iterator();
            while (recordedClassesIterator.hasNext()) {
                OWLClass owlClass = recordedClassesIterator.next();
                reverseIntervalRanks[intervalRanks.get(owlClass)].add(owlClass);
            }

        }

        /**
         * returns the maximum intervalRank of the current ontology
         */
        public int getMaximumRank() {
            return maxRank;
        }

        /**
         * returns a listing of all the different rank levels and how many classes there are in each.
         * This can later be used to draw an Excel graph of the distribution profile of each evaluated ontology.
         */
        public int[] getOntologyRankDistributionProfile() {
            int[] rankDistribution = new int[reverseIntervalRanks.length];

            for (int i = 0; i < reverseIntervalRanks.length; i++) {
                LinkedList<OWLClass> reverseIntervalRank = reverseIntervalRanks[i];
                rankDistribution[i] = reverseIntervalRank.size();
            }

            return rankDistribution;
        }

        /**
         * Returns the first class with the given rank or lower, once a class has been returned once it is not
         * returned again until the resetNextClassCount method is called.
         * Returns null if such a class does not exist.
         */
        public OWLClass getNextClassByMaximumRank(int maximumRank) {
            OWLClass returnedClass = null;

            maximumRank++;  //just for first run of while loop

            while (returnedClass == null && maximumRank > 0) {
                maximumRank--;
                Iterator<OWLClass> currentRankIterator = reverseIntervalRanks[maximumRank].iterator();

                while (currentRankIterator.hasNext()) {
                    returnedClass = currentRankIterator.next();
                    if (!alreadyReturned.contains(returnedClass)) break;
                    returnedClass = null;
                }
            }

            if (returnedClass != null) alreadyReturned.add(returnedClass);
            return returnedClass;   //returns null if nothing is found
        }

        /**
         * resets the count of classes already returned by getNextClassByMaximumRank for a different simulated user
         */
        public void resetNextClassCount() {
            alreadyReturned.clear();
        }
    }
}

