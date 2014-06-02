package uk.co.ordnancesurvey.android.maps.test;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import android.os.Environment;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import uk.co.ordnancesurvey.android.maps.*;

/**
 * These tests are reliant on preloading gaz50k.ospoi onto the SD card.
 */
public class GeocoderTest extends AndroidTestCase  {
	

	final String[] testGazResults = 
		{
			"130:NN9522:Abbey:NN82:56:23:3:41.5:722500:295500:W:PK:Pth & Kin:Perth and Kinross:X:01-MAR-1993:I:52:58:53",
			"8858:NY8325:Arngill Head Brocks:NY82:54:37.5:2:15.3:525500:383500:W:DU:Durham:Durham:X:01-MAR-1993:I:91:92:0",
			"14051:NS3590:Bandry:NS28:56:4.7:4:38.6:690500:235500:W:AR:Arg & Bt:Argyll and Bute:X:01-MAR-1993:I:56:0:0",
			"8679:NY1439:Arkleby:NY02:54:44.6:3:19.7:539500:314500:W:CU:Cumbr:Cumbria:O:01-MAR-1993:I:89:0:0",
			"17556:SP9842:Beancroft Fm:SP84:52:4.3:0:33.8:242500:498500:W:BK:C Beds:Central Bedfordshire:FM:01-MAR-1993:I:153:0:0",
			"24987:SN1324:Blaenafon:SN02:51:53.2:4:42.6:224500:213500:W:CT:Carm:Carmarthenshire:X:01-MAR-1993:I:145:158:0",
		};

	final class TestPostCode {
		public String code;
		int n1;
		int easting;
		int northing;
		String x1, x2, x3, x4, x5, x6;
		public TestPostCode(String _c, int _n1, int _e, int _n, String _x1, String _x2, String _x3, String _x4, String _x5, String _x6)
		{
			code = _c;
			n1 = _n1;
			easting = _e;
			northing = _n;
			x1 = _x1;
			x2 = _x2;
			x3 = _x3;
			x4 = _x4;
			x5 = _x5;
			x6 = _x6;
		}
	}
	
	TestPostCode[] testPCResults = {
			new TestPostCode("CV1 1DA",10,433370,279136,"E92000001","E19000001","E18000005","","E08000026","E05001228"),
			new TestPostCode("DL7 9TW",10,431442,488509,"E92000001","E19000001","E18000003","E10000023","E07000164","E05006217")
	};
	
	
	final String[] testLocatorResults = 
		{
//			":A1:358846:676314:355819:361615:675907:677383::East Linton / Gifford:East Lothian:East Lothian:NT57NE:NT57:Roads",
	//		":B6160:403153:461085:402243:403453:460450:461922:Burnsall:Burnsall:North Yorkshire:Craven District:SE06SW:SE06:Roads",
		//	":B742:241833:619691:238606:242496:617558:622770:Hillhead:Coylton and Minishant:South Ayrshire:South Ayrshire:NS41NW:NS41:Roads",
			"ADDISON GARDENS::518934:168213:518889:518962:168115:168313:LONDON:St. Mark's:Kingston upon Thames:Kingston upon Thames London Boro:TQ16NE:TQ16:Roads",

			//":B3257:226682:79643:225168:227963:78633:80151::Lewannick:Cornwall:Cornwall:SX27NE:SX27:Roads",
    
	//		"ADDISON GARDENS::524021:179674:523985:524057:179646:179702:LONDON:Holland:Royal Borough of Kensington & Chelsea:Kensington and Chelsea London Boro:TQ27NW:TQ27:Roads",
		};
	

	
	private Geocoder getGeocoder() {
		try {
			File db = new File(Environment.getExternalStorageDirectory(), "gaz50k.ospoi");
			return new Geocoder(db, "Airsource", getContext(), true);
		} catch(FailedToLoadException e) {
			return null;
		}
	}
	
	

	public void testGeocodeGazetteerPerformance()
	{
		Geocoder geocoder = getGeocoder();
		
		
		// Search for "S" over the entire national grid, on a range search
		long start = SystemClock.uptimeMillis();

		
		geocoder.geocodeString("S", EnumSet.of(Geocoder.GeocodeType.Gazetteer), null, 0, 100);

		long now = SystemClock.uptimeMillis();
		long d = now - start;
		assertTrue("Search of 'S' in gazetteer took " + d + "ms", d < 300);
    }

	public void doTestPostCode(boolean online)
	{
		Geocoder geocoder = getGeocoder();
    
		for(final TestPostCode pc : testPCResults)
		{
			final String searchString = pc.code;
			GridRect rect = null;
			EnumSet<Geocoder.GeocodeType> type = EnumSet.of(Geocoder.GeocodeType.Postcode);
			if(online)
			{
				type = EnumSet.of(Geocoder.GeocodeType.OnlinePostcode);
			}
			int start = -1;
			int numResults = 0;
			boolean matched = false;
			
			List<? extends Placemark> placemarks = geocoder.geocodeString(searchString, type, rect, start, numResults).getPlacemarks();
            for(Placemark p : placemarks)
            {
                // Ignore results that don't precisely match (it's a prefix search) apart from checking that they really are sensible...
				// Ignore results that don't precisely match (it's a prefix search) apart from checking that they really are sensible...
				if(!p.getName().equals(searchString))
				{
					assertFalse("Not a substring match", p.getName().indexOf(searchString) == -1);
					continue;
				}

				assertEquals("Check names match", p.getName(), searchString);

				GridPoint gp = p.getPosition();
                if(gp.y == pc.northing
                   && gp.x == pc.easting)
                {
                    matched = true;
                }
            }
            assertTrue("No match found in search results for " + searchString, matched);
			
        
		}
    } 
    
    
    
   
	private void doTestGeocodeGazetteer(final boolean online)
	{
		Geocoder geocoder = getGeocoder();

		for(String testResult : testGazResults)
		{
			final String[] comps = testResult.split(":"); 
			final String searchString = comps[2];
			
			
			// We're verifying the database in this test, not the filtering which is part of the query.
			GridRect rect = null;
			EnumSet<Geocoder.GeocodeType> type = EnumSet.of(Geocoder.GeocodeType.OnlineGazetteer);
			if(!online)
			{
				type = EnumSet.of(Geocoder.GeocodeType.Gazetteer);
			}
			int start = -1;
			int numResults = 0;
			
			boolean matched = false;
			
			List<? extends Placemark> placemarks = geocoder.geocodeString(searchString, type, rect, start, numResults).getPlacemarks();
			for(Placemark p : placemarks)
			{
				// Ignore results that don't precisely match (it's a prefix search) apart from checking that they really are sensible...
				if(!p.getName().equals(searchString))
				{
					assertFalse("Not a substring match " + p.getName() + "," + searchString, p.getName().indexOf(searchString) == -1);
					continue;
				}

				assertEquals("Check names match", p.getName(), comps[2]);

				GridPoint gp = p.getPosition();
				if(gp.x == Integer.parseInt(comps[9]) && gp.y == Integer.parseInt(comps[8]))
				{
					if(p.getCounty().equals(comps[13]))
					{
						if(p.getType().typeCode().equals(comps[14]))
						{
							matched = true;
						}
						
						// Some "X"s are now "O"s
						if(online && comps[14].equals("X") && p.getType().typeCode().equals("O"))
						{
							matched = true;
						}
					}
				}
			}
			// Note that the Abbey search fails with the online gazetteer. This is because the online service doesn't return all the results for Abbey!
			if(!online || !searchString.equals("Abbey"))
			{
				assertTrue("No match found in search results " + searchString, matched);
			}			
		}
        
    } 
    
   

	 
	public void testGeocodeGazetteer()
	{
		doTestGeocodeGazetteer(false);
		doTestGeocodeGazetteer(true);
	}

	public void testGeocodePostCode()
	{
		doTestPostCode(false);
		doTestPostCode(true);
	}
	

	public void testLocator()
	{
		Geocoder geocoder = getGeocoder();

		for(String testLocator : testLocatorResults)
		{
			final String[] comps = testLocator.split(":");
			final String searchString = comps[0].length() > 0 ? comps[0] : comps[1];
			
			GridRect rect = null;
			for(int j = 0; j < 3; j++)
			{
				if(j == 1)
				{
					GridPoint gp = new GridPoint(Double.valueOf(comps[2]), Double.valueOf(comps[3]));
					rect = new GridRect(gp.x - 500, gp.y - 500, gp.x + 500, gp.y + 500);
	            }
				if(j == 2)
				{
					// Move the rect outside the search point (move a fair way to allow for bounding boxes)
					rect = new GridRect(rect.minX + 100000, rect.minY, rect.maxX + 100000, rect.maxY);
				}
				// We're verifying the database in this test, not the filtering which is part of the query.
				EnumSet<Geocoder.GeocodeType> type = EnumSet.of(Geocoder.GeocodeType.Road);
            
				int start = -1;
				int numResults = 0;
				
				boolean matched = false;
				for(Placemark p : geocoder.geocodeString(searchString, type, rect, start, numResults).getPlacemarks())
				{
					Road r = (Road)p;
				    // Ignore results that don't precisely match (it's a prefix search) apart from checking that they really are sensible...
					if(!(r.roadClassifier() != null && r.roadClassifier().equals(searchString))
							&& (r.roadName() != null &&!r.roadName().toLowerCase(Locale.ENGLISH).equals(searchString.toLowerCase(Locale.ENGLISH))))
					{
						assertFalse("Not a substring match", r.roadClassifier().indexOf(searchString) == -1);
						continue;
	                }

					GridPoint gp = r.getPosition();
					if(String.valueOf((int)gp.x).equals(comps[2])
							&& String.valueOf((int)gp.y).equals(comps[3]))
					{
						String settlement = r.settlement();
						if(settlement == null)
						{
							settlement = "";
						}

						if(r.getCounty().equals(comps[10])
								&& settlement.equals(comps[8])
								&& r.locality().equals(comps[9]))
						{
							matched = true;
						}
					}
				}					



				if(j == 2)
				{
					assertFalse("Match found in search results for " +  testLocator, matched);
				}
				else
				{
					assertTrue(j + "No match found in search results for " +  testLocator, matched);
				}
			}
		} 




	}



	public void testInvalidDB() {
		try {
			File db = new File(Environment.getExternalStorageDirectory(), "jhkkjhj.ospoi");

			new Geocoder(db,null,null,true);
		} catch(FailedToLoadException e) {
			return;
		}
		fail("Expected FailedToLoadException");
	}
	
	
	

	public void testNoSearch() throws Throwable {
		Geocoder geo = new Geocoder(null,null,null,true);
		geo.geocodeString("nonexistent", Geocoder.GeocodeType.allOnline(), null, -1, 0);
		geo.geocodeString("nonexistent",  EnumSet.of(Geocoder.GeocodeType.Postcode), null, -1, 0);
		geo = null;
	}
}
