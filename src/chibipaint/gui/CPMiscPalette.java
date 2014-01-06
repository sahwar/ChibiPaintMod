/*
	ChibiPaintMod
   Copyright (c) 2012-2013 Sergey Semushin
   Copyright (c) 2006-2008 Marc Schefer

    This file is part of ChibiPaintMod (previously ChibiPaint).

    ChibiPaintMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChibiPaintMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChibiPaintMod. If not, see <http://www.gnu.org/licenses/>.

 */

package chibipaint.gui;

import chibipaint.controller.CPController;

import java.awt.*;

class CPMiscPalette extends CPPalette
{

private final CPIconButton zoomInButton;
private final CPIconButton zoomOutButton;
private final CPIconButton zoom100Button;
private final CPIconButton undoButton;
private final CPIconButton redoButton;
private CPIconButton sendButton = null;

public CPMiscPalette (CPController controller)
{
  super (controller);

  title = "Misc";
  setLayout (new FlowLayout ());

  Component spacer;
  icons = controller.loadImage ("icons.png");
  zoomInButton = addIconButton (13, "CPZoomIn");
  zoomOutButton = addIconButton (14, "CPZoomOut");
  zoom100Button = addIconButton (15, "CPZoom100");
  addSpacer ();
  undoButton = addIconButton (10, "CPUndo");
  redoButton = addIconButton (11, "CPRedo");

  if (controller.isRunningAsApplet ())
    {
      addSpacer ();
      sendButton = addIconButton (12, "CPSend");
    }
}

void setEnabledForTransform (boolean enabled)
{
  undoButton.setEnabled (enabled);
  redoButton.setEnabled (enabled);
  if (sendButton != null)
    sendButton.setEnabled (enabled);
}

}
