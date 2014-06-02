package uk.co.ordnancesurvey.android.maps;

import junit.framework.TestCase;

public final class BasicMapProjectionTest extends TestCase {
	static double dms(double d, double m, double s) {
		return d + m/60 + s/3600;
	}

	// This tests against the worked example in http://www.ordnancesurvey.co.uk/oswebsite/gps/docs/convertingcoordinatesEN.pdf
	public static void testLatLngToEastNorth() {
		double[] temp = new double[3];
		BasicMapProjection.latLngToEastNorth(6377563.396, 6356256.910, -100000, 400000, 0.9996012717, Math.toRadians(49), Math.toRadians(-2), dms(52,39,27.2531), dms(1,43,4.5177), temp);

		assertEquals(651409.903, temp[0], 0.001);
		assertEquals(313177.270, temp[1], 0.001);
	}

	// This tests against the worked example in http://www.ordnancesurvey.co.uk/oswebsite/gps/docs/convertingcoordinatesEN.pdf
	public static void testEastNorthToLatLng() {
		double[] temp = new double[3];
		BasicMapProjection.eastNorthToLatLng(6377563.396, 6356256.910, -100000, 400000, 0.9996012717, Math.toRadians(49), Math.toRadians(-2), 651409.903, 313177.270, temp);

		assertEquals(dms(52,39,27.2531), temp[0], dms(0,0,0.0001));
		assertEquals(dms(1,43,4.5177), temp[1], dms(0,0,0.0001));
	}

}
