package de.jacobsjo.androidofflinemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    MapBoxOfflineTileProvider provider;
    int oldZoom = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get a File reference to the MBTiles file.
        if (!isExternalStorageReadable())
            return;

        File myMBTiles = new File(getMapsStorageDir("Maps"),"map.mbtiles");

        // Create an instance of MapBoxOfflineTileProvider.
        provider = new MapBoxOfflineTileProvider(myMBTiles);

        LatLng center = provider.getBounds().getCenter();

      //  lastPosition = new CameraPosition.Builder().target(center).zoom(provider.getMinimumZoom()).build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
            @Override
            public boolean onMyLocationButtonClick()
            {
                if (mMap.getMyLocation() == null) return false;
                LatLng myLocation = new LatLng(mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude());
                int zoom = 13;
                if (provider.getBounds().contains(myLocation)) {
                    myLocation = provider.getBounds().getCenter();
                    zoom = provider.getMinimumZoom();
                }
                CameraUpdate camera_update = CameraUpdateFactory.newLatLngZoom(myLocation,zoom);
                mMap.animateCamera(camera_update);

                return true;
            }
        });
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener(){
            @Override
            public void onCameraChange(CameraPosition position) {

                float zoom = position.zoom;
                boolean updateZoom = false;
                float minZoom = provider.getMinimumZoom();// 8.0f;
                float maxZoom = provider.getMaximumZoom();//

                if (zoom > maxZoom){
                    updateZoom = true;
                    zoom = maxZoom;
                }

                if (zoom < minZoom){
                    updateZoom = true;
                    zoom = minZoom;
                }

                if (zoom % 1 != 0) {
                    updateZoom = true;
                    if (oldZoom>zoom) zoom = (float) Math.floor((double) zoom);
                    else zoom = (float) Math.ceil((double) zoom);
                    zoom = Math.round(zoom);
                } else {
                    oldZoom = (int) zoom;
                }

                if (updateZoom){
                    oldZoom = (int) zoom;
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
                }

            }
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        // Retrieve GoogleMap instance from MapFragment or elsewhere

        // Create new TileOverlayOptions instance.
        TileOverlayOptions opts = new TileOverlayOptions();



        // Set the tile provider on the TileOverlayOptions.
        opts.tileProvider(provider);

        // Add the tile overlay to the map.
        /*TileOverlay overlay =*/ mMap.addTileOverlay(opts);

        // Add a marker in Sydney and move the camera

        LatLng center = provider.getBounds().getCenter();
      //  mMap.addMarker(new MarkerOptions().position(esbjerg).title("Marker in Esbjerg"));
        CameraUpdate camera_update = CameraUpdateFactory.newLatLngZoom(center,provider.getMinimumZoom());
//        CameraUpdate camera_update = CameraUpdateFactory.newCameraPosition(lastPosition);
        mMap.moveCamera(camera_update);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public File getMapsStorageDir(String mapFolderName) {
        // Get the directory for the app's private pictures directory.
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), mapFolderName);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        provider.close();
    }
}
