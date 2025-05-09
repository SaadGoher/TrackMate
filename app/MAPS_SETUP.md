# Setting Up the Maps Functionality

To use the Maps functionality in TrackMate, you need to set up a Google Maps API key.

## Steps to get a Google Maps API key:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Maps SDK for Android
4. Create credentials to get your API key
5. Add restrictions to your API key (recommended)

## Add your API key to the app:

In `AndroidManifest.xml`, replace "YOUR_API_KEY" with your actual API key:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />
```

For more information, see the [Google Maps Platform documentation](https://developers.google.com/maps/documentation/android-sdk/get-api-key).
