OS OpenSpace Android SDK
-------

The openspace-android-sdk enables access to [Ordnance Survey](http://www.ordnancesurvey.co.uk/oswebsite/web-services/) Web Map Tile Services (WMTS) for Android devices. It provides access to a number of mapping layers and gazetteer lookups and has a similar API to Google Maps API v2, so that moving from Google mapping to Ordnance Survey mapping is simple, see [Converting](#converting-google-maps).

This SDK is available as a static framework, see the [Getting started](#getting-started) guide for instructions about downloading and importing into your own application or try a [demo app](#demo-projects) to get started quickly.

View available mapping layers [here] (http://www.ordnancesurvey.co.uk/oswebsite/web-services/os-openspace/pro/products.html)


#### Features and benefits

Here are some of the features available


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

TODO - stuff about API keys

### Offline databases

These offline databases are extensions to the openspace-android-sdk and can replace online access or supplement it, they can help your application overcome network coverage issues and continue to function wherever your customers are located. 

For more information please refer to specific documentation.

1. OSTile offline [tile packages](https://github.com/OrdnanceSurvey/openspace-sdk-resources#ostile-packages)
2. Offline [Places of interest gazetteer database](https://github.com/OrdnanceSurvey/openspace-sdk-resources#places-of-interest-geocoder-database)


### Download framework package

TODO


### Import into Android project

TODO


### Dependancies

TODO


### Imports

TODO


### Displaying a map

TODO

### Product Codes

A developer can select which Ordnance Survey mapping products to use by  selecting on of three pre-configured map stacks or customising their app by passing the product codes as and array of strings.

NOTE: Certain products and the Zoom map stack require a commercial licence.


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

To return the version of SDK you are currently using as a string;

```objective-c
NSLog(@"You are currently using SDK Version: %@", [OSMapView SDKVersion]);
```


API
-------

LOADS


Issues
--------

For any issues relating to developing with the SDK, obtaining API keys or service problems, please email osopenspace@ordnancesurvey.co.uk



Licence
-------

LIC
