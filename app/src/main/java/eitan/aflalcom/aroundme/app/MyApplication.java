package eitan.aflalcom.aroundme.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Eitan on 12/11/2016.
 */

public class MyApplication extends Application {

    SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Boolean dayNight = preferences.getBoolean("daynight_theme", false);

        if (dayNight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
