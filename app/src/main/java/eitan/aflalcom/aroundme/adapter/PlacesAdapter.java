package eitan.aflalcom.aroundme.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

import eitan.aflalcom.aroundme.R;
import eitan.aflalcom.aroundme.ViewPlaceActivity;
import eitan.aflalcom.aroundme.model.PlacesModel;
import eitan.aflalcom.aroundme.model.SearchResults;
import io.realm.Realm;
import io.realm.RealmQuery;

import static eitan.aflalcom.aroundme.app.Config.MAP_ZOOM_LEVEL;
import static eitan.aflalcom.aroundme.app.Config.TAG;
import static eitan.aflalcom.aroundme.app.Config.VIEW_PLACE_REQUEST;

/**
 * Created by Eitan on 02/11/2016.
 */

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.ViewHolder> {

    private Realm realm;
    private Activity activity;
    private Context context;
    private List<PlacesModel> placesModelList;
    private SharedPreferences sharedPreferences;
    private String distanceUnitPref;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            distanceUnitPref = sharedPreferences.getString("distance_unit_list", "1");
        }
    }

    private class AroundViewHolder extends ViewHolder {

        ConstraintLayout aroundmeCL;
        ImageView placeStarImageView;
        TextView placeNameTextView, placeAddressTextView, placeDistanceTextView;

        AroundViewHolder(View itemView) {
            super(itemView);

            realm = Realm.getDefaultInstance();
            aroundmeCL = (ConstraintLayout) itemView.findViewById(R.id.aroundmeCL);
            placeStarImageView = (ImageView) itemView.findViewById(R.id.placeStarImageView);
            placeNameTextView = (TextView) itemView.findViewById(R.id.placeNameTextView);
            placeAddressTextView = (TextView) itemView.findViewById(R.id.placeAddressTextView);
            placeDistanceTextView = (TextView) itemView.findViewById(R.id.placeDistanceTextView);
        }
    }

    private class FavsViewHolder extends ViewHolder {

        ConstraintLayout favsCL;
        TextView favsNameTextView, favsAddressTextView, favsDistanceTextView;

        FavsViewHolder(View itemView) {
            super(itemView);

            favsCL = (ConstraintLayout) itemView.findViewById(R.id.favsCL);
            favsNameTextView = (TextView) itemView.findViewById(R.id.favsNameTextView);
            favsAddressTextView = (TextView) itemView.findViewById(R.id.favsAddressTextView);
            favsDistanceTextView = (TextView) itemView.findViewById(R.id.favsDistanceTextView);
        }
    }

    public PlacesAdapter(Activity activity, Context context, List<PlacesModel> placesModelList) {
        this.activity = activity;
        this.context = context;
        this.placesModelList = placesModelList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 1:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.aroundme_cv, parent, false);
                return new AroundViewHolder(view);
            case 2:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.favs_cv, parent, false);
                return new FavsViewHolder(view);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 1:
                final AroundViewHolder aroundViewHolder = (AroundViewHolder) holder;

                if (placesModelList.get(position).isFav()) {
                    aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_on);
                } else {
                    aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_off);
                }

                aroundViewHolder.placeNameTextView.setText(placesModelList.get(position).getName());
                aroundViewHolder.placeAddressTextView.setText(placesModelList.get(position).getAddress());

                float distance = placesModelList.get(holder.getAdapterPosition()).getDistance();
                aroundViewHolder.placeDistanceTextView.setText(getDistanceUnit(distance));

                aroundViewHolder.placeStarImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (placesModelList.get(holder.getAdapterPosition()).isFav()) {
                            aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_off);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.where(PlacesModel.class).equalTo("id", placesModelList.get(holder.getAdapterPosition()).getId()).findFirst().deleteFromRealm();
                                    placesModelList.get(holder.getAdapterPosition()).setFav(false);
                                }
                            });
                        } else {
                            realm.executeTransactionAsync(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    PlacesModel placesModel = realm.createObject(PlacesModel.class, placesModelList.get(holder.getAdapterPosition()).getId());
                                    placesModel.setName(placesModelList.get(holder.getAdapterPosition()).getName());
                                    placesModel.setAddress(placesModelList.get(holder.getAdapterPosition()).getAddress());
                                    placesModel.setLatitude(placesModelList.get(holder.getAdapterPosition()).getLatitude());
                                    placesModel.setLongitude(placesModelList.get(holder.getAdapterPosition()).getLongitude());
                                    placesModel.setFav(true);
                                }
                            }, new Realm.Transaction.OnSuccess() {
                                @Override
                                public void onSuccess() {
                                    aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_on);
                                    placesModelList.get(holder.getAdapterPosition()).setFav(true);
                                }
                            });
                        }
                    }
                });

                aroundViewHolder.aroundmeCL.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(activity, ViewPlaceActivity.class);
                        intent.putExtra("placeId", placesModelList.get(holder.getAdapterPosition()).getId());
                        intent.putExtra("placeName", placesModelList.get(holder.getAdapterPosition()).getName());
                        intent.putExtra("placeAddress", placesModelList.get(holder.getAdapterPosition()).getAddress());
                        intent.putExtra("placeLatitude", placesModelList.get(holder.getAdapterPosition()).getLatitude());
                        intent.putExtra("placeLongitude", placesModelList.get(holder.getAdapterPosition()).getLongitude());
                        intent.putExtra("isFav", placesModelList.get(holder.getAdapterPosition()).isFav());
                        activity.startActivityForResult(intent, VIEW_PLACE_REQUEST);

                        RealmQuery<SearchResults> query = realm.where(SearchResults.class).equalTo("id", placesModelList.get(holder.getAdapterPosition()).getId());
                        if (query.count() > 0) {
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.where(SearchResults.class).equalTo("id", placesModelList.get(holder.getAdapterPosition()).getId()).findAll().deleteAllFromRealm();
                                }
                            });
                        }

                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                SearchResults object = realm.createObject(SearchResults.class, placesModelList.get(holder.getAdapterPosition()).getId());
                                object.setName(placesModelList.get(holder.getAdapterPosition()).getName());
                                object.setAddress(placesModelList.get(holder.getAdapterPosition()).getAddress());
                                object.setLatitude(placesModelList.get(holder.getAdapterPosition()).getLatitude());
                                object.setLongitude(placesModelList.get(holder.getAdapterPosition()).getLongitude());
                                object.setHistory(true);
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Added to history");
                            }
                        });
                    }
                });

                aroundViewHolder.aroundmeCL.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        CharSequence[] items = context.getResources().getStringArray(R.array.place_longclick);
                        new AlertDialog.Builder(activity, R.style.AppTheme_MyAlertDialog)
                                .setTitle("")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int item) {
                                        if (item == 0) {
                                            if (placesModelList.get(holder.getAdapterPosition()).isFav()) {
                                                aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_off);
                                                realm.executeTransaction(new Realm.Transaction() {
                                                    @Override
                                                    public void execute(Realm realm) {
                                                        realm.where(PlacesModel.class).equalTo("id", placesModelList.get(holder.getAdapterPosition()).getId()).findFirst().deleteFromRealm();
                                                        placesModelList.get(holder.getAdapterPosition()).setFav(false);
                                                    }
                                                });
                                            } else {
                                                realm.executeTransactionAsync(new Realm.Transaction() {
                                                    @Override
                                                    public void execute(Realm realm) {
                                                        PlacesModel placesModel = realm.createObject(PlacesModel.class, placesModelList.get(holder.getAdapterPosition()).getId());
                                                        placesModel.setName(placesModelList.get(holder.getAdapterPosition()).getName());
                                                        placesModel.setAddress(placesModelList.get(holder.getAdapterPosition()).getAddress());
                                                        placesModel.setLatitude(placesModelList.get(holder.getAdapterPosition()).getLatitude());
                                                        placesModel.setLongitude(placesModelList.get(holder.getAdapterPosition()).getLongitude());
                                                        placesModel.setFav(true);
                                                    }
                                                }, new Realm.Transaction.OnSuccess() {
                                                    @Override
                                                    public void onSuccess() {
                                                        aroundViewHolder.placeStarImageView.setImageResource(R.drawable.ic_star_on);
                                                        placesModelList.get(holder.getAdapterPosition()).setFav(true);
                                                    }
                                                });
                                            }
                                        } else if (item == 1) {
                                            Intent myShareIntent = new Intent(Intent.ACTION_SEND);
                                            myShareIntent.setType("text/plain");
                                            myShareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name));
                                            myShareIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q="
                                                    + Uri.encode(placesModelList.get(holder.getAdapterPosition()).getName()) + "&ll="
                                                    + placesModelList.get(holder.getAdapterPosition()).getLatitude()
                                                    + "," + placesModelList.get(holder.getAdapterPosition()).getLongitude()
                                                    + "&z=" + MAP_ZOOM_LEVEL);
                                            activity.startActivity(Intent.createChooser(myShareIntent, "Share location"));
                                        } else {
                                            dialog.dismiss();
                                        }
                                    }
                                })
                                .show();
                        return true;
                    }
                });
                break;
            case 2:
                final FavsViewHolder favsViewHolder = (FavsViewHolder) holder;

                favsViewHolder.favsNameTextView.setText(placesModelList.get(holder.getAdapterPosition()).getName());
                favsViewHolder.favsAddressTextView.setText(placesModelList.get(holder.getAdapterPosition()).getAddress());

                float favDistance = placesModelList.get(holder.getAdapterPosition()).getDistance();
                favsViewHolder.favsDistanceTextView.setText(getDistanceUnit(favDistance));

                favsViewHolder.favsCL.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(activity, ViewPlaceActivity.class);
                        intent.putExtra("placeId", placesModelList.get(holder.getAdapterPosition()).getId());
                        intent.putExtra("placeName", placesModelList.get(holder.getAdapterPosition()).getName());
                        intent.putExtra("placeAddress", placesModelList.get(holder.getAdapterPosition()).getAddress());
                        intent.putExtra("placeLatitude", placesModelList.get(holder.getAdapterPosition()).getLatitude());
                        intent.putExtra("placeLongitude", placesModelList.get(holder.getAdapterPosition()).getLongitude());
                        intent.putExtra("isFav", placesModelList.get(holder.getAdapterPosition()).isFav());
                        activity.startActivityForResult(intent, VIEW_PLACE_REQUEST);
                    }
                });
                break;
        }
    }

    private String getDistanceUnit(float distance) {
        String distanceUnit;

        if (distanceUnitPref.equals("1")) {
            if (distance > 100.0F) {
                distance /= 1000;
                distance = Float.valueOf(new DecimalFormat("#.#").format(distance));
                distanceUnit = " km";
            } else {
                distanceUnit = " m";
                return (int) distance + distanceUnit;
            }
        } else {
            if (distance >= 1609.345F) {
                distance /= 1609.345F;
                distance = Float.valueOf(new DecimalFormat("#.#").format(distance));
                distanceUnit = " mi";
            } else {
                distance *= 1.0936133F;
                distanceUnit = " yd";
                return (int) distance + distanceUnit;
            }
        }

        return distance + distanceUnit;
    }

    @Override
    public int getItemCount() {
        return placesModelList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return placesModelList.get(position).getType();
    }

    public void clearData() {
        int size = this.placesModelList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                this.placesModelList.remove(0);
            }

            this.notifyItemRangeRemoved(0, size);
        }
    }
}
