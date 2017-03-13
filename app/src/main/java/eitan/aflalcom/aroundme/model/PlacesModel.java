package eitan.aflalcom.aroundme.model;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Eitan on 04/11/2016.
 */

public class PlacesModel extends RealmObject {
    @PrimaryKey
    private String id;
    private String name;
    private String address;
    private String phone;
    private int priceLvl;
    private float rate;
    private String url;
    private boolean fav;
    private boolean history;
    private double latitude;
    private double longitude;
    private int type;
    @Ignore
    private int distance;

    public PlacesModel(String id, String name, String address, String phone, int priceLvl, float rate, String url, boolean fav, boolean history, double latitude, double longitude, int type, int distance) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.priceLvl = priceLvl;
        this.rate = rate;
        this.url = url;
        this.fav = fav;
        this.history = history;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.distance = distance;
    }

    public PlacesModel(String id, String name, String address, int distance, boolean fav, boolean history, double latitude, double longitude, int type) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.distance = distance;
        this.fav = fav;
        this.history = history;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getPriceLvl() {
        return priceLvl;
    }

    public void setPriceLvl(int priceLvl) {
        this.priceLvl = priceLvl;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public boolean isFav() {
        return fav;
    }

    public void setFav(boolean fav) {
        this.fav = fav;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public PlacesModel() {
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
}
