// TEST_COORDINATES[] are © Crown copyright 2002. All rights reserved.

package uk.co.ordnancesurvey.android.maps.test;

import android.location.Location;
import android.util.Log;
import uk.co.ordnancesurvey.android.maps.GridPoint;
import uk.co.ordnancesurvey.android.maps.MapProjection;
import junit.framework.TestCase;

public class MapProjectionTest extends TestCase {
	private final String TAG = "MapProjectionTest";

	public void testLatLngToOSGridPoint() {
		MapProjection projection = MapProjection.getDefault();
		double sumX = 0;
		double sumY = 0;
		double sumXX = 0;
		double sumYY = 0;
		double sumDSq = 0;
		double maxDSq = 0;
		int n = TEST_COORDINATES.length;
		for (int i = 0; i < n; i++) {
			GridPoint p = projection.toGridPoint(TEST_COORDINATES[i].lat, TEST_COORDINATES[i].lng);
			double diffX = p.x - TEST_COORDINATES[i].gp.x;
			double diffY = p.y - TEST_COORDINATES[i].gp.y;
			assertEquals("E error", 0.0, diffX, 6);
			assertEquals("N error", 0.0, diffY, 6);
			double diffSq = diffX * diffX + diffY * diffY;
			sumDSq += diffSq;
			maxDSq = Math.max(maxDSq, diffSq);
			sumX += diffX;
			sumY += diffY;
			sumXX += diffX * diffX;
			sumYY += diffY * diffY;
			// NSLog(@"Station %s diff (%g m, %g m)", TEST_COORDINATES[i].station, diffX, diffY);
		}
		double avgX = sumX / n;
		double avgY = sumY / n;
		double sdX = Math.sqrt((sumXX - n * avgX * avgX) / (n - 1));
		double sdY = Math.sqrt((sumYY - n * avgY * avgY) / (n - 1));
		double rmsD = Math.sqrt(sumDSq/n);
		Log.v(TAG, String.format("Average offset (%g±%g m, %g±%g m)", avgX, sdX, avgY, sdY));
		Log.v(TAG, String.format("RMS error: %g m", rmsD));
		Log.v(TAG, String.format("Maximum squared error: %g m^2", maxDSq));
		// TODO: This is a little worse than the iOS code.
		assertEquals("E error avg", 0.0, avgX, 0.3);
		assertEquals("N error avg", 0.0, avgY, 0.1);
		assertEquals("E error sd", 0.0, sdX, 1.3);
		assertEquals("N error sd", 0.0, sdY, 1.8);
		assertEquals("RMS error", 0.0, rmsD, 2.2);
		// Maximum error is a bit poor (sqrt(25) = 5m)
		assertEquals("Maximum squared error", 0.0, maxDSq, 25);
	}

	public void testOSGridPointToLatLngToGridPoint()
	{
		MapProjection projection = MapProjection.getDefault();
		double[] temp = new double[2];
		// Round-trip test.
		double sumX = 0;
		double sumY = 0;
		double sumXX = 0;
		double sumYY = 0;
		double maxoffsetsq = 0;
		int n = TEST_COORDINATES.length;
		for (int i = 0; i < n; i++)
		{
			projection.fromGridPoint(TEST_COORDINATES[i].gp, temp);
			double latitude = temp[0];
			double longitude = temp[1];

			GridPoint p = projection.toGridPoint(latitude, longitude);

			double diffX = p.x - TEST_COORDINATES[i].gp.x;
			double diffY = p.y - TEST_COORDINATES[i].gp.y;

			assertEquals("E error", 0.0, diffX, 0.005);
			assertEquals("N error", 0.0, diffY, 0.05);
			maxoffsetsq = Math.max(maxoffsetsq, diffX*diffX+diffY*diffY);
			sumX += diffX;
			sumY += diffY;
			sumXX += diffX*diffX;
			sumYY += diffY*diffY;
		}
		double avgX = sumX/n;
		double avgY = sumY/n;
		double sdX = Math.sqrt((sumXX-n*avgX*avgX)/(n-1));
		double sdY = Math.sqrt((sumYY-n*avgY*avgY)/(n-1));

		Log.v(TAG, String.format("Average offset (%g±%g m, %g±%g m)", avgX, sdX, avgY, sdY));
		Log.v(TAG, String.format("Maximum squared error: %g m^2", maxoffsetsq));

		// TODO: These appear better than Proj4 (because the test bounds have not been tightened?)
		assertEquals("E error avg", 0.0, avgX, 0.004);
		assertEquals("N error avg", 0.0, avgY, 0.0004);
		assertEquals("E error sd", 0.0, sdX, 0.0007);
		assertEquals("N error sd", 0.0, sdY, 0.002);
		assertEquals("Maximum squared error", 0.0, maxoffsetsq, 0.0003);
	}

	public void testOSGridPointToLatLng()
	{
		MapProjection projection = MapProjection.getDefault();
		double[] temp = new double[2];
		float[] temp2 = new float[1];

		double sumX = 0;
		double sumY = 0;
		double sumXX = 0;
		double sumYY = 0;
		double sumD = 0;
		double sumDD = 0;

		int n = TEST_COORDINATES.length;
		for (int i = 0; i < n; i++)
		{
			projection.fromGridPoint(TEST_COORDINATES[i].gp, temp);
			double latitude = temp[0];
			double longitude = temp[1];

			double diffX = longitude - TEST_COORDINATES[i].lng;
			double diffY = latitude - TEST_COORDINATES[i].lat;
			Location.distanceBetween(TEST_COORDINATES[i].lat, TEST_COORDINATES[i].lng, latitude, longitude, temp2);
			double distM = temp2[0];

			// TODO: This is somehow better than Proj4 (5.5m)
			assertEquals("Error distance", 0, distM, 5.5);

			sumX += diffX;
			sumY += diffY;
			sumXX += diffX*diffX;
			sumYY += diffY*diffY;

			sumD += distM;
			sumDD += distM*distM;
			//NSLog(@"Station %s diff %g °N %g °E", TEST_COORDINATES[i].station, diffX, diffY);
		}
		double avgX = sumX/n;
		double avgY = sumY/n;
		double sdX = Math.sqrt((sumXX-n*avgX*avgX)/(n-1));
		double sdY = Math.sqrt((sumYY-n*avgY*avgY)/(n-1));
		double avgD= sumD/n;
		double rmsD = Math.sqrt(sumDD/n);

		Log.v(TAG, String.format("Average offset %g±%g °N %g±%g °E", avgX, sdX, avgY, sdY));
		Log.v(TAG, String.format("Average distance %g m, RMS distance %g m", avgD, rmsD));

		assertEquals("avg dist", 0.0, avgD, 2.0);
		assertEquals("rms dist", 0.0, rmsD, 2.2);
	}

	public void testRoundTripConversion()
	{
		MapProjection projection = MapProjection.getDefault();
		double[] temp = new double[2];

		int n = TEST_COORDINATES.length;
		for (int i = 0; i < n; i++)
		{
			GridPoint gp = TEST_COORDINATES[i].gp;
			projection.fromGridPoint(gp, temp);
			double latitude = temp[0];
			double longitude = temp[1];

			GridPoint gp2 = projection.toGridPoint(latitude, longitude);
			// Proj4 is somewhat better (0.001, 0.001)
			assertEquals("Error E", gp.x, gp2.x, 0.005);
			assertEquals("Error N", gp.y, gp2.y, 0.01);
		}
	}

	private final static TestCoordinate[] TEST_COORDINATES = new TestCoordinate[] { new TestCoordinate("StKilda", 57.8135184216667, -8.57854461027778, 9587.897, 899448.993), new TestCoordinate("BLAC", 53.7791102569444, -3.04045490694444, 331534.552, 431920.792), new TestCoordinate("BRIS", 51.4275474336111, -2.54407618611111, 362269.979, 169978.688), new TestCoordinate("BUT1", 58.5156036180556, -6.26091455638889, 151968.641, 966483.777), new TestCoordinate("CARL", 54.8954234052778, -2.93827741472222, 339921.133, 556034.759), new TestCoordinate("CARM", 51.8589089675, -4.30852476611111, 241124.573, 220332.638), new TestCoordinate("COLC", 51.8943663752778, 0.897243275, 599445.578, 225722.824), new TestCoordinate("DARE", 53.3448028066667, -2.64049320722222, 357455.831, 383290.434), new TestCoordinate("DROI", 52.2552938163889, -2.15458614944444, 389544.178, 261912.151), new TestCoordinate("EDIN", 55.9247826525, -3.29479218777778, 319188.423, 670947.532), new TestCoordinate("FLA1", 54.1168514433333, -0.077731326666667, 525745.658, 470703.211), new TestCoordinate("GIR1", 57.1390251930556, -2.04856031611111, 397160.479, 805349.734), new TestCoordinate("GLAS", 55.8539995297222, -4.29649015555556, 256340.914, 664697.266), new TestCoordinate("INVE", 57.4862500033333, -4.21926398944444, 267056.756, 846176.969), new TestCoordinate("IOMN", 54.3291954105556, -4.38849118, 244780.625, 495254.884), new TestCoordinate("IOMS", 54.0866631808333, -4.634521685, 227778.318, 468847.386), new TestCoordinate("KING", 52.7513668744444, 0.401535476944444, 562180.535, 319784.993), new TestCoordinate("LEED", 53.8002151991667, -1.66379167583333, 422242.174, 433818.699), new TestCoordinate("LIZ1", 49.9600613830556, -5.20304610027778, 170370.706, 11572.404), new TestCoordinate("LOND", 51.4893656461111, -0.119925564166667, 530624.963, 178388.461), new TestCoordinate("LYN1", 53.4162851577778, -4.28918069305556, 247958.959, 393492.906), new TestCoordinate("LYN2", 53.4163092516667, -4.28917792638889, 247959.229, 393495.58), new TestCoordinate("MALA", 57.0060669652778, -5.82836692638889, 167634.19, 797067.142), new TestCoordinate("NAS1", 51.4007822038889, -3.55128348722222, 292184.858, 168003.462), new TestCoordinate("NEWC", 54.97912274, -1.61657684555556, 424639.343, 565012.7), new TestCoordinate("NFO1", 51.3744702591667, 1.44454730694444, 639821.823, 169565.856), new TestCoordinate("NORT", 52.2516095091667, -0.91248957, 474335.957, 262047.752), new TestCoordinate("NOTT", 52.962191095, -1.19747656166667, 454002.822, 340834.941), new TestCoordinate("OSHQ", 50.9312793775, -1.45051434055556, 438710.908, 114792.248), new TestCoordinate("PLYM", 50.4388582547222, -4.10864563972222, 250359.798, 62016.567), new TestCoordinate("SCP1", 50.5756366516667, -1.29782277138889, 449816.359, 75335.859), new TestCoordinate("SUM1", 59.8540991425, -1.27486911222222, 440725.061, 1107878.445), new TestCoordinate("THUR", 58.5812046144444, -3.72631021305556, 299721.879, 967202.99), new TestCoordinate("Scilly", 49.9222639433333, -6.29977752722222, 91492.135, 11318.801), new TestCoordinate("Flannan", 58.2126224813889, -7.59255563111111, 71713.12, 938516.401), new TestCoordinate("NorthRona", 59.0967161777778, -5.82799340888889, 180862.449, 1029604.111), new TestCoordinate("SuleSkerry", 59.0933503508333, -4.41757674166667, 261596.767, 1025447.599), new TestCoordinate("Foula", 60.1330809208333, -2.07382822361111, 395999.656, 1138728.948), new TestCoordinate("FairIsle", 59.5347079433333, -1.62516965833333, 421300.513, 1072147.236), new TestCoordinate("Orkney", 59.03743871, -3.21454001055556, 330398.311, 1017347.013), new TestCoordinate("Ork_Main(Ork)", 58.7189371830556, -3.07392603527778, 337898.195, 981746.359), new TestCoordinate("Ork_Main(Main)", 58.7210828644444, -3.13788287305556, 334198.101, 982046.419), };

	private static class TestCoordinate {
		public final String station;
		public final double lat, lng;
		public final GridPoint gp;

		public TestCoordinate(String station, double lat, double lng, double e, double n) {
			this.station = station;
			this.lat = lat;
			this.lng = lng;
			this.gp = new GridPoint(e, n);
		}
	}
}
