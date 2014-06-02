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

import android.util.Log;

public class Placemark {
	private final static String TAG = "Placemark";

	final String mName;
	final Type mType;
	final String mCounty;
	final GridPoint mGridPoint;

	Placemark(String name, String type, String county, GridPoint gp)
	{
		mName = name;
		// Convert the type to the index.
		mType = Type.fromString(type);
		mCounty = county;
		mGridPoint = gp;
	}

	public String getName() {
		return mName;
	}

	public Type getType() {
		return mType;
	}

	public String getCounty() {
		return mCounty;
	}

	public GridPoint getPosition() {
		return mGridPoint;
	}

	public static enum Type {
		// tchan: Should these be the other way around?
		ANTIQUITY("A"),
		CITY("C"),
		FOREST("F"),
		FARM("FM"),
		HILL("H"),
		OTHER("O"),
		ROMAN("R"),
		TOWN("T"),
		WATER("W"),
		UNKNOWN("X"),
		;

		private final String mShortCode;
		private Type(String code) {
			mShortCode = code;
		}

		public final String typeCode() {
			return this.mShortCode;
		}

		static Type fromNameOrNull(String typeName) {
			try {
				return Type.valueOf(typeName);
			} catch (IllegalArgumentException e) {
				if (BuildConfig.DEBUG) {
					Log.v(TAG, "", e);
				}
				// Map unmatched to Other.
				return Type.valueOf("OTHER");
			}
		}

		static Type fromCodeOrNull(String typeCode) {
			for (Type t : Type.values()) {
				if(t.mShortCode.equals(typeCode))
				{
					return t;
				}
			}
			return null;
		}

		static Type fromString(String s) {
			if (s == null) {
				return null;
			}

			if (s.length() <= 2) {
				return fromCodeOrNull(s);
			}

			return fromNameOrNull(s);
		}
	}
}

