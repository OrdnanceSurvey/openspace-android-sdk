# Development Guide #

---

## Overview ##

This is an in depth guide to developing with the Ordnance Survey SDK and should be used in conjunction with the SDK javadocs.

### Basic Map ###

The basic structure for creating a map is the same for Open, Pro and Custom stacks: 

1. Add the SDK and V4 Support libraries to the `libs` directory 
2. Add required AndroidManifest tags
2. Create and populate a `MapOpts` instance
3. Construct a Provider and add to the `MapOpts` instance
4. Construct a `MapView` using the `MapOpts` instance
5. Add the `MapView` to the layout - the samples directory shows examples of using the MapView alone (e.g. `open.browser`) and as part of a more complicated layout structure (e.g. `os.compare.open`)

The template project has much if this already filled in and can be used as a kickstart for a mapping project (see _Template Project_ below for more information).

### Providers ###

Providers manage the available Ordnance Survey mapping products displayed in the slippy map. For convenience there are three predefined 'stacks':

`OSOpenProvider`  
This is the sample stack as used in the Javascript Openspaces SDK.

`OSProProvider`  
This is the same as the open stack but contains VML instead of SV at the most zoomed in level.

`OSProCSProvider`  
The consistently styled mapping stack.


Information on the products included in each stack can be found in the relevant Javadocs.

In addition to the predefined stacks, clients can create their own by using the `OSProvider` class and specifying the products required in the constructor. So to create a simple open stack using 250K and 50K products (along with their resampled versions), do the following:

    OSProvider myProvider = new OSProvider(
         false,
         apikey, 
         Arrays.asList(
            OSProducts.PRODUCTS.get(OSProducts.P_250KR),
            OSProducts.PRODUCTS.get(OSProducts.P_250K),
            OSProducts.PRODUCTS.get(OSProducts.P_50KR),
            OSProducts.PRODUCTS.get(OSProducts.P_50K)
        )
    );
    
    MapOpts opts = new MapOpts();
    opts.provider = myProvider;

### Template Project ###

Included in the samples is the `template.project` which provides a bare-bones starter project with a single activity that displays an Openspaces map. You can use this as the basis for getting started. Outlined below are key areas to be aware of when developing with the SDK.

The following quickstart will use ANT to build and run the project.

1. Clone the `template.project` 
2. Copy the SDK lib into the `libs` folder
3. Copy the Android support library (usually found at `$ANDROID_HOME/extras/android/support/v4/android-support-v4.jar`) into the `libs` folder
4. Update the project: `$ android update project --name <project_name> --target android-12
--path <path_to_your_project>`. Although we specify `android-12` we use this only to allow hardware acceleration options to be use in the code (see _Hardware Acceleration_ below).
5. To build and run the project (assuming you have an emulator up and running) - `$ ant clean debug install`

---

### Running an Emulator ###

The Android emulator is well known for being slow and we recommend real devices for testing. However, to run an emulator ensure the following are set:

GPS = on  
GPU = on

When running the emulator from the command line:

`$ emulator -no-snapshot -memory 1024 -gpu on -avd <avd>`

Note that snapshots do not work with the gpu setting on.

---

### Versions ###

The SDK has been developed and tested for API level 10 (Gingerbread 2.3.3+) and beyond. However, best performance is achieved using API level 12 (Honeycomb 3.1+) since it was at this revision Android made hardware acceleration more available to developers.	

---

### Android Manifest ###

Below are the required and optional tags for your applications AndroidManifest. For more information about the android manifest, see <http://developer.android.com/guide/topics/manifest/manifest-intro.html>.


###### android.permission.INTERNET #####

__Usage__: required for HTTP/S tile access 

__Tag__: `<uses-permission android:name="android.permission.INTERNET" />`

###### android.permission.VIBRATE #####

__Usage__: optional - haptic feedback is used to signal the user when they can't zoom in or out any further

__Tag__: `<uses-permission android:name="android.permission.VIBRATE" />`


###### android.permission.WRITE\_EXTERNAL\_STORAGE #####

__Usage__: optional - used for tile cache

__Tag__: `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />`


###### android.permission.READ\_EXTERNAL\_STORAGE #####

__Usage__: optional but required if `WRITE_EXTERNAL_STORAGE` is specified

__Tag__: `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />`


###### android.permission.READ\_EXTERNAL\_STORAGE #####

__Usage__: optional but required if `WRITE_EXTERNAL_STORAGE` is specified

__Tag__: `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />`



When writing GPS enabled applications, the following permissions should also be included

`<uses-permission android:name="android.permission.ACCESS_LOCATION_EXTRA_COMMANDS" />`  
`<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`  
`<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`

---

### Hardware Acceleration ###

There are two options for enabling hardware acceleration in your applications:

In the `AndroidManifest.xml` set the attribute `android:hardwareAccelerated="true"` on the `<application …>` tag. This approach is recommended for applications targeting API levels 12+

In your activity holding the `MapView`, add the following in `onCreate`:

    if (android.os.Build.VERSION.SDK_INT >= 11) {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );
    }

This approach is recommended for apps targeting API levels 10+ - however, you must be building your applications against a library supporting API levels 12 or above in order to have access to the constants.

---

### GPS Coords and OSGB Eastings/Northings ###

The SDK includes some utilities to help convert between OSGB Easting/Northings and GPS Lat/Long (WGS84). These can be found in the class `GISUtils`. Refer to the Javadocs for more information.