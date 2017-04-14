package eitan.aflalcom.aroundme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResult;
import com.google.android.gms.location.places.Places;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import eitan.aflalcom.aroundme.app.RuntimePermissionsActivity;
import eitan.aflalcom.aroundme.model.PlacesModel;
import io.realm.Realm;
import me.relex.circleindicator.CircleIndicator;

import static android.Manifest.permission.CALL_PHONE;
import static eitan.aflalcom.aroundme.app.Config.MAP_ZOOM_LEVEL;
import static eitan.aflalcom.aroundme.app.Config.PHONE_CODE;
import static eitan.aflalcom.aroundme.app.Config.RETRY_ACTIVITY;
import static eitan.aflalcom.aroundme.app.Config.TAG;

public class ViewPlaceActivity extends RuntimePermissionsActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Realm realm;
    boolean isFav;
    private AsyncTask asyncTask;
    List<Bitmap> bitmapList;
    String[] priceLevels;

    String placeId, placeName, placeAddress;
    String phoneNumber = "";
    String placeUrl = "";
    Double placeLatitude, placeLongitude;
    GoogleApiClient googleApiClient;
    ImageView phoneImageView, starPlaceImageView, websiteImageView, streetIconImageView, ratingIconImageView, priceIconImageView;
    ViewPager placeViewPager;
    CircleIndicator indicator;
    ProgressBar progressBar2;
    LinearLayout callLL, saveLL, urlLL;
    TextView placeViewNameTextView, placeViewAddressTextView, placeViewPriceTextView,
            placeViewRateTextView;

    CustomPagerAdapter customPagerAdapter;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_place);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.view_place_activity_title));
        }

        realm = Realm.getDefaultInstance();

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .enableAutoManage(this, this)
                    .build();
        }

        placeId = getIntent().getStringExtra("placeId");
        placeName = getIntent().getStringExtra("placeName");
        placeAddress = getIntent().getStringExtra("placeAddress");
        placeLatitude = getIntent().getDoubleExtra("placeLatitude", 0);
        placeLongitude = getIntent().getDoubleExtra("placeLongitude", 0);
        isFav = getIntent().getBooleanExtra("isFav", false);

        placeViewPager = (ViewPager) findViewById(R.id.placeViewPager);
        indicator = (CircleIndicator) findViewById(R.id.indicator);
        phoneImageView = (ImageView) findViewById(R.id.phoneImageView);
        starPlaceImageView = (ImageView) findViewById(R.id.starPlaceImageView);
        websiteImageView = (ImageView) findViewById(R.id.websiteImageView);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        callLL = (LinearLayout) findViewById(R.id.callLL);
        saveLL = (LinearLayout) findViewById(R.id.saveLL);
        urlLL = (LinearLayout) findViewById(R.id.urlLL);
        placeViewNameTextView = (TextView) findViewById(R.id.placeViewNameTextView);
        placeViewAddressTextView = (TextView) findViewById(R.id.placeViewAddressTextView);
        placeViewPriceTextView = (TextView) findViewById(R.id.placeViewPriceTextView);
        placeViewRateTextView = (TextView) findViewById(R.id.placeViewRateTextView);
        streetIconImageView = (ImageView) findViewById(R.id.streetIconImageView);
        ratingIconImageView = (ImageView) findViewById(R.id.ratingIconImageView);
        priceIconImageView = (ImageView) findViewById(R.id.priceIconImageView);

        bitmapList = new ArrayList<>();

        if (realm.where(PlacesModel.class).equalTo("id", placeId).count() == 1) {
            isFav = true;
        }

        asyncTask = new PhotoTask() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected void onPostExecute(AllPhotos allPhotos) {
                if (allPhotos != null) {
                    for (int i = 0; i < allPhotos.photosCount; i++) {
                        bitmapList.add(allPhotos.bitmap.get(i));
                    }
                }

                customPagerAdapter = new CustomPagerAdapter(ViewPlaceActivity.this, bitmapList);
                placeViewPager.setAdapter(customPagerAdapter);
            }
        }.execute(placeId);

        Places.GeoDataApi.getPlaceById(googleApiClient, placeId)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {
                            final Place myPlace = places.get(0);
                            placeName = myPlace.getName().toString();
                            streetIconImageView.setVisibility(View.VISIBLE);
                            placeViewNameTextView.setText(placeName);
                            placeAddress = myPlace.getAddress().toString();
                            placeViewAddressTextView.setText(placeAddress);
                            if (myPlace.getRating() < 0) {
                                placeViewRateTextView.setVisibility(View.GONE);
                            } else {
                                placeViewRateTextView.setText(getString(R.string.rate_text, String.valueOf(myPlace.getRating())));
                                ratingIconImageView.setVisibility(View.VISIBLE);
                            }

                            if (!myPlace.getPhoneNumber().toString().isEmpty()) {
                                phoneNumber = String.valueOf(myPlace.getPhoneNumber());
                            } else {
                                callLL.setClickable(false);
                                phoneImageView.setImageResource(R.drawable.ic_phone_off);
                            }

                            if (myPlace.getWebsiteUri() != null) {
                                placeUrl = myPlace.getWebsiteUri().toString();
                            } else {
                                urlLL.setClickable(false);
                                websiteImageView.setImageResource(R.drawable.ic_website_off);
                            }

                            int priceLvl = myPlace.getPriceLevel();
                            if (priceLvl == -1) {
                                placeViewPriceTextView.setVisibility(View.GONE);
                            } else {
                                priceLevels = getResources().getStringArray(R.array.price_levels);

                                placeViewPriceTextView.setText(getString(R.string.price_text, priceLevels[priceLvl]));
                                priceIconImageView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.place_not_found), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        places.release();
                    }
                });

        checkIsFav();

        callLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(ViewPlaceActivity.this, CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                } else {
                    ViewPlaceActivity.super.requestAppPermissions(new
                            String[]{Manifest.permission.CALL_PHONE}, R.string.clear, PHONE_CODE);
                }
            }
        });

        saveLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePlace();
            }
        });

        urlLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!placeUrl.equals("")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(placeUrl));
                    startActivity(intent);
                }
            }
        });


    }

    private void checkIsFav() {
        if (isFav) {
            starPlaceImageView.setImageResource(R.drawable.ic_star_on_big);
        } else {
            starPlaceImageView.setImageResource(R.drawable.ic_star_off_big);
        }
    }

    private void savePlace() {
        if (isFav) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(PlacesModel.class).equalTo("id", placeId).findFirst().deleteFromRealm();
                    isFav = false;
                    checkIsFav();
                }
            });
        } else {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    PlacesModel placesModel = realm.createObject(PlacesModel.class, placeId);
                    placesModel.setName(placeName);
                    placesModel.setAddress(placeAddress);
                    placesModel.setLatitude(placeLatitude);
                    placesModel.setLongitude(placeLongitude);
                    placesModel.setFav(true);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    isFav = true;
                    checkIsFav();
                }
            });
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        if (requestCode == PHONE_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

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
            Log.e(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_activity_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        ShareActionProvider myShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        Intent myShareIntent = new Intent(Intent.ACTION_SEND);
        myShareIntent.setType("text/plain");
        myShareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
        myShareIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q="
                + Uri.encode(placeName) + "&ll="
                + placeLatitude + "," + placeLongitude + "&z=" + MAP_ZOOM_LEVEL);
        myShareActionProvider.setShareIntent(myShareIntent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    abstract class PhotoTask extends AsyncTask<String, Void, PhotoTask.AllPhotos> {

        @Override
        protected AllPhotos doInBackground(String... strings) {
            if (strings.length != 1) {
                return null;
            }

            AllPhotos allPhotos = null;

            PlacePhotoMetadataResult result = Places.GeoDataApi
                    .getPlacePhotos(googleApiClient, placeId).await();

            if (result.getStatus().isSuccess()) {
                List<Bitmap> bitmap = new ArrayList<>();
                PlacePhotoMetadataBuffer photoMetadataBuffer = result.getPhotoMetadata();
                if (photoMetadataBuffer.getCount() > 0 && !isCancelled()) {
                    for (int i = 0; i < photoMetadataBuffer.getCount(); i++) {
                        PlacePhotoMetadata photo = photoMetadataBuffer.get(i);
                        Bitmap image = photo.getScaledPhoto(googleApiClient, 500, 500).await().getBitmap();
                        bitmap.add(image);
                    }
                }

                photoMetadataBuffer.release();
                allPhotos = new AllPhotos(bitmap, photoMetadataBuffer.getCount());
            }
            return allPhotos;
        }

        class AllPhotos {

            List<Bitmap> bitmap;
            int photosCount;

            AllPhotos(List<Bitmap> bitmap, int photosCount) {
                this.bitmap = bitmap;
                this.photosCount = photosCount;
            }
        }
    }

    private class CustomPagerAdapter extends PagerAdapter {
        Context mContext;
        LayoutInflater mLayoutInflater;
        List<Bitmap> bitmapList = new ArrayList<>();
        ImageView galleryImageView;

        CustomPagerAdapter(Context context, List<Bitmap> bitmapList) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.bitmapList = bitmapList;
        }

        @Override
        public int getCount() {
            if (bitmapList.size() == 0) {
                return 1;
            }
            return bitmapList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            View itemView = mLayoutInflater.inflate(R.layout.galley_item, container, false);

            galleryImageView = (ImageView) itemView.findViewById(R.id.galleryImageView);

            if (bitmapList.size() == 0) {
                galleryImageView.setImageResource(R.drawable.nophoto);
                indicator.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    placeViewPager.setElevation(0);
                }
            } else {
                galleryImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                for (int i = 0; i < bitmapList.size(); i++) {
                    galleryImageView.setImageBitmap(bitmapList.get(position));
                }
            }

            galleryImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (bitmapList.size() == 0) {
                        return;
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmapList.get(position).compress(Bitmap.CompressFormat.WEBP, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    Intent intent = new Intent(getApplicationContext(), FullScreenImageActivity.class);
                    intent.putExtra("image", byteArray);
                    startActivity(intent);
                }
            });

            placeViewPager.setVisibility(View.VISIBLE);
            progressBar2.setVisibility(View.GONE);

            indicator.setViewPager(placeViewPager);

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}
