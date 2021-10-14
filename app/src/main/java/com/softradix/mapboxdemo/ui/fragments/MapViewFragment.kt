package com.softradix.mapboxdemo.ui.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.search.ui.view.SearchBottomSheetView
import com.softradix.mapboxdemo.R
import com.softradix.mapboxdemo.databinding.FragmentMapViewBinding
import com.softradix.mapboxdemo.utils.NetworkUtil
import com.softradix.mapboxdemo.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapViewFragment : Fragment(), PermissionsListener, OnMapReadyCallback {

    private lateinit var mapFragmentMapViewBinding: FragmentMapViewBinding

    private lateinit var mapboxMap: MapboxMap

    private lateinit var receiver: BroadcastReceiver

    private lateinit var client: MapboxDirections

    private lateinit var mapboxNavigation: MapboxNavigation

    private var origin: Point? = null
    private var destination: Point? = null

    private lateinit var mStyle: Style

    private val MARKER_ICON by lazy { "place" }
    private val ROUTE_LAYER_ID by lazy { "route-layer-id" }
    private val ROUTE_SOURCE_ID by lazy { "route-source-id" }
    private val ICON_LAYER_ID by lazy { "icon-layer-id" }
    private val ICON_SOURCE_ID by lazy { "icon-source-id" }
    private val RED_PIN_ICON_ID by lazy { "red-pin-icon-id" }

    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            requireActivity().applicationContext,
            getString(R.string.mapbox_access_token)
        )

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mapFragmentMapViewBinding = FragmentMapViewBinding.inflate(inflater, container, false)
        //initialize mapview view provided by mapbox sdk
        mapFragmentMapViewBinding.mapView.onCreate(savedInstanceState)

        //initialize search view provided by mapbox sdk
        mapFragmentMapViewBinding.searchView.initializeSearch(
            savedInstanceState,
            SearchBottomSheetView.Configuration()
        )

        searchFunction()

        receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                val status = NetworkUtil.getConnectivityStatusString(requireActivity())
                if (status == "3") {
                    Utils.showInternetDialog(requireActivity())
                } else {
                    Utils.hideInternetDialog()
                    mapFragmentMapViewBinding.mapView.getMapAsync(this@MapViewFragment)
                }
            }
        }
        @Suppress("DEPRECATION")
        requireActivity().registerReceiver(
            receiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )



        return mapFragmentMapViewBinding.root
    }

    private fun searchFunction() {
        mapFragmentMapViewBinding.searchView.addOnSearchResultClickListener {
            val coordinates = it.coordinate?.coordinates()
            val lat = coordinates?.get(1)
            val long = coordinates?.get(0)
            directionsStart(lat!!, long!!)
            Log.e("Place", "onCreate:${it.coordinate}")
            Log.e("Maki Icon", "onCreate:${it.makiIcon}")
            viewGoneSearch()
            navigateToPlace(lat, long)
        }
        mapFragmentMapViewBinding.btSearch.setOnClickListener {
            onMapReady(mapboxMap)
            if (mapFragmentMapViewBinding.searchView.visibility == View.GONE) {
                mapFragmentMapViewBinding.searchView.visibility = View.VISIBLE
                (it as Button).apply {
                    text = getString(R.string.close)
                }
            } else {
                viewGoneSearch()
            }
        }
    }

    private fun viewGoneSearch() {
        mapFragmentMapViewBinding.searchView.visibility = View.GONE
        mapFragmentMapViewBinding.btSearch.text = getString(R.string.search)


    }

    private fun navigateToPlace(lat: Double?, long: Double?) {
        val symbolManager = SymbolManager(mapFragmentMapViewBinding.mapView, mapboxMap, mStyle)
        val symbol =
            SymbolOptions().withLatLng(LatLng(lat!!, long!!)).withIconImage(MARKER_ICON)
                .withIconSize(1.3f)
        symbolManager.create(symbol)

        val position = CameraPosition.Builder()
            .target(LatLng(lat, long))
            .zoom(14.0)
            .build()
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 5000)


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            requireActivity(),
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        )
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.style?.let { enableLocationComponent(it) }
        } else {
            Toast.makeText(
                requireActivity(),
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireActivity())) {

            val context: Context
            context = requireActivity()

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(requireActivity())
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(requireActivity(), R.color.purple_200))
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(requireActivity(), loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {

                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                isLocationComponentEnabled = true

                //current location zoom
                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING
                val lat = mapboxMap.locationComponent.lastKnownLocation!!.latitude
                val lng = mapboxMap.locationComponent.lastKnownLocation!!.longitude
                val position = CameraPosition.Builder()
                    .target(LatLng(lat, lng))
                    .zoom(14.0)
                    .tilt(20.0)
                    .build()
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 5000)

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
        mapFragmentMapViewBinding.btSearch.visibility = View.VISIBLE
    }


    override fun onMapReady(mapboxMap: MapboxMap) {

        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS
        ) {
            mStyle = it
            it.addImage(
                MARKER_ICON,
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_marker_icon_blue)!!
            )
            Handler(Looper.getMainLooper()).postDelayed({
                enableLocationComponent(it)
            }, 2500)

        }
    }

    private fun directionsStart(searchLat: Double, searchLng: Double) {
        val lat = mapboxMap.locationComponent.lastKnownLocation!!.latitude
        val lng = mapboxMap.locationComponent.lastKnownLocation!!.longitude

        origin = Point.fromLngLat(lng, lat)
        destination = Point.fromLngLat(searchLng, searchLat)

        initSource(mStyle)
        initLayers(mStyle)

        client = MapboxDirections.builder()
            .origin(origin!!)
            .destination(destination!!)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.mapbox_access_token))
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {

                if (response.body() == null) {
                    return
                } else if (response.body()!!.routes().size < 1) {
                    return
                }

                // Get the directions route
                val currentRoute = response.body()!!.routes()[0]

                Toast.makeText(requireActivity(), "${currentRoute.distance()}", Toast.LENGTH_LONG)
                    .show()

                // Retrieve and update the source designated for showing the directions route
                mapboxMap.getStyle { style ->
                    val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
                    // Create a LineString with the directions route's geometry and
                    // reset the GeoJSON source for the route LineLayer source
                    source?.setGeoJson(
                        LineString.fromPolyline(
                            currentRoute.geometry()!!,
                            Constants.PRECISION_6
                        )
                    )
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Toast.makeText(requireActivity(), "Error: ${throwable.message}", Toast.LENGTH_LONG)
                    .show()
            }
        })

    }

    //here we are
    private fun initSource(loadedMapStyle: Style) {
        val geoJson = GeoJsonSource(ROUTE_SOURCE_ID)
        loadedMapStyle.addSource(geoJson)


        val iconGeoJsonSource = GeoJsonSource(
            ICON_SOURCE_ID, FeatureCollection.fromFeatures(
                arrayOf<Feature>(
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            origin?.longitude()!!,
                            origin?.latitude()!!
                        )
                    ),
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            destination?.longitude()!!,
                            destination?.latitude()!!
                        )
                    )
                )
            )
        )
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)
        loadedMapStyle.addImage(
            RED_PIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                ContextCompat.getDrawable(requireActivity(), R.drawable.ic_marker_icon_blue)
            )!!
        )
        val layer = SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
            PropertyFactory.iconImage(RED_PIN_ICON_ID),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconOffset(arrayOf(0f, -9f))
        )
        loadedMapStyle.addLayer(layer)

    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(receiver)
    }
}