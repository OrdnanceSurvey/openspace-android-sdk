/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;

import java.text.ParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Defines a point on the map, as an eastings/northings pair in the OS Coordinate system.
 * @author bblaukopf
 *
 */
public final class GridPoint {
	static final int GRID_WIDTH  =  700000;
	static final int GRID_HEIGHT = 1300000;

	public final double x;
	public final double y;

	private static final String[] NATGRID_LETTERS = {"VWXYZ","QRSTU","LMNOP","FGHJK","ABCDE"};
	
	/**
	 * Construct a GridPoint from eastings and northings
	 * @param xx Eastings
	 * @param yy Northing
	 */
	public GridPoint(double xx, double yy) {
		x = xx;
		y = yy;
	}

	/**
	 * Construct a GridPoint, constrained to be within the bounds of the OS National Grid.
	 * @param x Eastings
	 * @param y Northings
	 * @return the constructed GridPoint
	 */
	static GridPoint clippedToGridBounds(double x, double y) {
		x = Math.max(0, x);
		y = Math.max(0, y);
		x = Math.min(GridPoint.GRID_WIDTH, x);
		y = Math.min(GridPoint.GRID_HEIGHT, y);
		return new GridPoint(x,y);
	}

	GridPoint clippedToGridBounds() {
		// TODO: Is this actually worth it? The extra alloc could be optimized away by a sufficiently clever JIT.
		if (isInBounds()) {
			return this;
		}
		return clippedToGridBounds(x,y);
	}
	
	boolean isInBounds() {
		if (0 <= x && x <= GridPoint.GRID_WIDTH && 0 <= y && y <= GridPoint.GRID_HEIGHT) {
			return true;
		}
		return false;
	}

	double distanceTo(GridPoint other) {
		return Math.hypot(this.x-other.x, this.y-other.y);
	}

	/**
	 * Describe this GridPoint
	 */
	@Override
	public String toString() {
		return "GridPoint(" + toString(-1) + ")";
	};

	/**
	 * Return a String containing a National Grid Reference containing two letters and an even number of digits (e.g. SK35)
	 * @param digits Number of digits to use for eastings and northings. For example, SK35 contains one digit of eastings and northings.
	 * @return OS Grid Reference, as a String
	 */
	public String toString(int digits)
	{
		int e = (int)x;
		int n = (int)y;
		if (digits < 0) {
			return e + "," + n;
		}
		// We can actually handle negative E and N in the lettered case, but that's more effort.
		if (e < 0 || n < 0) { return null; }

		String ret = "";

		// 	The following code doesn't correctly handle e<0 or n<0 due to problems with / and %.
		int big = 500000;
		int small = big/5;
		int firstdig = small/10;

		int es = e/big;
		int ns = n/big;
		e = e % big;
		n = n % big;
		// move to the S square
		es += 2;
		ns += 1;
		if (es > 4 || ns > 4) { return null; }
		ret = ret + NATGRID_LETTERS[ns].charAt(es);

		es = e/small;
		ns = n/small;
		e = e % small;
		n = n % small;
		ret= ret + NATGRID_LETTERS[ns].charAt(es);

		// Only add spaces if there are digits too. This lets us have "zero-figure" grid references, e.g. "SK"
		if (digits > 0)
		{
			ret += ' ';

			for (int dig = firstdig, i = 0; dig != 0 && i < digits; i++, dig /= 10) {
				ret += (e/dig%10);
			}

			ret += ' ';
		
			for (int dig = firstdig, i = 0; dig != 0 && i < digits; i++, dig /= 10) {
				ret += (n/dig%10);
			}
		}

		return ret;
	}
	
	/**
	 * Create a GridPoint from an OS Grid Reference (e.g. SK35)
	 * @param gridRef An OS Grid Reference
	 * @param numDigitsOut If not null, on returns contains the number of digits of eastings and northings data in the first element. For example
	 * parsing a gridRef of "SK35" would, on return, store the value (1) in numDigitsOut[0].
	 * @return The newly constructed GridPoint corresponding to gridRef
	 * @throws ParseException
	 */
	static GridPoint parse(String gridRef, int[] numDigitsOut) throws ParseException
	{
		gridRef = gridRef.toUpperCase(Locale.ENGLISH).trim();
		if(gridRef.length() < 2)
		{
			throw new ParseException("Too short", gridRef.length());
		}
		try
		{
			Scanner s = new Scanner(gridRef);
			double x = s.nextDouble();
			s.next(",");
			double y = s.nextDouble();
			if(!s.hasNext())
			{
				if (numDigitsOut != null) {
					numDigitsOut[0] = -1;
				}
				return new GridPoint(x,y);
			}
		} catch(NoSuchElementException e) {
		}

		int big = 500000;
        int small = big/5;

        // Read the first two digits, converting them into an easting and northing "index".
        char c0 = gridRef.charAt(0);
        char c1 = gridRef.charAt(1);
        int e0 = -1, e1 = -1;
        int n0 = -1, n1 = -1;
        
        for (int n = 0; n < 5; n++)
        {
        	int e = NATGRID_LETTERS[n].indexOf(c0);
        	if(e > -1)
        	{
       			// Offset relative to the S square. This means we immediately discard coordinates south/west of S.
       			e0 = e - 2;
       			n0 = n - 1;
       		}
        	e = NATGRID_LETTERS[n].indexOf(c1);
       		if (e > -1)
        	{
        		e1 = e;
        		n1 = n;
        	}
        }

        if (!(e0 >= 0 && e1 >= 0 && n0 >= 0 && n1 >= 0))
        {
			throw new ParseException("Not on the National Grid", gridRef.length());
        }
        double x = e0 * big + e1 * small;
        double y = n0 * big + n1 * small;
        
        // If it's off the grid, we also want to reject it.
        // We also want to reject coordinates on 700000e or 1300000n, since those would use grid letters off the map.
        // Use the contrapositive to ensure NAN-safety.
        if (!(x < GRID_WIDTH && y < GRID_HEIGHT))
        {
			throw new ParseException("Not on the National Grid", gridRef.length());
        }

        if (gridRef.length() <= 2)
        {
        	// We'll fail to scan any digits below if there are no digits to scan, as with the bare (digitless) "SV".
        	// Handle it here.
        	
			if (numDigitsOut != null) {
				numDigitsOut[0] = 0;
			}
			return new GridPoint(x,y);
        }

        gridRef = gridRef.substring(2).trim();
        Scanner s = new Scanner(gridRef);
        String eStr = null;
        String nStr = null;
        boolean success = false;
        
        try {
        	eStr = s.next("\\d+");
        	// Skip any white space
        	if(s.hasNext()){
        		s.skip("\\s+");
        		nStr = s.next("\\d+");
        	}
            success = !s.hasNext();
        } catch(NoSuchElementException e) {
        }
        
        // We should be "successful". We should also have some digits.
        if (!success || eStr == null)
        {
			throw new ParseException("Failed to parse grid reference", 2);
        }
        int ndigs = eStr.length();
    
        // If we don't have separate northing digits, attempt to split the easting digits in half.
        if (nStr == null)
        {
        	ndigs /= 2;
        	nStr = eStr.substring(ndigs);
        	eStr = eStr.substring(0, ndigs);
        	assert(ndigs == eStr.length());
        	// This should still be true.
        }

        // Handle an odd number of digits (NN123) or an inconsistent number of digits (NN 12 3456).
        // Also handle too few digits (NN), which should be taken care of above, or too many digits (NN 123456 123456).
        if (ndigs != nStr.length() || ndigs < 1 || ndigs > 5)
        {
			throw new ParseException("Invalid number of digits in grid reference", 2);
        }
        
        x += small/Math.pow(10, ndigs) * Integer.valueOf(eStr);
        y += small/Math.pow(10, ndigs) * Integer.valueOf(nStr);;

		if (numDigitsOut != null) {
			numDigitsOut[0] = ndigs;
		}
		return new GridPoint(x,y);
	}
}
