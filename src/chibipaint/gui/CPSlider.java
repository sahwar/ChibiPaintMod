/*
 * ChibiPaintMod
 *     Copyright (c) 2012-2014 Sergey Semushin
 *     Copyright (c) 2006-2008 Marc Schefer
 *
 *     This file is part of ChibiPaintMod (previously ChibiPaint).
 *
 *     ChibiPaintMod is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ChibiPaintMod is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.
 */

package chibipaint.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class CPSlider extends JComponent implements MouseListener, MouseMotionListener
{

int value;
private final int valueRange;
int minValue = 0;
private final int maxValue;
String title;

private boolean dragNormal = false;
private boolean dragPrecise = false;
private int dragPreciseX;

boolean centerMode = false;

public CPSlider (int valueRange)
{
  setBackground (Color.white);
  setSize (130, 16);

  this.valueRange = valueRange;
  maxValue = valueRange;
  value = valueRange;

  title = "";

  addMouseListener (this);
  addMouseMotionListener (this);
}

@Override
public void paint (Graphics g)
{
  Dimension d = getSize ();

  g.setColor (Color.white);
  g.fillRect (0, 0, d.width, d.height);

  g.setColor (Color.black);
  if (centerMode)
    {
      if (value >= valueRange / 2)
        {
          g.fillRect (d.width / 2, 0, (value - valueRange / 2) * d.width / valueRange, d.height);
        }
      else
        {
          g.fillRect (value * d.width / valueRange, 0, (valueRange / 2 - value) * d.width / valueRange, d.height);
        }
    }
  else
    {
      g.fillRect (0, 0, value * d.width / valueRange, d.height);
    }

  g.setColor (Color.white);
  g.setXORMode (Color.black);
  g.drawString (title, 2, 14);
}

public void onValueChange ()
{
}

void onFinalValueChange ()
{
}

public void setValue (int value)
{
  this.value = Math.max (minValue, Math.min (maxValue, value));
  onValueChange ();
  repaint ();
}

void mouseSelect (MouseEvent e)
{
  Dimension d = getSize ();

  int x = e.getX ();
  setValue (x * valueRange / d.width);
}

@Override
public void mousePressed (MouseEvent e)
{
  boolean drag = dragNormal || dragPrecise;
  if (!drag && (e.getModifiers () & InputEvent.BUTTON1_MASK) != 0)
    {
      dragNormal = true;
      mouseSelect (e);
    }
  if (!drag && (e.getModifiers () & InputEvent.BUTTON3_MASK) != 0)
    {
      dragPrecise = true;
      dragPreciseX = e.getPoint ().x;
    }
}

@Override
public void mouseDragged (MouseEvent e)
{
  if (dragNormal)
    {
      mouseSelect (e);
    }
  else if (dragPrecise)
    {
      int diff = (e.getPoint ().x - dragPreciseX) / 4;
      if (diff != 0)
        {
          setValue (value + diff);
          dragPreciseX = e.getPoint ().x;
        }
    }
}

@Override
public void mouseReleased (MouseEvent e)
{
  if (dragNormal && (e.getModifiers () & InputEvent.BUTTON1_MASK) != 0)
    {
      dragNormal = false;
      onFinalValueChange ();
    }
  if (dragPrecise && (e.getModifiers () & InputEvent.BUTTON3_MASK) != 0)
    {
      dragPrecise = false;
      onFinalValueChange ();
    }
}

// Unused interface methods
@Override
public void mouseEntered (MouseEvent e)
{
}

@Override
public void mouseExited (MouseEvent e)
{
}

@Override
public void mouseClicked (MouseEvent e)
{
}

@Override
public void mouseMoved (MouseEvent e)
{
}
}
