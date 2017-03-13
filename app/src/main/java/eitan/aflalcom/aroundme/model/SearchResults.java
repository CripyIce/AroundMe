package eitan.aflalcom.aroundme.model;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by Eitan on 24/12/2016.
 */

@RealmClass
public class SearchResults implements SearchSuggestion, RealmModel {
    @PrimaryKey
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private boolean history;

    public SearchResults() {

    }

    public SearchResults(String id, String name, String address, double latitude, double longitude, boolean history) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.history = history;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public SearchResults(Parcel source) {
        this.name = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(address);
    }

    @Override
    public String getBody() {
        return name;
    }

    public static final Creator<SearchResults> CREATOR = new Creator<SearchResults>() {
        @Override
        public SearchResults createFromParcel(Parcel in) {
            return new SearchResults(in);
        }

        @Override
        public SearchResults[] newArray(int size) {
            return new SearchResults[size];
        }
    };
}
