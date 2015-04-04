package org.coode.cloud.ui;

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
 * A modified version of FlowLayout that allows containers using this
 * Layout to behave in a reasonable manner when placed inside a
 * JScrollPane

 * @author Babu Kalakrishnan
 */
public class ModifiedFlowLayout extends FlowLayout
{

	private static final long serialVersionUID = 2867348977944303355L;
	
	private static final int FUDGE_FACTOR = 20;

    public ModifiedFlowLayout()
    {
        super();
    }

    public ModifiedFlowLayout(int align)
    {
        super(align);
    }

    public ModifiedFlowLayout(int align, int hgap, int vgap)
    {
        super(align, hgap, vgap);
    }

    public Dimension minimumLayoutSize(Container target)
    {
        return computeSize(target, false);
    }

    public Dimension preferredLayoutSize(Container target)
    {
        return computeSize(target, true);
    }

    private Dimension computeSize(Container target, boolean minimum)
    {
        synchronized (target.getTreeLock())
        {
            int hgap = getHgap();
            int vgap = getVgap();
            int w = target.getWidth();

// Let this behave like a regular FlowLayout (single row)
// if the container hasn't been assigned any size yet
            if (w == 0)
                w = Integer.MAX_VALUE;

            Insets insets = target.getInsets();
            if (insets == null)
                insets = new Insets(0, 0, 0, 0);
            int reqdWidth = 0;

            int maxwidth = w - (insets.left + insets.right + hgap * 2);
            int n = target.getComponentCount();
            int x = 0;
            int y = insets.top;
            int rowHeight = 0;

            for (int i = 0; i < n; i++)
            {
                Component c = target.getComponent(i);
                if (c.isVisible())
                {
                    Dimension d =
                            minimum ? c.getMinimumSize() :
                            c.getPreferredSize();
                    if ((x == 0) || ((x + d.width) <= maxwidth))
                    {
                        if (x > 0)
                        {
                            x += hgap;
                        }
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else
                    {
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                    reqdWidth = Math.max(reqdWidth, x);
                }
            }
            y += rowHeight + FUDGE_FACTOR;
            return new Dimension(reqdWidth+insets.left+insets.right, y);
        }
    }
}