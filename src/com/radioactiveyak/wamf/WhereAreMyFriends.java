package com.radioactiveyak.wamf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * List based Activity screen that displays your 
 * current address and a list of each of your contacts
 * and your distance from them.
 * 
 * @author Reto Meier
 * Author of Professional Android Application Development
 * http://www.amazon.com/gp/product/0470344717?tag=interventione-20
 *
 */
public class WhereAreMyFriends extends Activity {

  private HashMap<String, Location> friendLocations;
  private ArrayList<String> friendDistanceList;
  private Location currentLocation;
  private ArrayAdapter<String> aa;
  private LocationManager locationManager;
  private Criteria criteria;

  static final private int MENU_ITEM_MAP = Menu.FIRST;
  static final private int MENU_ITEM_REFRESH = Menu.FIRST + 1;

  /**
   * Refresh the hash of contact names / physical locations.
   */
  public void refreshFriendLocations() {
    friendLocations = FriendLocationLookup.GetFriendLocations(getApplicationContext());
  }
  
	@Override
	public void onCreate(Bundle icicle) {
	  super.onCreate(icicle);
	  setContentView(R.layout.main);
	  
	  // Bind the ListView to an ArrayList of strings.
	  friendDistanceList = new ArrayList<String>();
	  ListView lv = (ListView)findViewById(R.id.myListView);
	  aa = new ArrayAdapter<String>(getApplicationContext(), 
	                                android.R.layout.simple_list_item_1,
	                                friendDistanceList);
	  lv.setAdapter(aa);
	  
    // Get a reference to the LocationManager.	  
    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

    // Define a set of criteria used to select a location provider.
    criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_FINE);
    criteria.setAltitudeRequired(false);
    criteria.setBearingRequired(false);
    criteria.setCostAllowed(true);
    criteria.setPowerRequirement(Criteria.POWER_LOW);
    
    // Refresh the hash of contact locations.
    refreshFriendLocations();
	}

  private final LocationListener locationListener = new LocationListener() {
    public void onLocationChanged(Location location) {
      updateWithNewLocation(location);
    }
   
    public void onProviderDisabled(String provider){
      updateWithNewLocation(null);
    }

    public void onProviderEnabled(String provider) {}

    public void onStatusChanged(String provider, int status, Bundle extras) {}
  };
  
  /**
   * Update the current location, reevaluating the distances between each 
   * friend and your current location.
   * 
   * @param location Your current physical Location
   */
	private void updateWithNewLocation(Location location) {
	  // Update your current location
	  currentLocation = location;

	  // Refresh the ArrayList of contacts
    if (location != null)
      refreshListView();

	  // Geocode your current location to find an address.
	  String latLongString = "";
	  String addressString = "No address found";
	  
    if (location != null) {
      double lat = location.getLatitude();
      double lng = location.getLongitude();
      latLongString = "Lat:" + lat + "\nLong:" + lng;

      Geocoder gc = new Geocoder(this, Locale.getDefault());
      try {
        List<Address> addresses = gc.getFromLocation(lat, lng, 1);
        StringBuilder sb = new StringBuilder();
        if (addresses.size() > 0) {
          Address address = addresses.get(0);
  
          sb.append(address.getLocality()).append("\n");
          sb.append(address.getCountryName());
        }
        addressString = sb.toString();
      } catch (IOException e) {}
    } else {
      latLongString = "No location found";
    }

    // Update the TextView to show your current address.
    TextView myLocationText = (TextView)findViewById(R.id.myLocationText);
    myLocationText.setText("Your Current Position is:\n" + latLongString + "\n" + addressString);
	}

	/**
	 * Update the ArrayList that's bound to the ListView
	 * to show the current distance of each contact to
	 * your current physical location.
	 */
  private void refreshListView() {
    friendDistanceList.clear();
    
    if (friendLocations.size() > 0) {
      Iterator<String> e = friendLocations.keySet().iterator();
      do {
        // Get the name and location
        String name = e.next();          
        Location location = friendLocations.get(name);

        // Find their distance from you
        int distance = (int)currentLocation.distanceTo(location);

        String str = name + " (" + String.valueOf(distance) + "m)";

        // Update the ArrayList
        friendDistanceList.add(str);
      } while (e.hasNext());
    }
    aa.notifyDataSetChanged();
  }
  
  @Override
  public void onStart() {
    super.onStart();
    
    // Find a Location Provider to use.
    String provider = locationManager.getBestProvider(criteria, true);

    // Update the GUI with the last known position.
    Location location = locationManager.getLastKnownLocation(provider);
    updateWithNewLocation(location);

    // Register the LocationListener to listen for location changes
    // using the provider found above.
    locationManager.requestLocationUpdates(provider, 
                                           60000, // 1min
                                           1000,  // 1km 
    	                                     locationListener);
  }
  
  @Override 
  public void onStop() {
    // Unregister the LocationListener to stop updating the
    // GUI when the Activity isn't visible.
    locationManager.removeUpdates(locationListener);

    super.onStop();
  }
    
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_ITEM_MAP, Menu.NONE, R.string.menu_item_map);
    menu.add(0, MENU_ITEM_REFRESH, Menu.NONE, R.string.menu_item_refresh);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      // Check for each known menu item
      case (MENU_ITEM_MAP):
        // Start the Map Activity
        startActivity(new Intent(this, WhereAreMyFriendsMap.class));
        return true;
      case (MENU_ITEM_REFRESH) :
        // Refresh the Friend Location hash
        refreshFriendLocations();
        refreshListView();
        return true;
    }
    
    // Return false if you have not handled the menu item.
    return false;
  }
}