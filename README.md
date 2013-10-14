OS OpenSpace Android SDK
-------

The openspace-android-sdk enables access to [Ordnance Survey](http://www.ordnancesurvey.co.uk/oswebsite/web-services/) Web Map Tile Services (WMTS) for Android devices. It provides access to a number of mapping layers and gazetteer lookups and has a similar API to Google Maps API v2, so that moving from Google mapping to Ordnance Survey mapping is simple, see [Converting](#converting-google-maps).

This SDK is available as a static framework, see the [Getting started](#getting-started) guide for instructions about downloading and importing into your own application or try a [demo app](#demo-projects) to get started quickly.

View available mapping layers [here] (http://www.ordnancesurvey.co.uk/oswebsite/web-services/os-openspace/pro/products.html)


![SimpleDemo-ScreenShot](https://github.com/OrdnanceSurvey/android-sdk-mapping-demo/raw/master/screenshot.png "Screenshot of basic demo app")


#### Features and benefits

Here are some of the features available

- Native Android framework to incorporate Ordnance Survey mapping.
- Select which products are displayed - see [products available](#product-codes).
- Zoom and pan controls - native touch gesture recogonisers provide tap, pinch zoom control, etc.
- Annotations - create and customise annotations.
- Overlays - create and style polylines and polygons.
- Offline tile storage - read [about offline tile packages](#offline-databases).
- Places of interest geocoder - Search 1:50K Gazetteer, OS Locator and Codepoint Open datasets available either online or offline.
- Uses [OSGB36 British National Grid](http://www.ordnancesurvey.co.uk/oswebsite/support/the-national-grid.html) map projection - ordnancesurvey-android-sdk converts between WGS84 latitude/longitude and OSGB36 National Grid easting/northing. Most Classes handle geometry as an OSGB GridPoint and the sdk provides translation between both projections.
- User location - openspace-android-sdk provides a wrapper around the standard location services to easily display your app's user location on the map and use the data.
- Use OpenGL for fast, smooth map rendering.
- Street level mapping features detailed buildings property boundaries and accurate road network.
- World famous countryside and National park mapping featuring accurate tracks, paths and fields.

Here are some of the benefits

- Fully supported by Ordnance Survey – ongoing SDK upgrades, active user forum.
- Online capability – fast rendering of Ordnance Survey maps.
- Offline maps and search capability – as used in OS MapFinder™.
- Fast rendering and smooth panning - for great user experience.
- Complements our service – another way to get our data.

Contents
-------

The openspace-ios-sdk comprises of a couple of items:

1. The openspace-android-sdk framework is downloaded from [www.ordnancesurvey.co.uk](https://www.ordnancesurvey.co.uk/oswebsite/web-services/os-openspace/android-sdk.html) 
2. [Documentation](http://ordnancesurvey.github.io/openspace-android-sdk/) - The documentation for the latest version of openspace-android-sdk in javadoc format


###### Demo projects

Check out a demo project to get started:

* [Simple Map Demo](https://github.com/OrdnanceSurvey/android-sdk-mapping-demo)


Getting started
-------

### Commercial Use Registration and Access

In order to access OS OpenSpace Pro for premium data you must apply for an API key, by registering for a free trial or commercial licence:-

- A free 90 day trial licence (for internal trial and testing)
- A free 90 day trial licence (for external trial and demonstrating)
- A commercial re-use licence 

see http://www.ordnancesurvey.co.uk/oswebsite/web-services/os-openspace/pro/free-trial.html or contact Newbusinessenquiries@ordnancesurvey.co.uk 

### OS OpenSpace Registration and Access

In order to access the service for public facing, non commercial applications with no financial gain you must apply for an API key, by signing up to an OS OpenSpace Developer Agreement:-

#### NOTE: A free Daily Data Limit will apply. 
#### NOTE: For online access only.

See [OS Website](https://openspaceregister.ordnancesurvey.co.uk/osmapapi/register.do) to register.

### For directly licenced customers (for example OS OnDemand). 

Register for an OS OnDemand licence in order to obtain an API key to access the service. See [pricing](http://www.ordnancesurvey.co.uk/oswebsite/web-services/os-ondemand/pricing.html).

###Authentication parameters

When registering for an API key we'll to know the following:

##### App ID (PackageName) of the application that will use the API key.

Example: `uk.co.ordnancesurvey.android.myDemoApp`

Let us know the package name in which you will be using the API key. This is available and configurable when creating an Android project or from `AndroidManifest.xml`.



### Offline databases

These offline databases are extensions to the openspace-android-sdk and can replace online access or supplement it, they can help your application overcome network coverage issues and continue to function wherever your customers are located. 

For more information please refer to specific documentation.

1. OSTile offline [tile packages](https://github.com/OrdnanceSurvey/openspace-sdk-resources#ostile-packages)
2. Offline [Places of interest gazetteer database](https://github.com/OrdnanceSurvey/openspace-sdk-resources#places-of-interest-geocoder-database)


### Download framework package

TODO:


### Import into Android project

TODO


### Dependancies & requirements

##### Android Support Library v4

The openspace-android-sdk requires the Android Support Library v4 but does not include it, please provide an instance from your application.

TODO: if targetting >API 11 do you depend on this?

##### OpenGL ES 2.0

The openspace-android-sdk uses OpenGL ES 2.0 to render the map, please specify this feature in `AndroidManifest.xml` as below.

```xml

<!-- Use OpenGL ES 2.0 -->
<uses-feature
    android:glEsVersion="0x00020000"
    android:required="true" />

```

##### Permissions

Depending on the features used, request the following permissions in `AndroidManifest.xml`.

* `android.permission.INTERNET` is required when accessing online services.
* `android.permission.ACCESS_NETWORK_STATE` is required to check whether data can be downloaded.
* `android.permission.WRITE_EXTERNAL_STORAGE` is required to cache map tile images.
* `android.permission.ACCESS_COARSE_LOCATION` and `android.permission.ACCESS_FINE_LOCATION` are required to receive accurate device location data.


```xml

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

```


### Displaying a map

The simplest method of displaying a map is to add a MapFragment to your activity, for example add the code below to the xml configuration for the activity.

```xml

<fragment
    android:id="@+id/map_fragment"
    class="uk.co.ordnancesurvey.android.maps.MapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
```

The OSMap fragment cannot be initialised on its own and requries a TileSource in order to access content either online or offline.

Open the respective Java class accomanying the activity xml and add an `OSTileSource` to the OSMap instance, in this example we create a new WebTileSource that accesses the online OS OpenSpace service with an API key.

```java


//add to onCreate implementation

MapFragment mf = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment));

OSMap mMap = mf.getMap();

//create list of tileSources
ArrayList<OSTileSource> sources = new ArrayList<OSTileSource>();

//create web tile source with API details
sources.add( mMap.webTileSource( "API_KEY", true/false, null ) );
mMap.setTileSources(sources);


```


### Product Codes

A developer can select which Ordnance Survey mapping products to use by  selecting on of three pre-configured map stacks or customising their app by passing the product codes as and array of strings.

NOTE: Certain products and the Zoom map stack require a commercial licence.

TODO: how do you do this?


#### Full Product list

// OS OpenSpace users can use these mapping products: NOTE: DAILY LIMITS APPLY

- "SV"   // Street view
- "SVR"  // Street view resampled
- "50K"  // 1:50k
- "50KR" // 1:50k resampled
- "VMD"  // Vector Map District
- "VMDR" // Vector Map District resampled
- "250K" // 1:250k
- "250KR"// 1:250k resampled
- "MS"   // 1:1M
- "MSR"  // 1:1M resampled
- "OV2"  // Overview 2
- "OV1"  // Overview 1
- "OV0"  // Overview 0

// Users with an appropriate commercial or direct licence can also select to use these products:

- "VML"  // Vector Map Local
- "VMLR" // Vector Map Local resampled
- "25K"  // 1:25k
- "25KR" // 1:25k resampled
- "50K"  // 1:50k
- "50KR" // 1:50k resampled

// Users with an appropriate commercial or direct licence can also select to use the Zoom stack products - enables consistently styled layers

- "CS00" // "zoomed out"
- "CS01"
- "CS02"
- "CS03"
- "CS04"
- "CS05"
- "CS06"
- "CS07"
- "CS08"
- "CS09"
- "CS10" // "zoomed in"


### Converting Google Maps

TODO

### Versioning

Ordnance Survey will provide and offically support the latest version of the SDK.


API
-------

In this section we will run through some of the important components in the SDK. For more details please see the [reference documentation](http://ordnancesurvey.github.io/openspace-android-sdk/) or any [demo app](#demo-projects) for full application usage. The API is largely similar to Google Maps Android V2 and as such much of the documentation can apply to this SDK.


### OS Map (`OSMap` class)

This is the main class to interact with the map, it provides access to configure and manipulate the map. You cannot instantiate this class, to access it call the `getMap()` method on the `MapFragment` or `MapView` depending on how you are using the SDK in your application.

**NOTE:**

* The `OSMap` class can only be read or modified from the main thread.
* The `OSMap` must be configured with a tile source - see Tilesources below.
* `OSMap` does not support drawing maps outside Great Britain.
* The `OSMap` does not support other Map Types than the standard 2D map, however it does support rendering different Ordnance Survey products.

**DEMO**

See any of the [demo projects](#demo-projects) for working examples

### OS MapFragment (`MapFragment` class)

The `MapFragment` is a container for the OS Map, this is the simplest way of using the SDK in your application as it will initialise the Map and the view and handles the lifecycle requirements.

Configure a MapFragment in an activity, for example add the code below to the xml configuration for the activity.

**NOTE:**

* Use `MapFragment` class if you are targeting API level 11 and above otherwise use the `SupportMapFragment` along with the Android support library.

```xml

<fragment
    android:id="@+id/map_fragment"
    class="uk.co.ordnancesurvey.android.maps.MapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
```

Use the `getMap()` method on `MapFragment` to obtain instance of `OSMap`.


```java

MapFragment mf = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment));

//obtain instance of OSMap from SupportMapFragment
OSMap mMap = mf.getMap();

//configure OSMap...

```

### OS SupportMapFragment (`SupportMapFragment` class)

The `SupportMapFragment` is again, as the `MapFragment` above but necessary if tageting below API level 11 along with the Android support library.


**NOTE:**

* To use `SupportMapFragment` you must provide the Android support library in your build path.


```xml

<fragment
    android:id="@+id/map_fragment"
    class="uk.co.ordnancesurvey.android.maps.SupportMapFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
```

Use the `getMap()` method on `MapFragment` to obtain instance of `OSMap`.


```java

android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
SupportMapFragment mf = (SupportMapFragment)fm.findFragmentById(R.id.map_fragment);

OSMap mMap = mf.getMap();

//configure OSMap...

```

### Tilesources (`OSTileSource` class)

The `OSMap` class requires atleast one online or offline tile source to render a map.

The offline tilesources must be present on the file system in the form of `ostiles` packages. Using online tilesources requires an API key.

```java

//create a Collection to hold tile sources
ArrayList<OSTileSource> sources = new ArrayList<OSTileSource>();

// Load all ".ostiles" files from the SD card.
sources.addAll(mMap.localTileSourcesInDirectory(this, Environment.getExternalStorageDirectory()));

// Fall back to a web tile source.
sources.add(mMap.webTileSource("API_KEY", false, null));

// TODO: are these rendered in order in array???
mMap.setTileSources(sources);


```


**NOTE:**

* To configure product codes, see the [products available](#product-codes) section.


### Markers (`Marker` class & `MarkerOptions` class)

Markers identify single point locations on the map and can be interacted with in the form of touch events and info windows.

To customise the `Marker` appearance and behaviour, the `addMarker` method accepts a configuration object `MarkerOptions`, use this class to alter how the marker behaves and users interact with it.


Add a `Marker` to the map with options.

```java

OSMap mMap = //get OSMap instance

Marker marker = mMap.addMarker(new MarkerOptions()
            .gridPoint(new GridPoint(260899, 354314))
            .title("Snowdon summit")
            .snippet("Congratulations! If you make it to this point, you can always get the train down."));

```


It is possible to respond to touch events from markers, this interaction is done through the `OSMap` class by registering a listener for the callback you are interested in.

To respond to a Marker click event, pass an `OnMarkerClickListener` to the `OSMap` using the `OSMap.setOnMarkerClickListener` method to receive a callback when a user clicks on a marker. Return a boolean to indicate if you have consumed the event and if you want to suppress the default action.

The `OnMarkerDragListener` interface will allow you to receive callbacks for the events surrounding a marker being dragged, the `onMarkerDragStart`, `onMarkerDragEnd` and `onMarkerDrag` methods encapsulate the start, finish and during the drag event. To use an `OnMarkerDragListener` pass to the map using the `OSMap.setOnMarkerDragListener` method.

It is possible to customise info windows by implementing an `InfoWindowAdapter` and using the `OSMap.setInfoWindowAdapter` to pass to the map. This `InfoWindowAdapter` can return a `android.view.View` for either the entire info window (`getInfoWindow`) or just the contents (`getInfoContents`).  To receive callbacks for when an info window is clicked, create an `OnInfoWindowClickListener` and pass to the map using the method `OSMap.setOnInfoWindowClickListener`.


### Shapes

The set of Shapes available allow a wide range of overlays to be applied to the map along with customisation.

The pattern is similar to Markers above, customise the `*Options` object before creating the shape and adding to the map. Similarly the option object requires some geometry to specify the position and extent of the shape on the map.

In the example below we create a square `Polygon` and style it.

```java

OSMap mMap = //get OSMap instance

final GridPoint sw = new GridPoint(437200, 115450);

PolygonOptions rectOptions = new PolygonOptions()
        .add(sw,
             new GridPoint(sw.x, sw.y + 200),
             new GridPoint(sw.x + 200, sw.y + 200),
             new GridPoint(sw.x + 200, sw.y))//no need to close the polygon
        .strokeColor(Color.GREEN)
        .fillColor(0x7F00FF00);

Polygon polygon = mMap.addPolygon(rectOptions);

```

There are existing SDK classes for the following shapes, please see reference documentation for more details:

* Polygon - without interior polygons
* Polyline
* Circle


### Geocoding (`Geocoder` class)

The `Geocoder` class provides offline search facility against the following datasets; 

* [1:50 000 Scale Gazetteer](http://www.ordnancesurvey.co.uk/oswebsite/products/50k-gazetteer/index.html) - Place names
* [Code-Point Open](http://www.ordnancesurvey.co.uk/oswebsite/products/code-point-open/index.html) - Post codes
* [OS Locator](http://www.ordnancesurvey.co.uk/oswebsite/products/os-locator/index.html) - Road names
* Grid Reference

Along with online search facility against the following datasets; 

* [1:50 000 Scale Gazetteer](http://www.ordnancesurvey.co.uk/oswebsite/products/50k-gazetteer/index.html) - Place names
* [Code-Point Open](http://www.ordnancesurvey.co.uk/oswebsite/products/code-point-open/index.html) - Post codes

The `Geocoder` can be created using either an offline database, online API key details or both; just pass in `null` for the parameters that are not used. For example, to create an online only `Geocoder` just pass in an API key.

```java

Geocoder geocoder = new Geocoder(null, "API_Key", true);

```


The product to search is determined by passing a `Geocoder.GeocodeType` into the `Geocoder.geocodeString(String s, GeocodeType geocodeType, GridRect boundingRect, int start, int numResults)` method. For simplicity you can use `Geocoder.GeocodeType.allOffline()` or `Geocoder.GeocodeType.allOnline()` or simply select a `Geocoder.GeocodeType` individually.

Offline searches can be performed within an area by passing a `GridRect`, to search the entire country pass in `null`.

The number of results can be limited by specifying the number (and offset) of results to return. This will be applied individually to each type of search. To return ALL results, set `numResults` to 0.

The `Geocoder` returns a `Results` object that contains a list of `Placemark` as the matched resultset and a list of `GeocodeException` if any. For example, you may retrieve the list of matches as below;

```java

Results results = //perform Geocoder search

List<? extends Placemark> placemarks = results.getPlacemarks()

```

**NOTE:**

* The `Geocoder` class can work with an offline database - See [offline places of interest gazetteer database](https://github.com/OrdnanceSurvey/openspace-sdk-resources#places-of-interest-geocoder-database) 
* The online access requires an API key configured
* There are currently no reverse geocoding facilities

### Geometry

The SDK provides an implementation to translate between ubiquitous WGS84 coordinate system used in many systems include the GPS network and British National Grid.

The SDK class `GridPoint` represents OSGB36 British National Grid easting/northing.

Conversion between WGS84 and `GridPoint` can be performed with the SDK and the `MapProjection` class — applications should not perform unnecessary conversions. Coordinates should be stored in their source coordinate system in order to benefit from future accuracy improvements within the SDK. 

Conversions can be performed via this SDK if required using the functions below;


```java

//grab the default MapProjection in use
MapProjection mapProjection = MapProjection.getDefault();

//convert my WGS84 lat, lng in decimal degress to a GridPoint object in OSGB36
GridPoint gridPointFromLatLng = mapProjection.toGridPoint(50.937773, -1.470603);

//create an array to hold returned 2D point, convert the OSGB36 GridPoint to WGS84(x, y)
double[] latLng2d = new double[2];
mapProjection.fromGridPoint(new GridPoint(437294, 115504), latLng2d);


``` 

The class `GridRect` exists as a representation of a rectangular region on the OS Map.

```java

GridRect su30GridSquare = new GridRect(430000, 100000, 440000, 110000);

```





Issues
--------

For any issues relating to developing with the SDK, obtaining API keys or service problems, please email osopenspace@ordnancesurvey.co.uk



Licence
-------

LIC
