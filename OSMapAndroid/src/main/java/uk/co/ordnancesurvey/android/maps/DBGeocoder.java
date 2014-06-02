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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

final class DBGeocoder {
	private SQLiteDatabase mDB;
	private final String[] mFeatureCodes;
	private final int[] mFeatureIndexes;

	private static final String[] ROAD_SUFFIXES =
	{
	"GRANGE", "ORCHARD", "BROW", "MEADOWS", "WOOD", "VALE", "BRAE", "FIELDS", "WYND", "YARD", "GARTH", "CHASE", "FOLD", "FIELD", "END", "MEAD", "MOUNT", "DROVE", "EAST", "MEADOW", "SOUTH", "NORTH", "COURT", "ROW", "PLACE", "WEST", "GATE", "BANK", "WAY", "CROFT", "DRIVE", "STREET", "AVENUE", "SQUARE", "GREEN", "RISE", "LANE", "MEWS", "VIEW", "WALK", "PARK", "CLOSE", "HILL", "ROAD", "TERRACE", "CRESCENT", "GARDENS"
	};
	private final static HashMap<String, Integer> SUFFIXES_DICT;

	/** Static initializer */
	static {
		// Build suffixes dictionary.
		HashMap<String, Integer> d = new HashMap<String, Integer>();
		int i = 0;
		for(String suffix : ROAD_SUFFIXES)
		{
			d.put(suffix, i);
			i++;
		}

		// Add some aliases
		d.put("RD", d.get("ROAD"));
		d.put("ST", d.get("STREET"));
		d.put("AV", d.get("AVENUE"));
		d.put("AVE", d.get("AVENUE"));
		d.put("FM", d.get("FARM"));
		d.put("CT", d.get("COURT"));

		SUFFIXES_DICT = d;
	}

	public DBGeocoder(File db) throws FailedToLoadException
	{
		if (!db.exists()) {
			// Reduce logspam: Don't try to open a database doesn't exist.
			// Sqlite unconditionally logs about 30 lines at ERROR severity.
			throw new FailedToLoadException("File not found: " + db.getPath());
		}

		try {
			mDB = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		} catch(SQLiteException e) {
			throw new FailedToLoadException(e);
		}

		ArrayList<String> featureCodes = new ArrayList<String>();
		ArrayList<Integer> featureIndexes = new ArrayList<Integer>();
		// Load up feature codes.
		Cursor cursor = mDB.rawQuery("SELECT * from feature_codes", null);
		while(cursor.moveToNext())
		{
			String f_code = cursor.getString(0);
			int firstIndex = cursor.getInt(1);
			featureCodes.add(f_code);
			featureIndexes.add(firstIndex);
		}
		int[] featureIndexArray = new int[featureIndexes.size()];
		for (int i = 0; i < featureIndexArray.length; i++)
		{
			featureIndexArray[i] = featureIndexes.get(i);
		}
		mFeatureCodes = featureCodes.toArray(new String[0]);
		mFeatureIndexes = featureIndexArray;
	}

	private String featureCodeForSeq(int seq)
	{
		int i = -1;
		for(int index : mFeatureIndexes)
		{
			if(seq < index) 
			{
				break;
			}
			i++;
		}
		return mFeatureCodes[i];
	}

	private static String stringPlusOne(String s)
	{
		int last = s.length() - 1;
		char c = s.charAt(last);
		c = (char)(c + 1);
		String ss = s.substring(0, last) + c;
	    return ss;
	}

	/**
	* Like cursor.getString() but converts the empty string to null.
	*/
	private static String getStringOrNull(Cursor cursor, int column)
	{
		String ret = cursor.getString(column);
		if (ret == null || ret.length() == 0)
		{
			return null;
		}
		return ret;
	}

	private static RoadWrapper roadFromCursor(Cursor cursor)
	{
		String name = cursor.getString(0);
		if(!cursor.isNull(6))
		{
			String suffix = ROAD_SUFFIXES[cursor.getInt(6)];
			
			// Uppercase first character
			char[] stringArray = suffix.toCharArray();
			stringArray[0] = Character.toUpperCase(stringArray[0]);
			
			name = name + " " + new String(stringArray);
		}
		
		String roadClassifier = getStringOrNull(cursor, 1);
		String settlement = getStringOrNull(cursor, 2);
		String locality = getStringOrNull(cursor, 3);

		int x = cursor.getInt(4);
		int y = cursor.getInt(5);
		GridPoint gp = new GridPoint(x,y);
		
		String county = cursor.getString(7);
		int rowId = cursor.getInt(8);

		// TODO: This was unused in the original code. Isn't it just "name"?
		String roadName = null;

		Road road = new Road(name, county, gp, roadName, roadClassifier, settlement, locality);
		return new RoadWrapper(road, rowId);
	}

	List<RoadWrapper> _locateRoad(String s, String extraQuery, List<String> extraArgs, int start, int numResults)
	{
		// Identify suffixes in the search string
		String[] components = s.split(" ");
		ArrayList<String> nComponents = new ArrayList<String>();
		String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		// Normalize the search
		for(String component : components) 
		{
			String comp = component.replaceAll(" ", "");
			if(comp.length() == 0)
			{
				continue;
			}
			
			// Components starting with a number should be lower cased.
			char firstChar = comp.charAt(0);
			if(alpha.indexOf(firstChar) > -1)
			{
				// Capitalize the component
				comp = toTitleCase(comp);
			}
			else
			{
				comp = comp.toLowerCase(Locale.ENGLISH);
			}
			nComponents.add(comp);
		}
		
		int last = nComponents.size() - 1;
		String suffix = nComponents.get(last).toUpperCase(Locale.ENGLISH);
		Integer suffixNumber = SUFFIXES_DICT.get(suffix);
		if(suffixNumber != null)
		{
			if(last > 0)
			{
				nComponents.remove(last);
			}
			// We ought to handle just searching on a suffix...
		}
		
		String ss = TextUtils.join(" ", nComponents);
		
		String query = null;
		String[] queryArgs = new String[1];
		int argi = 0;
		queryArgs[argi++] = ss;
		
		String preQuery = "select id from road_names where name = ?";
		Cursor result = mDB.rawQuery(preQuery, queryArgs);
		String nameMatch = null;
		if(result.moveToNext())
		{
			int nameId = result.getInt(0);
			nameMatch = " = " + nameId + " ";
			result.close();
		}
		else
		{
			preQuery = "select id from road_names where name >= ? and name < ? ORDER BY id";
			result.close();
			queryArgs = Arrays.copyOf(queryArgs, queryArgs.length + 1);
			queryArgs[argi++] = stringPlusOne(ss);
			result = mDB.rawQuery(preQuery, queryArgs);
			
			int firstId = -1;
			int lastId = -1;
			while(result.moveToNext()){
				lastId = result.getInt(0);
				if(firstId == -1)
				{
					firstId = lastId;
				}
			}
			result.close();
			if(firstId != -1)
			{
				nameMatch = " between " + firstId + " and " + lastId;
			}
			
		}
		
		String[] args = new String[0];
		argi = 0;
		String basicSelect = "select name,classification,settlement,locality,center_x,center_y,suffix,full_county,roads.id from roads inner join counties on roads.co_code = counties.co_code left outer join settlements on roads.settlement_id  = settlements.settlement_id natural join localities";
		if(nameMatch != null)
		{
			query = basicSelect + " inner join road_names on roads.name_id = road_names.id";
			if(suffixNumber != null)
			{
				args = Arrays.copyOf(args, args.length + 1);
				args[argi++] = suffixNumber.toString();
				query = query + " where (roads.name_id " + nameMatch + " and suffix is ?)";
			}
			else
			{
				query = query + " where roads.name_id " + nameMatch;
			}
		}
		else
		{
            // No name found. Work off classification
            // Standardize the formatting of classification - if it's A1M, standardize to A1(M).
			String classification = ss;
			if(classification.length() > 1 && classification.endsWith("M"))
			{
				classification = classification.replaceAll("M", "(M)");
			}
            // We'd also like A1 to match A1(M)...
			String class2 = classification + "(M)";
			args = Arrays.copyOf(args, args.length + 2);
			args[argi++] = classification;
			args[argi++] = class2;
			query = basicSelect + " left outer join road_names on roads.name_id = road_names.id where ((classification = ?) or (classification = ?))";
		}
		if(extraArgs != null)
		{
			args = Arrays.copyOf(args, args.length + extraArgs.size());

			for(String earg : extraArgs)
			{
				args[argi++] = earg;
			}
		}
		if(extraQuery != null)
		{
			query = query + " " + extraQuery;
		}
		if(start > -1)
		{
			query = query + " limit " + numResults + " offset " + start;
		}
		result = mDB.rawQuery(query, args);

		ArrayList<RoadWrapper> roads = new ArrayList<RoadWrapper>(20);
		while(result.moveToNext())
		{
			RoadWrapper road = roadFromCursor(result);
			roads.add(road);
		}
		return roads;
	}
	
	
	List<RoadWrapper> _locateRoad(String s, GridRect rect, int start, int numResults)
	{
		if(rect == null || rect.isNull())
		{
			return _locateRoad(s, null, null, start, numResults);
		}
		ArrayList<String> extraArgs = new ArrayList<String>(4);
		extraArgs.add(String.valueOf(rect.minX));
		extraArgs.add(String.valueOf(rect.maxX));
		extraArgs.add(String.valueOf(rect.minY));
		extraArgs.add(String.valueOf(rect.maxY));
		
		return _locateRoad(s, " and center_x between ? and  ? and center_y between ? and  ?",
				extraArgs, start, numResults);
	}
	
	List<Placemark> _doGazetteerSearch(String s, GridRect rect, int start, int numResults, boolean exact)
	{
		ArrayList<Placemark> placemarks = new ArrayList<Placemark>(10);
		String ss = s.toLowerCase(Locale.ENGLISH);
		// Strip apostrophes
		ss = ss.replaceAll("'", "");
		
        // Construct the search. We want an exact search on all but the last word..
		String[] words = ss.split("\\s+");
		String exactWords = null;
		int wordCount = 0;
		String lastWord = null;
		
		for(int i = words.length -1; i >= 0; i--)
		{
			String word = words[i];
			// probably an unnecessary check
			if(word.length() == 0)
			{
				continue;
			}
			if(lastWord == null && !exact)
			{
				lastWord = word;
				continue;
			}
			word = "'" + word + "'";
			if(exactWords != null)
			{
				exactWords = exactWords +"," + word;
			}
			else
			{
				exactWords = word;
			}
			wordCount++;
		}
		
		String query = "select seq,def_nam,full_county,east1000,north1000 from features left outer join counties on features.co_code = counties.co_code ";
		if(wordCount > 0)
		{
			query = query + "where seq in (select feature_id from matches where word_id in (select word_id from words where word in (" 
								+ exactWords  
								+ ")) group by feature_id having count(word_id) = " + wordCount + " ";
		}
		Cursor result;
		int argi = 0;
		String[] args = null;
		if(lastWord != null)
		{             
			// Handle the lastWord, normally matched with a prefix.
			String preQuery = "select word_id from words where word >= ? and word < ? ORDER BY word_id LIMIT 1";
			String lastWordPlusOne = stringPlusOne(lastWord);
			String[] preargs = new String[2];
			preargs[0] = lastWord;
			preargs[1] = lastWordPlusOne;		
			result = mDB.rawQuery(preQuery,  preargs);
			if(!result.moveToFirst())
			{
				result.close();
				return null;
			}
			int firstId = result.getInt(0);
			result.close();
			String preQuery2 = "select word_id from words where word >= ? and word < ? ORDER BY word_id DESC LIMIT 1";
			preargs[0] = lastWord;
			preargs[1] = lastWordPlusOne;
			result = mDB.rawQuery(preQuery2, preargs);
			if(!result.moveToFirst())
			{
				result.close();
				return null;
			}
			int lastId = result.getInt(0);
			result.close();
			
			args = new String[2];
			args[argi++] = String.valueOf(firstId);
			args[argi++] = String.valueOf(lastId);
			if(wordCount > 0)
			{
				query += "intersect select distinct feature_id from matches where word_id between ? and ? @)";				
			}
			else
			{
                // Only look for the first two features as an optimisation.
				int numTowns = mFeatureIndexes[2];
				if((lastId - firstId) > numTowns)
				{
                    // Optimisation - limit the set of returned results to towns. Indexing on feature id products
                    // a smaller set, so do that... The + on word_id forces indexing on feature_id
					query += "where seq in (select distinct feature_id from matches where +word_id between ? and ? and feature_id < ? order by feature_id @)";
                    args = Arrays.copyOf(args, args.length + 1);
                    args[argi++] = Integer.toString(numTowns);
				}
				else
				{
					query +=  "where seq in (select distinct feature_id from matches where word_id between ? and ? order by feature_id @)";
				}
			}
		}
		else
		{
			query = query + ")";
		}
		String limitSubClause = "";
		if(rect != null && !rect.isNull())
		{
			query = query + " and north1000 between ? and  ? and east1000 between ? and ?";
			if(args != null)
			{
				args = Arrays.copyOf(args, args.length + 4);
			}
			else
			{
				args = new String[4];
			}
			args[argi++] = String.valueOf(rect.minY / 1000);
			args[argi++] = String.valueOf(rect.maxY / 1000);
			args[argi++] = String.valueOf(rect.minX / 1000);
			args[argi++] = String.valueOf(rect.maxX / 1000);
		}
		if(start > -1)
		{
			query = query + " limit " + numResults + " offset " + start;
			
			if(rect == null || rect.isNull())
			{
                // With no geographic rect and no offset, it's safe to add a limit clause to the prefix.
                // It is important to order by feature_id to ensure that the order of results is the same as if we searched
                // the entire set of matches. 
				limitSubClause = " limit " + (start + numResults);
			}
		}
		
		if(lastWord != null)
		{
			query = query.replaceAll("@", limitSubClause);
		}
		
		result = mDB.rawQuery(query, args);
		
		while(result.moveToNext())
		{
            // Build a placemark for this result.
			final int SEQ = 0;
			final int DEF_NAME = 1;
			final int FULL_COUNTY = 2;
			final int EAST1000 = 3;
			final int NORTH1000 = 4;
			
			int seq =  result.getInt(SEQ);
			String name = result.getString(DEF_NAME);
			String type = featureCodeForSeq(seq);
			String county = result.getString(FULL_COUNTY);

			int x = result.getInt(EAST1000) * 1000 + 500;
			int y = result.getInt(NORTH1000) * 1000 + 500;
			GridPoint gp = new GridPoint(x,y);
			Placemark placemark = new Placemark(name, type, county, gp);
			placemarks.add(placemark);
        }
		result.close();
		return placemarks;
	}
	
	private Integer partIdForPart(String part)
	{
		if(part == null)
		{
			return null;
		}
		String query = "select id from parts where part = ?";
		String[] args = new String[1];
		args[0] = part;
		Cursor result = mDB.rawQuery(query, args);
		Integer partId = null;
		while(result.moveToNext())
		{
			partId = Integer.valueOf(result.getInt(0));
	        // There should be only one exactly matching result, but break anyway just in case!
			break;
		}
		result.close();
		return partId;
	}
	

/*
 Postcode search will match as follows:
 A postcode area (e.g. CB) will not match any results
 A postcode district (e.g. CB12) will return up to two results,.
 One result will be returned for a matching district.
 One result will be returned for a matching sector (i.e. CB1 2)
 A postcode sector specified with a space (e.g. CB1 2) will return up to one result for the matching sector
 A postcode unit (e.g. CB4 1BH) will return up to one result, for the matching unit
 A partially specified postcode unit (e.g. CB4 1B) will return a result for each unit in the database
 that matches the partial specification (i.e. CB4 1BS, CB 1BHT, ...)
*/
	List<Placemark> _doPostcodeSearch(String s, GridRect rect, int start, int numResults)
	{
	    // Normalize the search term
		String ss = s.toUpperCase(Locale.ENGLISH);
		if(ss.length() == 0)
		{
			return null;
		}
		
		// Is there a space
		int space = ss.indexOf(" ");
		String part1 = null;
		String part2 = null;
		String part1alt = null;
		String part2alt = null;
		
		if(space != -1)
		{
			// Split on the space
			part1 = ss.substring(0, space);
			part2 = ss.substring(space);
			part1 = part1.replaceAll("\\s", "");
			part2 = part2.replaceAll("\\s", "");			
		}
		else
		{
	        // No space entered:
	        // for DDL (split between 1 and 2), or DDD (split between 2 and 3). The ambiguous cases
	        // are D - this could be something like CB4, or the start of CB41. Treat as CB4, because we
	        // only allow exact matches on part1 anyway.
	        // the final ambiguous case is CB41. Is this CB4 1, or CB41. The only logical thing we can do
	        // is search for both - or maybe require the user to enter a space?
	        // Searching for both is problematic if a range is specified.We need to search for part1 CB41, any part 2,
	        // and part 1 CB4, part 2 1xx. Well, it's doable.
	        
	        // These rules can be simplified.
	        // If the string entered is 2 characters long then it's all part1.
	        // If the string is 5 or more characters long, then split before the final digit.
	        // If the string is 3 or 4 characters long, we will have two things to search for...
			
			String digits = "0123456789";
			int len = ss.length();
			if(len <= 2)
			{
				part1 = ss;
			}
			else if(len >= 5)
			{
				// Split on last digit
				for(int c = len - 1; c > 0; c--)
				{
					char test = ss.charAt(c);
					// A mathematical comparison would be faster
					if(digits.indexOf(test) != -1)
					{
						// This is the last digit.
						part1 = ss.substring(0, c);
						part2 = ss.substring(c);
						break;
					}
				}
			}
			else
			{
	            // This is the complicated case, something like DD.
	            // We assume that both the last two characters are digits (no point checking otherwise).
	            // We have to combine two searches!
				part1 = ss;
				part1alt = ss.substring(0,  len - 1);
				
				// Spit on first digit.
				for(int c = 0; c < len - 1; c++)
				{
					char test = part1alt.charAt(c);
					if(digits.indexOf(test) != -1)
					{
						part2alt = ss.substring(len -1);
					}
				}
				if(part2alt == null)
				{
					part1alt = null;
				}
			}
		}
		
		if(part1 != null && part1.length() == 0)
		{
			part1 = null;
		}
		if(part2 != null && part2.length() == 0)
		{
			part2 = null;
		}
		if(part1alt != null && part1alt.length() == 0)
		{
			part1alt = null;
		}
		
		ArrayList<Placemark> placemarks = new ArrayList<Placemark>(10);
		Integer part1_id = partIdForPart(part1);
		if(part1_id != null)
		{
			String query  = "select * from postcodes left outer join parts on postcodes.part2_id = parts.id where (part1_id = ?";
			String[] args = new String[1];
			int argi = 0;
			args[argi++] = part1_id.toString();
			
            // In addition we do an exact match on part2, unless part2 is 2 characters in which case we do a prefix search.
			if(part2 == null)
			{
				query = query + " and part2_id is null)";
			}
			
			if(part1alt != null)
			{
				Integer part1_alt_id = partIdForPart(part1alt);
				if(part1_alt_id != null)
				{
					query = query + " or (part1_id = ?";
					args = Arrays.copyOf(args, args.length + 1);
					args[argi++] = part1_alt_id.toString();
					part2 = part2alt;
				}
			}
			
			if(part2 != null && part2.length() == 2)
			{
                // With a prefix match on part2 if one was specified.
				query = query + " and part2_id in (select id from parts where part >= ? and part < ?))";
				args = Arrays.copyOf(args, args.length + 2);
				args[argi++] = part2;
				args[argi++] = stringPlusOne(part2);
			}
			else if(part2 != null) 
			{
				query = query + " and part2_id in (select id from parts where part = ?))";
				args = Arrays.copyOf(args, args.length + 1);
				args[argi++] = part2;
			}
			
			if(rect != null && !rect.isNull())
			{
				query = query + " and easting between ? and  ? and northing between ? and ?";
				args = Arrays.copyOf(args, args.length + 4);
				args[argi++] = String.valueOf(rect.minX);
				args[argi++] = String.valueOf(rect.maxX);
				args[argi++] = String.valueOf(rect.minY);
				args[argi++] = String.valueOf(rect.maxY);
			}
			
			if(start != -1)
			{
				query = query + " limit "+ numResults + " offset " + start;
			}
			
			Cursor result = mDB.rawQuery(query,  args);
			while(result.moveToNext())
			{
				part2 = result.getString(result.getColumnIndex("part"));
				int partid = result.getInt(result.getColumnIndex("part1_id"));
				String name;
				if(partid == part1_id.intValue())
				{
					name = part1;
				}
				else
				{
					name = part1alt;
				}
				if(part2 != null)
				{
					name = name + " " + part2;
				}
				int x = result.getInt(result.getColumnIndex("easting"));
				int y = result.getInt(result.getColumnIndex("northing"));
				
				GridPoint gp = new GridPoint(x,y);
				Placemark p = new Placemark(name,  null, null, gp);
				placemarks.add(p);
			}
			result.close();
		}
		return placemarks;
	}
	

	/*
 Road search looks for roads where the search term is a prefix of the road name. Common suffixes are separated out
 for efficiency. Consequently
 "Hig Street" will match "High Street"
 "High Str" will not match "High Street"
 "132kv" will match "132KV Switch House Road"
 "Switch House" will not match  "132KV Switch House Road"


 If no matches are found (taking account of any specified area or range), then the the search attempts to split the
 road name into a road and a (gazetteer) location.  This attempt is slightly different to the normal gazetteer
 search, in that all words need to match exactly - there is no prefix matching. "High St Cambridg" will not match
 "High St, Cambridge". If a comma was included in the search term, the split is made there. Otherwise the match on
 location is initially greedy - all but the first word is treated as a location, and reduces in scope one word
 at a time until at least one location is matched.

 Once a set of candidate locations have been established, the set is trimmed to those containing the highest
 precedence feature code (i.e. preferably Cities, failing that Towns, and so forth). We then look for roads
 near those features. The set of roads is ordered by distance from the nearest feature, and we trim the set to
 those closer that twice the minimum distance.
	 */
	List<Road> _doLocator(String s, GridRect rect, int start, int numResults)
	{
		{
			// If there's an exact match on the road name, return that.
			List<RoadWrapper> roads = _locateRoad(s, rect, start, numResults);
			if(roads.size() > 0)
			{
				return RoadWrapper.unwrap(roads);
			}
		}

		// try to be intelligent. Is this a string in the form "Mill Road(,) Cambridge" or "High Street(,)Downham Market"??
		// If so, then we can strip a word (or more) off the end until we identify a placename and then try to match....

		// Strip words until we identify a place, or we give up.
		List<Placemark> places = null;
		String roadName = null;
		String placeName = null;
		ArrayList<Road> results = new ArrayList<Road>(10);

		// Is there a comma specified? We only cope with one comma. If you put two in, you're on your own.
		int sep = s.indexOf(",");
		ArrayList<String> w1 = new ArrayList<String>();
		ArrayList<String> w2 = new ArrayList<String>();
		if(sep != -1)
		{
			String p1 = s.substring(0, sep);
			String p2 = s.substring(sep);
			w1.add(p1);
			w2.add(p2);
		}
		else
		{
			String[] words = s.split("\\s+");
			for(String word : words)
			{
				if(w1.size() == 0)
				{
					w1.add(word);
				}
				else
				{
					w2.add(word);
				}
			}
		}
		
		roadName = null;
		while(w2.size() > 0)
		{
			roadName = TextUtils.join(" ", w1);
			placeName = TextUtils.join(" ", w2);
			
			places = _doGazetteerSearch(placeName, rect, -1, 0, true);
			if(places.size() > 0)
			{
				break;
			}
			
			// Move a string onto the road.
			w1.add(w2.get(0));
			w2.remove(0);
		}
		
		if(places == null || places.size() == 0)
		{
			return results;
		}

		// For each place, search for the road within a 20km bounding rect.
		double closest = Double.POSITIVE_INFINITY;
		// Protect against duplicates by using a set
		HashMap<RoadWrapper,Double> candidates = new HashMap<RoadWrapper,Double>();
		Placemark.Type f_code = null;
		
		for(Placemark place : places)
		{
			// If we match a city, don't bother with towns. And so forth.
			if(f_code == null)
			{
				f_code = place.mType;
			}
			else if(f_code == place.mType)
			{
				break;
			}
			
			int size;
			switch (place.mType)
			{
			case CITY:
				// 60km is enough for all points on the M25 to show up for London.
				size = 60000;
				break;
			case TOWN:
				// Arbitrary
				size = 20000;
				break;
			default:
				size = 10000;
				break;
			}
			
			GridPoint gp = place.mGridPoint;
			GridRect placeRect = GridRect.fromCentreXYWH(gp.x, gp.y, size, size);
			if(rect != null && !rect.isNull())
			{
				placeRect = placeRect.intersect(rect);
			}
			
			List<RoadWrapper> placeRoads = _locateRoad(roadName, placeRect, -1, 0);
			for(RoadWrapper road : placeRoads)
			{
				double d = road.mRoad.mGridPoint.distanceTo(gp);
				
				if(d < closest)
				{
					closest  = d;
				}
				// Pre-filtering. This can trim the result set
				// quite signficantly
				if(d <= closest * 2)
				{
					Double other = candidates.get(road);
					if (other == null || d < other.doubleValue())
					{
						candidates.put(road, d);
					}
				}
			}
		}

		// Only take results that are within 2 * minDistance
		for(Map.Entry<RoadWrapper,Double> entry : candidates.entrySet())
		{
			// The road is "close enough" if the distance to the nearest "place" is within 2*closest.
			double d = entry.getValue().doubleValue();
			if (d <= 2*closest)
			{
				results.add(entry.getKey().mRoad);
				break;
			}
		}
		return results;
	}

	public List<? extends Placemark> geocodeString(String ss, Geocoder.GeocodeType geocodeType, GridRect boundingRect, int start, int numResults) {
		ss = ss.trim();
		switch (geocodeType)
		{
		case Gazetteer:
			return _doGazetteerSearch(ss, boundingRect, start, numResults, false);
		case Postcode:
			return _doPostcodeSearch(ss, boundingRect, start, numResults);
		case Road:
			return _doLocator(ss, boundingRect, start, numResults);
		default:
			throw new IllegalArgumentException("Unsupported geocode type " + geocodeType);
		}
	}

	public void close()
	{
		synchronized (this)
		{
			if (mDB == null)
			{
				throw new IllegalStateException("Database is already closed");
			}
			mDB.close();
			mDB = null;
		}
	}

	private static String toTitleCase(String s) {
		char[] a = s.toCharArray();
		boolean prevWs = true;
		for (int i = 0; i < a.length; i++) {
			char c = a[i];
			a[i] = (prevWs ? Character.toTitleCase(c) : Character.toLowerCase(c));
			prevWs = Character.isWhitespace(c);
		}
		return new String(a);
	}

	private final static class RoadWrapper {
		final Road mRoad;
		final int mRowId;
		public RoadWrapper(Road road, int rowId) {
			mRoad = road;
			mRowId = rowId;
		}

		@Override
		public int hashCode() {
			// TODO: Do something better.
			return mRowId;
		}

		@Override
		public boolean equals(Object o) {
			// These are expected of anything overriding Object.equals()
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}

			RoadWrapper other = (RoadWrapper)o;
			// TODO: Do something better.
			return mRowId == other.mRowId;
		}

		static List<Road> unwrap(List<RoadWrapper> wrappedRoads) {
			ArrayList<Road> ret = new ArrayList<Road>(wrappedRoads.size());
			for (RoadWrapper r : wrappedRoads) {
				ret.add(r.mRoad);
			}
			return ret;
		}
	}
}
