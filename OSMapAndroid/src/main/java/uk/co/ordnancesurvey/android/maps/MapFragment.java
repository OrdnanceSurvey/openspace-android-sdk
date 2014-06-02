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

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// TODO this bit needs thinking about
/**
 * A Map component in an app. This fragment is the simplest way to place a map in an application. It's a wrapper around a view of a map 
 * to automatically handle the necessary life cycle needs. Being a fragment, this component can be added to an activity's layout file 
 * simply with the XML below.
 * <code>
&lt;fragment
    class="uk.co.ordnancesurvey.android.maps.MapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/&gt;
    * </code>
    * <p>
    * An {@link OSMap} can only be acquired using {@link #getMap()} when the underlying maps system is loaded and the underlying 
    * view in the fragment exists. This class automatically initializes the maps system and the view. If an {@link OSMap}
    *  is not available, {@link #getMap()} will return null.
    * <p>
    * A view can be removed when the MapFragment's {@link #onDestroyView()} method is called. When this happens the MapFragment 
    * is no longer valid until the view is recreated again later when MapFragment's onCreateView(LayoutInflater, ViewGroup, Bundle) 
    * method is called.
    * <p>
    * Any objects obtained from the {@link OSMap} are associated with the view. It's important to not hold on to objects
    *  (e.g. {@link Marker}) beyond the view's life. Otherwise it will cause a memory leak as the view cannot be released.
    * <p>
    * Use this class only if you are targeting API 11 and above. Otherwise, use SupportMapFragment.
    */
@TargetApi(11)
public class MapFragment extends Fragment {
	public final OSMapOptions mOptions; 
	public MapView mMapView;
	
	public MapFragment()
	{
		super();
		mOptions = null;
	}
	
	public MapFragment(OSMapOptions options) 
	{
		super();
		mOptions = options;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// This method is final to prevent other methods breaking if this class becomes non-final.
		// If overriding this method is desired, we need to store the MapView in a member field instead of assuming that it is returned by getView().
		// Subclasses can also override getView(), but that obviously causes breakage.
		Context context = getActivity();
		mMapView = new MapView(context, mOptions);
		mMapView.onCreate(savedInstanceState);
		return mMapView;
	}
	
	/**
	 * Gets the underlying {@link OSMap} that is tied to the view wrapped by this fragment.
	 * <p>
	 * @return Returns the OSMap. Null if the view of the fragment is not yet ready. 
	 * This can happen if the fragment lifecyle have not gone through onCreateView(LayoutInflater, ViewGroup, Bundle) yet. 
	 */
	public OSMap getMap()
	{
		return (mMapView == null ? null : mMapView.getMap());
	}
	
	// Lifecycle methods.
	@Override
	public void onResume()
	{
		super.onResume();
		mMapView.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mMapView.onPause();
	}
	
	@Override
	public void onDestroyView() {
		// tchan: Order might matter here!
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroyView();
	}

	/**
	 * Creates a map fragment, using default options.
	 */
	public static MapFragment newInstance()
	{
		return newInstance(null);
	}

	/**
	 * Creates a map fragment with the given options
	 */
	public static MapFragment newInstance(OSMapOptions options) {
		return new MapFragment(options);
	}
}
