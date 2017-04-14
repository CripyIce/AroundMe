package eitan.aflalcom.aroundme;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eitan.aflalcom.aroundme.adapter.PlacesAdapter;
import eitan.aflalcom.aroundme.app.RuntimePermissionsActivity;
import eitan.aflalcom.aroundme.model.PlacesModel;
import eitan.aflalcom.aroundme.model.SearchResults;
import io.realm.Realm;
import io.realm.RealmQuery;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static eitan.aflalcom.aroundme.app.Config.FIND_SUGGESTION_SIMULATED_DELAY;
import static eitan.aflalcom.aroundme.app.Config.LOCATION_CODE;
import static eitan.aflalcom.aroundme.app.Config.LOCATION_INTERVAL;
import static eitan.aflalcom.aroundme.app.Config.LOCATION_INTERVAL_FAST;
import static eitan.aflalcom.aroundme.app.Config.MAP_ZOOM_LEVEL;
import static eitan.aflalcom.aroundme.app.Config.RETRY_ACTIVITY;
import static eitan.aflalcom.aroundme.app.Config.SEARCH_PLACE_API;
import static eitan.aflalcom.aroundme.app.Config.SEARCH_PLACE_URL;
import static eitan.aflalcom.aroundme.app.Config.TAG;
import static eitan.aflalcom.aroundme.app.Config.TURN_GPS;
import static eitan.aflalcom.aroundme.app.Config.VIEW_PLACE_REQUEST;

public class MainActivity extends RuntimePermissionsActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener, GoogleApiClient.OnConnectionFailedListener
        , LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnInfoWindowClickListener {

    Realm realm;

    SharedPreferences preferences;

    String mapStyle;
    Boolean dayNight;

    Boolean focusLocation = true;
    Boolean fabRaised = false;
    String placeSelectedId;
    int whatIsOpen = 0;

    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    MapFragment mapFragment;
    GoogleMap googleMap;
    LatLng latLng;
    Location currentLocation, placeLocation, lastLocation;

    List<PlacesModel> placesModelsList;
    PlacesAdapter placesAdapter;

    BottomSheetBehavior behavior;
    FloatingActionButton myLocationFAB, aroundMeFAB;
    FloatingSearchView floatingSearchView;
    TextView bottomSheetTextView, bottomSheetHideTextView, noContentTextView;
    ProgressBar placesProgressBar;
    RecyclerView rv;
    CardView chargingView;

    ViewGroup.MarginLayoutParams bottomSheetTextViewLayoutParams, aroundMeFABLayoutParams;

    List<SearchResults> searchResultsList;

    HandlerThread mHandlerThread;
    Handler mThreadHandler;

    PowerConnectionReceiver powerConnectionReceiver;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinator);
        final View bottomSheet = coordinator.findViewById(R.id.bottom_sheet);
        myLocationFAB = (FloatingActionButton) findViewById(R.id.myLocationFAB);
        aroundMeFAB = (FloatingActionButton) findViewById(R.id.aroundMeFAB);
        floatingSearchView = (FloatingSearchView) findViewById(R.id.floatingSearchView);
        bottomSheetTextView = (TextView) findViewById(R.id.bottomSheetTextView);
        bottomSheetHideTextView = (TextView) findViewById(R.id.bottomSheetHideTextView);
        noContentTextView = (TextView) findViewById(R.id.noContentTextView);
        placesProgressBar = (ProgressBar) findViewById(R.id.placesProgressBar);
        rv = (RecyclerView) findViewById(R.id.rv);
        chargingView = (CardView) findViewById(R.id.chargingView);

        bottomSheetTextViewLayoutParams = (ViewGroup.MarginLayoutParams) bottomSheetTextView.getLayoutParams();
        aroundMeFABLayoutParams = (ViewGroup.MarginLayoutParams) aroundMeFAB.getLayoutParams();

        realm = Realm.getDefaultInstance();

        searchResultsList = new ArrayList<>();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mapStyle = preferences.getString("map_style_list", "1");
        dayNight = preferences.getBoolean("daynight_theme", false);

        currentLocation = new Location("currentLocation");
        placeLocation = new Location("placeLocation");

        if (mThreadHandler == null) {
            mHandlerThread = new HandlerThread(MainActivity.class.getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();

            mThreadHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        Toast.makeText(getApplicationContext(), "Worked!", Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(LOCATION_INTERVAL)
                    .setFastestInterval(LOCATION_INTERVAL_FAST);

            turnGPSDialog();
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MainActivity.super.requestAppPermissions(new
                    String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, R.string.clear, LOCATION_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            floatingSearchView.setZ(999);
        }

        floatingSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.search_favs_menu:
                        bottomSheetTextView.setText(getString(R.string.favorites));
                        behavior.setPeekHeight(bottomSheetTextView.getHeight() + bottomSheetTextViewLayoutParams.bottomMargin + bottomSheetTextViewLayoutParams.topMargin);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        getFavorites();

                        if (!fabRaised) {
                            aroundMeFABLayoutParams.setMargins(aroundMeFABLayoutParams.leftMargin,
                                    aroundMeFABLayoutParams.topMargin, aroundMeFABLayoutParams.rightMargin,
                                    aroundMeFABLayoutParams.bottomMargin + bottomSheetTextView.getHeight() + bottomSheetTextViewLayoutParams.bottomMargin + bottomSheetTextViewLayoutParams.topMargin);
                            aroundMeFAB.setLayoutParams(aroundMeFABLayoutParams);
                            fabRaised = true;
                        }
                        break;
                    case R.id.search_settings_menu:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        break;
                }
            }
        });

        floatingSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    floatingSearchView.clearSuggestions();
                    floatingSearchView.hideProgress();
                    getSearchHistory();
                } else {
                    floatingSearchView.showProgress();
                    mThreadHandler.removeCallbacksAndMessages(null);

                    mThreadHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            getPlaceSearch(newQuery);
                        }
                    }, FIND_SUGGESTION_SIMULATED_DELAY);
                }
            }
        });

        floatingSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            @SuppressWarnings("deprecation")
            public void onBindSuggestion(View suggestionView, ImageView leftIcon, TextView textView, SearchSuggestion item, int itemPosition) {
                SearchResults searchResults = (SearchResults) item;

                String textLight = dayNight ? "#bfbfbf" : "#787878";
                String htmlText;

                if (searchResults.getId().equals("") && searchResults.getAddress().equals("")) {
                    htmlText = "<font color=\"" + textLight + "\">" + searchResults.getBody() + "</font>";
                } else if (floatingSearchView.getQuery().isEmpty()) {
                    htmlText = "<b><font color=\"" + textLight + "\">" + searchResults.getBody() + "</font></b>"
                            + "<br/><font color=\"" + textLight + "\">" + searchResults.getAddress() + "</font>";
                } else {
                    htmlText = searchResults.getBody().replaceFirst(autoCapitalize(floatingSearchView.getQuery()),
                            "<b><font color=\"" + textLight + "\">" + autoCapitalize(floatingSearchView.getQuery()) + "</font></b>");
                    htmlText = htmlText + "<br/><font color=\"" + textLight + "\">" + searchResults.getAddress() + "</font>";
                }

                if (searchResults.isHistory()) {
                    leftIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_history, null));
                } else {
                    leftIcon.setImageDrawable(null);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    textView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT));
                } else {
                    textView.setText(Html.fromHtml(htmlText));
                }

            }
        });

        floatingSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                final SearchResults results = (SearchResults) searchSuggestion;

                if (results.getId().equals("") && results.getAddress().equals("")) {
                    floatingSearchView.clearQuery();
                    results.setName("");
                    floatingSearchView.setSearchText("");
                    return;
                }

                focusLocation = false;
                myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location, null));

                LatLng placeLatLng = new LatLng(results.getLatitude(), results.getLongitude());

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(placeLatLng)
                        .title(results.getName())
                        .snippet(results.getAddress()));
                marker.setTag(0);
                marker.showInfoWindow();
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(placeLatLng));
                placeSelectedId = results.getId();

                RealmQuery<SearchResults> query = realm.where(SearchResults.class).equalTo("id", results.getId());
                if (query.count() > 0) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.where(SearchResults.class).equalTo("id", results.getId()).findAll().deleteAllFromRealm();
                        }
                    });
                }

                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        SearchResults object = realm.createObject(SearchResults.class, results.getId());
                        object.setName(results.getName());
                        object.setAddress(results.getAddress());
                        object.setLatitude(results.getLatitude());
                        object.setLongitude(results.getLongitude());
                        object.setHistory(true);
                    }
                }, new Realm.Transaction.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added to history");
                    }
                });
            }

            @Override
            public void onSearchAction(String currentQuery) {
                if (currentQuery.equals("")) {
                    floatingSearchView.clearSuggestions();
                    getSearchHistory();
                } else {
                    getPlaceSearch(currentQuery);
                }
            }
        });

        floatingSearchView.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {
                getSearchHistory();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    aroundMeFAB.setVisibility(View.GONE);
                    myLocationFAB.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFocusCleared() {
                floatingSearchView.hideProgress();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    aroundMeFAB.setVisibility(View.VISIBLE);
                    myLocationFAB.setVisibility(View.VISIBLE);
                }
            }
        });

        myLocationFAB.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                if (!focusLocation) {
                    turnGPSDialog();
                } else {
                    focusLocation = false;
                    myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location, null));
                }
            }
        });

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setHideable(true);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetHideTextView.setText(getString(R.string.hide));
                } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetHideTextView.setText(getString(R.string.close));
                } else if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetHideTextView.setText(getString(R.string.close));
                    whatIsOpen = 0;
                    if (fabRaised) {
                        aroundMeFABLayoutParams.setMargins(aroundMeFABLayoutParams.leftMargin,
                                aroundMeFABLayoutParams.topMargin, aroundMeFABLayoutParams.rightMargin,
                                aroundMeFABLayoutParams.bottomMargin - bottomSheetTextView.getHeight() - bottomSheetTextViewLayoutParams.bottomMargin - bottomSheetTextViewLayoutParams.topMargin);
                        aroundMeFAB.setLayoutParams(aroundMeFABLayoutParams);
                        fabRaised = false;
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        bottomSheetTextView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                behavior.setState(checkBottomSheetClose());
            }
        });

        bottomSheetHideTextView.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                behavior.setState(checkBottomSheetClose());
            }
        });

        aroundMeFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetTextView.setText(getString(R.string.places_around_you));
                behavior.setPeekHeight(bottomSheetTextView.getHeight() + bottomSheetTextViewLayoutParams.bottomMargin + bottomSheetTextViewLayoutParams.topMargin);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                getPlacesAroundMe();

                if (!fabRaised) {
                    aroundMeFABLayoutParams.setMargins(aroundMeFABLayoutParams.getMarginStart(),
                            aroundMeFABLayoutParams.topMargin, aroundMeFABLayoutParams.getMarginEnd(),
                            aroundMeFABLayoutParams.bottomMargin + bottomSheetTextView.getHeight() + bottomSheetTextViewLayoutParams.bottomMargin + bottomSheetTextViewLayoutParams.topMargin);
                    aroundMeFAB.setLayoutParams(aroundMeFABLayoutParams);
                    fabRaised = true;
                }
            }
        });

        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        placesModelsList = new ArrayList<>();
        placesAdapter = new PlacesAdapter(MainActivity.this, getApplicationContext(), placesModelsList);

        powerConnectionReceiver = new PowerConnectionReceiver();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(powerConnectionReceiver, ifilter);
    }

    private void getSearchHistory() {
        if (realm.where(SearchResults.class).count() != 0) {
            searchResultsList.clear();
            floatingSearchView.clearSuggestions();

            for (SearchResults searchResults : realm.where(SearchResults.class).findAll()) {
                searchResultsList.add(new SearchResults(searchResults.getId(), searchResults.getName()
                        , searchResults.getAddress(), searchResults.getLatitude(), searchResults.getLongitude(), true));
            }

            floatingSearchView.swapSuggestions(searchResultsList);
        }
    }

    private String autoCapitalize(String strings) {
        String[] strArray = strings.trim().split(" ");
        StringBuilder builder = new StringBuilder();

        for (String s : strArray) {
            if (s.length() == 1) {
                builder.append(s.toUpperCase());
            } else if (s.length() > 1) {
                String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
                builder.append(cap + " ");
            }
        }
        return builder.toString();
    }

    private void getPlaceSearch(final String place) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (SEARCH_PLACE_URL + Uri.encode(place) + "&location=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude()
                        + "&radius=13" + SEARCH_PLACE_API, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            searchResultsList.clear();
                            floatingSearchView.clearSuggestions();

                            if (response.getString("status").equals("ZERO_RESULTS")) {
                                searchResultsList.add(new SearchResults("", getString(R.string.no_search_results), "", 0, 0, false));
                                floatingSearchView.swapSuggestions(searchResultsList);
                                floatingSearchView.hideProgress();
                                return;
                            }

                            JSONArray arr = response.getJSONArray("results");

                            boolean isHistory;

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject jsonObject = arr.getJSONObject(i);

                                isHistory = (realm.where(SearchResults.class).equalTo("id", jsonObject.getString("place_id")).count() == 1);

                                searchResultsList.add(new SearchResults(
                                        jsonObject.getString("place_id"),
                                        jsonObject.getString("name"),
                                        jsonObject.getString("formatted_address"),
                                        Double.valueOf(jsonObject.getJSONObject("geometry").getJSONObject("location").getString("lat")),
                                        Double.valueOf(jsonObject.getJSONObject("geometry").getJSONObject("location").getString("lng")),
                                        isHistory
                                ));
                            }

                            floatingSearchView.swapSuggestions(searchResultsList);
                            floatingSearchView.hideProgress();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        searchResultsList.clear();
                        searchResultsList.add(new SearchResults("", getString(R.string.no_search_results), "", 0, 0, false));
                        floatingSearchView.swapSuggestions(searchResultsList);
                        floatingSearchView.hideProgress();
                        error.printStackTrace();
                    }
                });

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(jsonRequest);
    }

    private int checkBottomSheetClose() {
        int i = 0;

        if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            i = BottomSheetBehavior.STATE_HIDDEN;
        } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            i = BottomSheetBehavior.STATE_COLLAPSED;
        } else if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            i = BottomSheetBehavior.STATE_EXPANDED;
        }

        return i;
    }

    private void getPlacesAroundMe() {
        placesAdapter.clearData();
        rv.setVisibility(View.GONE);
        placesProgressBar.setVisibility(View.VISIBLE);
        noContentTextView.setVisibility(View.GONE);
        whatIsOpen = 1;

        if (ActivityCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(googleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    if (likelyPlaces.getCount() == 0 || !isNetworkConnected()) {
                        noContentTextView.setVisibility(View.VISIBLE);
                        noContentTextView.setText(getString(R.string.no_places_found));
                        placesProgressBar.setVisibility(View.GONE);
                        return;
                    }

                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        placeLocation.setLatitude(placeLikelihood.getPlace().getLatLng().latitude);
                        placeLocation.setLongitude(placeLikelihood.getPlace().getLatLng().longitude);

                        boolean fav = false;
                        if (realm.where(PlacesModel.class).equalTo("id", placeLikelihood.getPlace().getId()).count() == 1) {
                            fav = true;
                        }

                        placesModelsList.add(new PlacesModel(placeLikelihood.getPlace().getId(),
                                String.valueOf(placeLikelihood.getPlace().getName()), String.valueOf(placeLikelihood.getPlace().getAddress()),
                                String.valueOf(placeLikelihood.getPlace().getPhoneNumber()), placeLikelihood.getPlace().getPriceLevel(), placeLikelihood.getPlace().getRating(),
                                String.valueOf(placeLikelihood.getPlace().getWebsiteUri()), fav, false,
                                placeLikelihood.getPlace().getLatLng().latitude, placeLikelihood.getPlace().getLatLng().longitude,
                                1, (int) currentLocation.distanceTo(placeLocation)));
                    }
                    likelyPlaces.release();

                    Collections.sort(placesModelsList, new Comparator<PlacesModel>() {
                        @Override
                        public int compare(PlacesModel placesModel, PlacesModel t1) {
                            return Integer.valueOf(placesModel.getDistance()).compareTo(t1.getDistance());
                        }
                    });

                    rv.setHasFixedSize(true);
                    rv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    rv.setAdapter(placesAdapter);
                    placesAdapter.notifyDataSetChanged();

                    placesProgressBar.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void getFavorites() {
        placesAdapter.clearData();
        rv.setVisibility(View.GONE);
        placesProgressBar.setVisibility(View.VISIBLE);
        noContentTextView.setVisibility(View.GONE);
        whatIsOpen = 2;

        int calcDistance;

        if (realm.where(PlacesModel.class).count() == 0) {
            noContentTextView.setVisibility(View.VISIBLE);
            noContentTextView.setText(getString(R.string.no_favorites));
            placesProgressBar.setVisibility(View.GONE);
            return;
        }

        for (PlacesModel placesModel : realm.where(PlacesModel.class).findAll()) {
            Location temp = new Location("");
            temp.setLatitude(placesModel.getLatitude());
            temp.setLongitude(placesModel.getLongitude());
            calcDistance = (int) currentLocation.distanceTo(temp);
            placesModelsList.add(new PlacesModel(placesModel.getId(),
                    String.valueOf(placesModel.getName()), placesModel.getAddress(),
                    calcDistance, true, placesModel.isHistory(), placesModel.getLatitude(), placesModel.getLongitude(), 2));
        }

        Collections.sort(placesModelsList, new Comparator<PlacesModel>() {
            @Override
            public int compare(PlacesModel placesModel, PlacesModel t1) {
                return Integer.valueOf(placesModel.getDistance()).compareTo(t1.getDistance());
            }
        });

        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        rv.setAdapter(placesAdapter);
        placesAdapter.notifyDataSetChanged();

        placesProgressBar.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
    }

    private void turnGPSDialog() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        focusLocation = true;
                        myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location_2, null));
                        handleNewLocation(currentLocation);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, TURN_GPS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error with turning GPS on.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case VIEW_PLACE_REQUEST:
                if (whatIsOpen == 1) {
                    getPlacesAroundMe();
                } else if (whatIsOpen == 2) {
                    getFavorites();
                }
                break;
            case TURN_GPS:
                if (resultCode == RESULT_OK) {
                    focusLocation = true;
                    myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location_2, null));
                    handleNewLocation(currentLocation);
                } else if (resultCode == RESULT_CANCELED) {
                    focusLocation = false;
                    myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location, null));
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                this.googleMap = googleMap;
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.setOnPoiClickListener(this);
                googleMap.setOnCameraMoveStartedListener(this);
                googleMap.setOnInfoWindowClickListener(this);

                int rawId = 0;
                switch (mapStyle) {
                    case "2":
                        rawId = R.raw.map_style_night;
                        break;
                    case "3":
                        rawId = R.raw.map_style_silver;
                        break;
                    case "4":
                        rawId = R.raw.map_style_retro;
                        break;
                    case "5":
                        rawId = R.raw.map_style_dark;
                        break;
                    case "6":
                        rawId = R.raw.map_style_aubergine;
                        break;
                }
                if (rawId != 0) {
                    if (!googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, rawId))) {
                        Log.e(TAG, "Style parsing failed.");
                    }
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RETRY_ACTIVITY);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {
        if (!isNetworkConnected()) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ViewPlaceActivity.class);
        intent.putExtra("placeId", pointOfInterest.placeId);
        intent.putExtra("placeName", pointOfInterest.name);
        intent.putExtra("placeLatitude", pointOfInterest.latLng.latitude);
        intent.putExtra("placeLongitude", pointOfInterest.latLng.longitude);
        startActivity(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (focusLocation) {
            handleNewLocation(location);
        }
    }

    private void handleNewLocation(Location location) {
        if (currentLocation != null) {
            if (behavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                checkLocBeforeRefresh(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        }

        if (googleMap != null) {

            currentLocation = location;

            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();
            latLng = new LatLng(currentLatitude, currentLongitude);

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM_LEVEL));
        }
    }

    private void checkLocBeforeRefresh(LatLng latLng) {
        if (this.latLng != null) {
            if (Math.abs(latLng.latitude - this.latLng.latitude) > 0.001 ||
                    Math.abs(latLng.longitude - this.latLng.longitude) > 0.001) {
                if (whatIsOpen != 2) {
                    getPlacesAroundMe();
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

        if (lastLocation != null) {
            LatLng lastKnownLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, MAP_ZOOM_LEVEL));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }

        try {
            unregisterReceiver(powerConnectionReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        if (googleApiClient.isConnected()) {
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        super.onResume();
    }

    @Override
    public void onCameraMoveStarted(int i) {
        if (i == REASON_GESTURE) {
            focusLocation = false;
            myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location, null));
        }
    }

    @Override
    public void onBackPressed() {
        if (floatingSearchView.isSearchBarFocused()) {
            floatingSearchView.clearSearchFocus();
            return;
        }

        if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!isNetworkConnected()) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        Integer clickCount = (Integer) marker.getTag();

        if (clickCount != null) {
            clickCount += 1;
            marker.setTag(clickCount);

            Intent intent = new Intent(this, ViewPlaceActivity.class);
            intent.putExtra("placeId", placeSelectedId);
            intent.putExtra("placeLatitude", marker.getPosition().latitude);
            intent.putExtra("placeLongitude", marker.getPosition().longitude);
            startActivity(intent);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public void onPermissionsGranted(final int requestCode) {
        if (requestCode == LOCATION_CODE) {
            mapFragment.getMapAsync(this);
            focusLocation = true;
            myLocationFAB.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_my_location_2, null));
            handleNewLocation(currentLocation);
        }
    }

    private class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            if (isCharging) {
                chargingView.setVisibility(View.VISIBLE);
            } else {
                chargingView.setVisibility(View.GONE);
            }
        }
    }
}
