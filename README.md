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

* Android Support Library v4

The openspace-android-sdk requires the Android Support Library v4 but does not include it, please provide an instance from your application.

* OpenGL ES 2.0

The openspace-android-sdk uses OpenGL ES 2.0 to render the map, please specify this feature in `AndroidManifest.xml` as below.

```xml

<!-- Use OpenGL ES 2.0 -->
<uses-feature
    android:glEsVersion="0x00020000"
    android:required="true" />

```

* Permissions

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

In this section we will run through some of the important components in the SDK. For more details please see the [reference documentation](http://ordnancesurvey.github.io/openspace-android-sdk/) or any [demo app](#demo-projects) for full application usage.


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

* Use `MapFragment` class if you are targetting API level 11 and above otherwise use the `SupportMapFragment` along with the Android support library.

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

//configure OSMap

```

### OS MapView (`MapView` class)

TODO




Issues
--------

For any issues relating to developing with the SDK, obtaining API keys or service problems, please email osopenspace@ordnancesurvey.co.uk



Licence
-------

LIC
