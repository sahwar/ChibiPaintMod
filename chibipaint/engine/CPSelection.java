package chibipaint.engine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Stack;

import chibipaint.gui.CPCanvas;
import chibipaint.util.CPRect;

public class CPSelection extends CPGreyBmp {
	byte[] markerArray;
	private float initialDashPiece = 0.0f;
	private boolean initialDashDrawn = false;
	private boolean dashDrawn;
	private ArrayList <PixelSingleLine> CurPixelLines;
	private boolean invalidateSelection = true;
	int minX, minY, maxX, maxY;

	public CPSelection(int width, int height) {
		super(width, height);
		markerArray = new byte[(width + 1) * (height + 1)];
		Arrays.fill (data, (byte) 0);;
	}

	public void RaiseInitialDash ()
	{
		initialDashPiece += 0.1;
		if (initialDashPiece >= 1.0)
		{
			initialDashDrawn = !initialDashDrawn;
			initialDashPiece -= 1.0;
		}
	}

	public CPSelection(CPSelection original) {
		super(original);
		System.arraycopy (original.markerArray, 0, markerArray, 0, width * height);
	}

	public void AddToSelection (CPSelection otherSelection)
	{
		for (int i = 0; i < height * width; i++)
		{
			if ((data[i] & 0xff) < (otherSelection.data[i] & 0xff))
				data[i] = otherSelection.data[i];
		}
		invalidateSelection = true;
	}


	public void SubtractFromSelection (CPSelection otherSelection)
	{
		for (int i = 0; i < height * width; i++)
		{
			int difference = data[i] & 0xff - otherSelection.data[i] & 0xff;
			data[i] = difference < 0 ? 0 : (byte) difference;
		}
		invalidateSelection = true;
	}


	public void IntersectWithSelection (CPSelection otherSelection)
	{
		for (int i = 0; i < height * width; i++)
		{
			data[i] = (byte) Math.min (data[i] & 0xff, otherSelection.data[i] & 0xff);
		}
		invalidateSelection = true;
	}

	public void makeRectangularSelection (CPRect rect)
	{
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				if ((i >= rect.left) && (i < rect.right) &&
						(j >= rect.top) && (j < rect.bottom)
						)
					data[j * width + i] = (byte) 0xff;
				else
					data[j * width + i] = 0;
				invalidateSelection = true;
				/*
				if ((i - 100) * (i - 100) + (j - 100) * (j - 100) < 5000)
					data[j * width + i] = (byte) 0xff;
				else
					data[j * width + i] = 0;
				 */
			}
	}

	private boolean getIsActive (int i, int j)
	{
		if (isInside (i, j))
			return getIsActiveInBounds (i, j);
		else
			return false;
	}

	private boolean getIsActiveInBounds (int i, int j)
	{
		return (data[j * width + i] != 0);
	}

	private boolean isActive (PixelCoords px)
	{
		if (isInside (px.x, px.y))
			return (data [px.y * width + px.x] != 0);
		else
			return false;
	}

	private int isMarked (PixelCoords px)
	{
		if (isInside (px.x, px.y))
			return (markerArray [px.y * width + px.x]);
		else
			return 15;
	}

	private static void drawLine (Graphics2D g2d, CPCanvas canvas, float x1, float y1, float x2, float y2)
	{
		Point2D.Float p1 = canvas.coordToDisplay (new Point2D.Float(x1, y1));
		Point2D.Float p2 = canvas.coordToDisplay (new Point2D.Float(x2, y2));
		g2d.draw(new Line2D.Float(p1, p2));
	}

	// Thanks to http://www.java2s.com/Code/Java/2D-Graphics-GUI/CanceltheeffectsofthezoomonaparticularStroke.htm
	public static Stroke getInvertedZoomedStroke(Stroke stroke, double zoom)
	{
		if (stroke == null || !(stroke instanceof BasicStroke )) return stroke;

		BasicStroke bs = (BasicStroke)stroke;
		float[] dashArray = bs.getDashArray();

		float[] newDashArray = null;
		if (dashArray != null)
		{
			newDashArray = new float[dashArray.length];
			for (int i = 0; i < newDashArray.length; ++i)
			{
				newDashArray[i] = (float) (dashArray[i] / zoom);
			}
		}

		BasicStroke newStroke = new BasicStroke(
				(float) (bs.getLineWidth() / zoom),
				bs.getEndCap (),
				bs.getLineJoin (),
				bs.getMiterLimit (),
				//(float)(bs.getMiterLimit() / zoom),
				newDashArray,
				(float)(bs.getDashPhase() / zoom)
				);
		return newStroke;
	}


	int[][] Mv = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
	int[][] Corners = {{1, 1}, {0, 1}, {0, 0}, {1, 0}};
	int[] OppositeDirection = {2, 3, 0, 1};
	int[] PowersOf2 = {1, 2, 4, 8};

	class PixelCoords implements Comparable<PixelCoords>
	{
		int x,y;
		PixelCoords (int xArg, int yArg)
		{
			x = xArg;
			y = yArg;
		}

		public PixelCoords(PixelCoords point) {
			x = point.x;
			y = point.y;
		}

		PixelCoords left ()
		{
			return new PixelCoords (x - 1, y);
		}

		PixelCoords right ()
		{
			return new PixelCoords (x + 1, y);
		}

		PixelCoords up ()
		{
			return new PixelCoords (x, y - 1);
		}

		PixelCoords down ()
		{
			return new PixelCoords (x, y + 1);
		}

		PixelCoords MoveByMv (int num)
		{
			return new PixelCoords (x + Mv[num % 4][0], y + Mv[num % 4][1]);
		}

		PixelCoords MoveToCorner (int num)
		{
			return new PixelCoords (x + Corners[num % 4][0], y + Corners[num % 4][1]);
		}

		@Override
		public int compareTo(PixelCoords o) {
			if (x != o.x)
				return (x - o.x);
			else
				return (o.y - y);
		}


	}

	class Lump extends ArrayList<PixelCoords>
	{
	}

	class PixelSingleLine extends ArrayList<PixelCoords>
	{
		boolean backwards = false;
	}

	static int directionsNum = 4;
	enum Directions
	{
		Right,
		Down,
		Left,
		Top,
	}


	int GetNextDirectionNum (PixelCoords currentPoint ,PixelCoords prevPoint)
	{
		int result = -1;
		for (int i = 0; i < directionsNum; i++)
			if (currentPoint.x + Corners[i][0] == prevPoint .x &&
			currentPoint.y + Corners[i][1] == prevPoint.y)
			{
				result = i;
				break;
			}
		return result;
	}

	public Rectangle GetBoundingBox (CPCanvas canvas)
	{
		Point2D.Float firstPoint = canvas.coordToDisplay (new Point2D.Float(minX, minY));
		Point2D.Float secondPoint = canvas.coordToDisplay (new Point2D.Float(maxX, maxY));
		return new Rectangle ((int) firstPoint.x, (int) firstPoint.y, (int) Math.ceil(secondPoint.x) - (int) (firstPoint.x) + 2, (int) Math.ceil(secondPoint.y) - (int) firstPoint.y + 2);
	}

	void convertLumpToPixelSingleLines (ArrayList<PixelSingleLine> pixelLinesTarget, Lump x)
	{
        // TODO: Rewrite using scanline technique
		int fillMode = 1;
		PixelCoords startingPoint = Collections.min (x);
		PixelSingleLine sL = new PixelSingleLine ();
		sL.add (startingPoint.right ().down ());
		do
		{
			PixelCoords currentPoint = new PixelCoords (startingPoint);
			boolean Finished = false;
			do
			{
				minX = Math.min (minX, currentPoint.x);
				maxX = Math.max (maxX, currentPoint.x);
				minY = Math.min (minY, currentPoint.y);
				maxY = Math.max (maxY, currentPoint.y);
				int scanningDirection = GetNextDirectionNum (currentPoint, sL.get (sL.size () - 1));
				if (scanningDirection == -1)
					break;
				for (int i = 1; i < directionsNum; i++)
				{
					scanningDirection = (scanningDirection + 1) % 4;
					if (isActive (currentPoint.MoveByMv(scanningDirection)) == (fillMode == 0))
					{
						if (fillMode == 1)
							markerArray[currentPoint.y * width + currentPoint.x] |= PowersOf2[scanningDirection];
						else
							markerArray[(currentPoint.y + Mv[scanningDirection][1]) * width + currentPoint.x + Mv[scanningDirection][0]] |= (PowersOf2[OppositeDirection [scanningDirection]]);
						if (currentPoint.MoveToCorner(scanningDirection).compareTo (sL.get (0)) != 0)
							sL.add (currentPoint.MoveToCorner(scanningDirection));
						else
						{
							Finished = true;
							break;
						}
					}
					else
					{
						PixelCoords nextPoint = currentPoint.MoveByMv(scanningDirection);
						PixelCoords pointAfterNext = nextPoint.MoveByMv(GetNextDirectionNum (nextPoint, sL.get (sL.size () - 1)) + 1);
						if (isActive (pointAfterNext) == (fillMode == 0))
							currentPoint = nextPoint;
						else
							currentPoint = pointAfterNext;
						break;
					}
				}
				if (Finished)
					break;

			}
			while(true);
			if (fillMode == 0)
				Collections.reverse (sL);
			pixelLinesTarget.add(sL);
			fillMode = 0;
			boolean holeFound = false;
			// Checking for inner holes
			for (int i = 0; i < x.size (); i++)
			{
				for (int j = 0; j < directionsNum; j++)
				{
					PixelCoords px = x.get (i).MoveByMv(j);
					if (!isActive (px) && ((isMarked(x.get(i)) & PowersOf2[j]) == 0))
					{
						startingPoint = px;
						sL = new PixelSingleLine ();
						for (int k = 0; k < directionsNum; k++)
						{
							if (isActive (startingPoint.MoveByMv(k)))
							{
								sL.add(startingPoint.MoveToCorner((k + 3) % 4));
								break;
							}
						}

						holeFound = true;
						break;
					}
				}
				if (holeFound)
					break;
			}
			if (!holeFound)
				break;
		}
		while (true);
	}

	public void drawItself(Graphics2D g2d, CPCanvas canvas) {
		/*
		g2d.setXORMode(new Color(0x808080));
		Stroke stroke = g2d.getStroke();
		// float dashSize = 1.f / canvas.getZoom();
		g2d.setStroke (new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[] { 8.0f, 8.0f}, 0f));
		 */

		if (invalidateSelection)
		{
			minX = width;
			maxX = 0;
			minY = height;
			maxY = 0;
			// First step: we're dividing everything on separate 4-connected regions
			ArrayList<Lump> lumps = new ArrayList<Lump> ();
			Arrays.fill (markerArray, (byte) 0);
			for (int i = 0; i < width; i++)
				for (int j = 0; j < height; j++)
				{
					if (getIsActive (i, j) && markerArray[j * width + i] == 0) // If something active and not marked found then we're making Depth-first search
					{
						lumps.add (MakeLumpByScanLines (i, j));
					}
				}


			Arrays.fill (markerArray, (byte) 0);
			// Now we've got our pixels lumped according to 4-connections, now let's build their boundary
			// They will be converted to pixel single lines class which is basically - lines to be drawn.
			CurPixelLines = new ArrayList <PixelSingleLine> ();
			for (int i = 0; i < lumps.size (); i++)
			{
				convertLumpToPixelSingleLines (CurPixelLines, lumps.get(i));
			}
			invalidateSelection = false;
		}


		Color prevColor = g2d.getColor ();
		float defaultDashLength = 8.2f / canvas.getZoom ();
		float dashLength = defaultDashLength;

		for (int i = 0; i < CurPixelLines.size (); i++)
		{
			/*
			float newDashLength = defaultDashLength;

			int NumberOfDashes = (int) Math.ceil(((CurPixelLines.get(i).size ()) / dashLength));
			if (NumberOfDashes != 0)
				newDashLength = CurPixelLines.get(i).size () / NumberOfDashes;

			if (Math.abs (newDashLength) > Float.MIN_VALUE)
				dashLength = newDashLength;
			else
				dashLength = defaultDashLength;
			 */

			dashDrawn = initialDashDrawn;
			float dashCurLength = initialDashPiece * dashLength;
			for (int j = 0; j < CurPixelLines.get(i).size (); j++)
			{
				PixelSingleLine CurPSL = CurPixelLines.get(i);
				PixelCoords FirstPoint = CurPSL.get (j);
				PixelCoords SecondPoint = CurPSL.get ((j + 1) % CurPixelLines.get(i).size ());
				if (dashLength - dashCurLength > 1.0f)
				{
					if (dashDrawn)
						g2d.setColor (Color.black);
					else
						g2d.setColor (Color.white);

					drawLine (g2d, canvas, FirstPoint.x, FirstPoint.y, SecondPoint.x, SecondPoint.y);
					dashCurLength += 1.0f;
				}

				else
				{
					float segmentStart = 0.0f;
					boolean leaveCycle = false;
					while (true)
					{
						float segmentEnd = segmentStart + (dashLength - dashCurLength);
						if (segmentEnd > 1.0f)
						{
							dashCurLength = 1.0f - segmentStart;
							segmentEnd = 1.0f;
							leaveCycle = true;
						}
						if (dashDrawn)
							g2d.setColor (Color.black);
						else
							g2d.setColor (Color.white);

						drawLine (g2d, canvas, FirstPoint.x + (SecondPoint.x - FirstPoint.x) * (segmentStart) , FirstPoint.y + (SecondPoint.y - FirstPoint.y) * (segmentStart),
								FirstPoint.x + (SecondPoint.x - FirstPoint.x) * (segmentEnd),
								FirstPoint.y + (SecondPoint.y - FirstPoint.y) * (segmentEnd));


						if (leaveCycle)
							break;
						segmentStart = segmentEnd;
						dashDrawn = !dashDrawn;
						dashCurLength = 0.0f;
					}
				}
			}
		}

		g2d.setColor (prevColor);
		// Preparing
		// And then the MAGICS!
		// g2d.setStroke(stroke);
	}

    private Lump MakeLumpByScanLines (int x, int y) {
        Stack <PixelCoords> S = new Stack <PixelCoords> ();
        PixelCoords newPx = new PixelCoords (x, y);
        S.push (newPx);
        Lump ResultingLump = new Lump ();
        while (!S.empty())
        {
            PixelCoords px = S.pop();
                // Skipping all the stuff we should add into our lump from the left
                while (px.x >= 0 && getIsActiveInBounds (px.x, px.y))
                    px.x--;
                px.x++; // Now we find the left side of this part
                // careful: px.x is used as an iterator and px.y is constant
                boolean spanTop = false;
                boolean spanBottom = false;

                while (px.x < width && getIsActiveInBounds (px.x, px.y) && markerArray [px.y * width + px.x] == 0)
                {
                    ResultingLump.add (new PixelCoords (px));
                    markerArray [px.y * width + px.x] = 1;
                    if(!spanTop && px.y > 0 && getIsActiveInBounds (px.x, px.y - 1) && markerArray [px.y * width - width + px.x] == 0)
                    {
                        newPx = new PixelCoords (px.x, px.y - 1);
                        S.push(newPx);
                        spanTop = true;
                    }
                    else if (spanTop && px.y > 0 && (!getIsActiveInBounds (px.x, px.y - 1) || markerArray [px.y * width - width + px.x] == 1))
                    {
                        spanTop = false;
                    }
                    if(!spanBottom && px.y < height - 1 &&  getIsActiveInBounds (px.x, px.y + 1) && markerArray [px.y * width + width + px.x] == 0)
                    {
                        newPx = new PixelCoords (px.x, px.y + 1);
                        S.push(newPx);
                        spanBottom = true;
                    }
                    else if(spanBottom && px.y < height - 1 && (!getIsActiveInBounds (px.x, px.y + 1) || markerArray [px.y * width + width + px.x] == 1))
                    {
                        spanBottom = false;
                    }
                    px.x++;
                }
            }
        return ResultingLump;
    }

	private Lump MakeLump(int x, int y) {
		Stack <PixelCoords> S = new Stack <PixelCoords> ();
		PixelCoords newPx = new PixelCoords (x, y);
		S.push (newPx);
		Lump ResultingLump = new Lump ();
		while (!S.empty())
		{
			PixelCoords px = S.pop();
			markerArray[px.y * width + px.x] = 1;
			ResultingLump.add (px);
			for (int i = 0; i < Mv.length; i++)
			{
				int newY = px.y + Mv[i][1];
				int newX = px.x + Mv[i][0];
				if (!isInside (newX, newY))
					continue;
				if (markerArray [newY * width + newX] == 0 && getIsActiveInBounds (newX, newY))
					S.push (new PixelCoords (newX, newY));
			}
		}
		return ResultingLump;
	}
}