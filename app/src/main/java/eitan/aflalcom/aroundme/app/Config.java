package eitan.aflalcom.aroundme.app;

/**
 * Created by Eitan on 16/12/2016.
 */

public class Config {
    public static final String TAG = "AroundMe";
    public static final long FIND_SUGGESTION_SIMULATED_DELAY = 500;
    public static final int VIEW_PLACE_REQUEST = 1;
    public static final int LOCATION_CODE = 1000;
    public static final int PHONE_CODE = 1001;
    public static final int TURN_GPS = 1002;
    public static final int RETRY_ACTIVITY = 9000;
    public static final int LOCATION_INTERVAL = 5000;
    public static final int LOCATION_INTERVAL_FAST = 2000;
    public static final int MAP_ZOOM_LEVEL = 18;
    public static final String SEARCH_PLACE_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=";
    public static final String SEARCH_PLACE_API = "&key=AIzaSyDhC0g0xJUXXuebdPIWq3P1WjiSRzXDq1M";
}
