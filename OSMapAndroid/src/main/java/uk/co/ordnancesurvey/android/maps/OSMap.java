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
import java.util.Collection;

import android.content.Context;
import android.location.Location;
import android.view.View;

/**
 * This is the main class of the Google Maps Android API and is the entry point for all methods related to the map. You cannot 
 * instantiate an {@link OSMap} object directly, rather, you must obtain one from the {@link MapView#getMap()} method on a 
 * {@link MapFragment} or {@link MapView} that you have added to your application.
 * <p>
 * <b>Note:</b> Similar to a android.view.View View object, an {@link OSMap} can only be read and modified from the main thread. 
 * Calling {@link OSMap} methods from another thread may result in an exception.
 * <p>
 * <b>Developer Guide</b>
 * <p>
 * To get started, read  <a href="https://developers.google.com/maps/documentation/android/">Google Maps Developer Guide</a>
 * {@link OSMap} closely follows the Google Maps Android v2 interface, and therefore the documentation for that API 
 * is the best starting point for users of this class.
 * <p>
 * The following classes in Google Maps have no equivalent in OS Map
 * <ul>
 * 	<li>	GoogleMap.CancelableCallback
 * 	<li>	LocationSource
 * 	<li>	CameraUpdate
 * 	<li>	CameraUpdateFactory
 * 	<li>	MapsInitializer (a map can only be used as part of an activity or a fragment)
 * 	<li>	UiSettings
 *  <li>		CameraPosition.Builder
 * 	<li>	GroundOverlay
 * 	<li>	GroundOverlayOptions
 * 	<li>	LatLng is replaced by GridPoint
 * 	<li>	LatLngBounds
 * 	<li>	LatLngBounds.Builder
 * 	<li>	Tile
 * 	<li>	TileOverlay
 * 	<li>	TileOverlayOptions
 * 	<li>	UrlTileProvider
 * 	<li>	VisibleRegion is replaced by GridRect
 * </ul>
 * <p>
 * In, the following features of Google Maps are not supported
 * <ul>
 *  <li>	z-index for overlays
 *  <li>	map types other than the a normal 2D OS map
 *  <li>	customisable animations
 *  <li>	traffic
 *  <li>	only a perpendicular view is supported 
 * 	<li>	compass
 * 	<li>	zoom controls (zoom gestures are supported, visible controls are not)
 * 	<li>	tilt/rotate
 * 	<li>	my location button
 * </ul>
 */
public interface OSMap {
	/**
	 * Methods on this provider are called when it is time to show an info window for a marker, regardless of the cause 
	 * (either a user gesture or a programmatic call to {@link Marker#showInfoWindow()}. Since there is only one info window shown at 
	 * any one time, this provider may choose to reuse views, or it may choose to create new views on each method invocation.
	 * <p>
	 * When constructing an info-window, methods in this class are called in a defined order. To replace the default info-window, 
	 * override {@link #getInfoWindow(Marker)} with your custom rendering. To replace just the info-window contents, inside the default info-window 
	 * frame (the callout bubble), leave the default implementation of {@link #getInfoWindow(Marker)} in place and override 
	 * {@link #getInfoContents(Marker)} instead.
	 */
	interface InfoWindowAdapter
	{
		/**
		 * 	Provides custom contents for the default info-window frame of a marker. This method is only called if 
		 *  {@link #getInfoWindow(Marker)} first returns null. If this method returns a view, it will be placed inside 
		 *  the default info-window frame. If you change this view after this method is called, those changes will not 
		 *  necessarily be reflected in the rendered info-window. If this method returns null, the default rendering will be used instead.
		 *  
		 *  @param marker	The marker for which an info window is being populated.
		 *  @return		A custom view to display as contents in the info window for marker, or null to use the default content rendering instead.
		 **/
		public abstract View getInfoContents(Marker marker);
		
		/**
		 * Provides a custom info-window for a marker. If this method returns a view, it is used for the entire info-window. 
		 * If you change this view after this method is called, those changes will not necessarily be reflected in the rendered 
		 * info-window. If this method returns null , the default info-window frame will be used, with contents provided by 
		 * {@link #getInfoContents(Marker)}.
		 * @param marker The marker for which an info window is being populated.
		 * @return A custom info-window for marker, or null to use the default info-window frame with custom contents.
		 */
		public abstract View getInfoWindow(Marker marker);
	}
	
	/**
	 * Callback interface for when the user taps on the map. 
	 * <p>
	 * Listeners will be invoked on the main thread.
	 */
	interface OnMapClickListener
	{
		/**
		 * 	Called when the user makes a tap gesture on the map, but only if none of the overlays of the map handled the gesture. 
		 * Implementations of this method are always invoked on the main thread.
		 * 
		 * @param gp The point on the ground (projected from the screen point) that was tapped.
		 */
		public abstract boolean onMapClick(GridPoint gp);		
	}

	/**
	 * Callback interface for when the user long presses on the map. 
	 * <p>
	 * Listeners will be invoked on the main thread.
	 */
	interface OnMapLongClickListener
	{
		/**
		 * Called when the user makes a long-press gesture on the map, but only if none of the overlays of the map handled the gesture. 
		 * Implementations of this method are always invoked on the main thread.
		 * 
 		 * @param gp The point on the ground (projected from the screen point) that was tapped.
		 */
		public abstract void onMapLongClick(GridPoint gp);		
	}

	/**
	 * Defines signatures for methods that are called when a marker is clicked or tapped.
	 */
	interface OnMarkerClickListener
	{
		/**
		 * Called when a marker has been clicked or tapped.
		 * @param marker The marker that was clicked.
		 * @return true if the listener has consumed the event (i.e., the default behavior should not occur), 
		 * false otherwise (i.e., the default behavior should occur). The default behavior is for the camera to move to the map and an info window to appear.
		 */
		public abstract boolean onMarkerClick(Marker marker);
	}
	
	/**
	 * Callback interface for drag events on markers.
	 */
	interface OnMarkerDragListener
	{
		/**
		 * Called repeatedly while a marker is being dragged. The marker's location can be accessed via {@link Marker#getGridPoint()}.
		 * @param marker The marker being dragged
		 */
		public abstract void onMarkerDrag(Marker marker);
		/**
		 * Called when a marker has finished beign dragged. The marker's location can be accessed via {@link Marker#getGridPoint()}.
		 * @param marker The marker being dragged
		 */
		public abstract void onMarkerDragEnd(Marker marker);
		/**
		 * Called when a marker starts being dragged. The marker's location can be accessed via {@link Marker#getGridPoint()}; this position may 
		 * be different to the position prior to the start of the drag because the marker is popped up above the touch point.
		 * @param marker The marker being dragged
		 */
		public abstract void onMarkerDragStart(Marker marker);
	}

	/**
	 * Callback interface for when the My Location dot (which signifies the user's location) changes location.
	 */
	interface OnMyLocationChangeListener
	{
		/**
		 * Called when the Location of the My Location dot has changed (be it gridpoint or accuracy).
		 * @param location The current location of the My Location dot.
		 */
		public abstract void onMyLocationChange(GridPoint location);
	}
	
	/**
	 * Callback interface for click/tap events on a marker's info window.
	 */
	interface OnInfoWindowClickListener
	{
		/**
		 * Called when the marker's info window is clicked.
		 * @param marker	The marker of the info window that was clicked.
		 */
		public abstract void onInfoWindowClick(Marker marker);
	}

	/**
	 * Defines signatures for methods that are called when the camera changes position.
	 */
	interface OnCameraChangeListener
	{
		/**
		 * Called after the camera position has changed. During an animation, this listener may not be notified of intermediate camera positions. 
		 * It is always called for the final position in the animation.
		 * <p>
		 * This is called on the main thread.
		 * @param position	The CameraPosition at the end of the last camera change.
		 */
		public abstract void onCameraChange(CameraPosition position);
	}

	/**
	 * Adds a marker to this map.
	 * <p>The marker's icon is rendered on the map at the location Marker.position. Clicking the marker centers the camera on the marker. 
	 * If Marker.title is defined, the map shows an info box with the marker's title and snippet. If the marker is draggable, 
	 * long-clicking and then dragging the marker moves it.
	 * @param options	A marker options object that defines how to render the marker.
	 * @return The Marker that was added to the map.
	 */
	public Marker addMarker(MarkerOptions options);
	
	/**
	 * Removes all markers, overlays, and polylines from the map.
	 */
	public void clear();

	/**
	 * Adds a polyline to this map.
	 * @param options	A polyline options object that defines how to render the Polyline.
	 * @return The Polyline object that was added to the map.
	 */
	Polyline addPolyline(PolylineOptions options);
	
	/**
	 * Adds a polygon to this map.
	 * @param options A polygon options object that defines how to render the Polygon.
	 * @return The Polygon object that is added to the map.
	 */
	Polygon addPolygon(PolygonOptions options);
	
	/**
	 * Add a circle to this map.
	 * @param options A circle options object that defines how to render the Circle
	 * @return The Circle object that is added to the map
	 */
	Circle addCircle(CircleOptions options);

	/**
	 * Sets a custom renderer for the contents of info windows.
	 * <p>
	 * Like the map's event listeners, this state is not serialized with the map. If the map gets re-created 
	 * (e.g., due to a configuration change), you must ensure that you call this method again in order to preserve the customization.
	 * @param adapter	The adapter to use for info window contents, or null to use the default content rendering in info windows.
	 */
	public void setInfoWindowAdapter(InfoWindowAdapter adapter);
	
	/**
	 * Sets a callback that's invoked when a marker info window is clicked.
	 * @param listener	The callback that's invoked when a marker info window is clicked. To unset the callback, use null.
	 */
	public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener);
	
	/**
	 * Sets a callback that's invoked when the map is tapped
	 * @param listener	The callback that's invoked when the map is tapped. To unset the callback, use null.
	 */
	public void setOnMapClickListener(OnMapClickListener listener);

	/**
	 * Sets a callback that's invoked when the map is long pressed.
	 * @param listener	The callback that's invoked when the map is long pressed. To unset the callback, use null.
	 */
	public void setOnMapLongClickListener(OnMapLongClickListener listener);

	/**
	 * Sets a callback that's invoked when a marker is clicked.
	 * @param listener	The callback that's invoked when a marker is clicked. To unset the callback, use null.
	 */
	public void setOnMarkerClickListener(OnMarkerClickListener listener);
	
	/**
	 * Sets a callback that's invoked when a marker is dragged.
	 * @param listener	The callback that's invoked on marker drag events. To unset the callback, use null.
	 */
	public void setOnMarkerDragListener(OnMarkerDragListener listener);

	/**
	 * Gets the status of the my-location layer.
	 * @return True if the my-location layer is enabled, false otherwise.
	 */
	public boolean isMyLocationEnabled();
	
	/**
	 * Enables or disables the my-location layer.
	 * While enabled, the my-location layer continuously draws an indication of a user's current location, 
	 * @param enabled	True to enable; false to disable.
	 */
	public void setMyLocationEnabled(boolean enabled);
	
	/**
	 * Returns the currently displayed user location, or null if there is no location data available.
	 * @return The currently displayed Location user location.
	 */
	public Location getMyLocation();
	/**
	 * Convenience method to get current user location as a {@link GridPoint}
	 * @return The current user location as a {@link GridPoint}
	 */
	public GridPoint getMyGridPoint();
	
	/**
	 * Sets a callback that's invoked when the my location dot changes location.
	 * @param listener The callback that's invoked when the my location dot changes.
	 */
	public void setOnMyLocationChangeListener(OnMyLocationChangeListener listener);
	
	/* 
	 * Repositions the camera. The move may be animated.
	 * @param camera The new camera position and zoom level
	 * @param animated If the camera move is to be animated, or made instantenously
	 */
	public void moveCamera(CameraPosition camera, boolean animated);	
	
	/**
	 * Sets a callback that's invoked when the camera changes.
	 * @param listener	The callback that's invoked when the camera changes. To unset the callback, use null.
	 */
	public void setOnCameraChangeListener(OnCameraChangeListener listener);

	/**
	 * Replaces the location source of the my-location layer
	 * @param source A location source to use in the my-location layer. Set to null to use the default location source.
	 */
	public void setLocationSource(LocationSource source);
	
	public void setTileSources(Collection<OSTileSource> tileSources);
	
	/**
	 * @param apiKey
	 * @param openSpacePro
	 * @param productsOrNull only fetch products in this list. If null, then this parameter is ignored
	 * @return
	 */
	public OSTileSource webTileSource(String apiKey, boolean openSpacePro, String[] productsOrNull);
	public OSTileSource localTileSource(Context context, File file) throws FailedToLoadException;
	public Collection<OSTileSource> localTileSourcesInDirectory(Context context, File dir);
}
