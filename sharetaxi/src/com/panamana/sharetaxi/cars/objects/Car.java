package com.panamana.sharetaxi.cars.objects;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.panamana.sharetaxi.cars.CarsWorker;
import com.panamana.sharetaxi.cars.locations.parser.LocationsJSONParser.LocationsJsonTags;
import com.panamana.sharetaxi.maps.MapManager;
import com.panamana.sharetaxi.utils.DirectionalVector;
import com.panamana.sharetaxi.utils.Position;

/**
 * this is the Car object that gets the JSON data from the locations server
 * response example:
 * "androidID": "1234", "date": 1399279938.0, "lineNum": "5", "longitude": "34.7833047", "latitude": "32.0987302"
 * @author naama
 */
public class Car {

	private static final String TAG = Car.class.getSimpleName();
	// Fields:
	private String mID;
	private String mTime;
	private String mDirection;
	private LatLng mLatLng;
	private String mLineName;
	private Marker mMarker;
	private int mIRouteLocation;
	private float mDistanceFromI;
	
	// Constructor:
	public Car (String ID, String time, String line, LatLng latlng ) {
		this.mID = ID;
		this.mTime = time;
		this.mLineName = line;
		this.mLatLng = latlng;
		this.mMarker = null;
		this.mIRouteLocation = 10000;
		this.mDistanceFromI = 10000;
		this.mDirection = "";
	}
	public String getDirection() {
		return mDirection;
	}
	public void setDirection(String mDirection) {
		this.mDirection = mDirection;
	}
	public int getIRouteLocation() {
		return mIRouteLocation;
	}
	public void setIRootLocation(int mIRootLocation) {
		this.mIRouteLocation = mIRootLocation;
	}
	public float getDistanceFromI() {
		return mDistanceFromI;
	}
	public void setDistanceFromI(float mDistanceFromI) {
		this.mDistanceFromI = mDistanceFromI;
	}
	public Car (JSONObject jo) throws JSONException {
		this(
			jo.getString(LocationsJsonTags.ANDROIDID),           
			jo.getString(LocationsJsonTags.DATE),               
			jo.getString(LocationsJsonTags.LINENUM),          
			new LatLng( jo.getDouble(LocationsJsonTags.LATITUDE), 
	                  	jo.getDouble(LocationsJsonTags.LONGITUDE))
			);
	}

	// Methods:
	
	public Marker getMarker() {
		return mMarker;
	}
	
	public void setMarker(Marker marker) {
		this.mMarker = marker;
	}
	/**
	 * prefix + "5" OR "4a" OR "4"
	 * @return
	 */
	public String getLineName() {
		return mLineName;
	}

	public void setLineName(String mLineName) {
		this.mLineName = mLineName;
	}

	/**
	 * i.e. 1234
	 * @return
	 */
	public String getID() {
		return mID;
	}

	/**
	 * i.e 1399279938.0
	 * @return
	 */
	public String getTime() {
		return mTime;
	}


	public LatLng getLatLng() {
		return mLatLng;
	}

	@Override
	public String toString() {
		return "Car [mID=" + mID + ", mTime=" + mTime + ", mDirection="
				+ mDirection + ", mLatLng=" + mLatLng + ", mLine=" + mLineName + ", mMarker=" + mMarker 
				+ "]";
	}
	
	/**
	 * updates the location of the car on the lines' route and the distance from the last polyline vertex
	 */
	public void calcIRouteLocationAndDistance() {
		PolylineOptions linePolylineOptions = MapManager.polylineOptionsMap.get("line"+mLineName+"North");
		
		if (linePolylineOptions != null) {
			// valid poly line
			int iTHLocation = 0;
			Position carXYZPoint = LatLng2XYZ(mLatLng);
			List<LatLng> linePoints = linePolylineOptions.getPoints(); 
			float distanceFromLine = 10000;
			for (int i = 0; i<linePoints.size()-1; i++) {
				float distanceFromThisLine = 
						Position.distancePfromVectorAB(
								carXYZPoint,
								LatLng2XYZ(linePoints.get(i)),
								LatLng2XYZ(linePoints.get(i+1)));
				if (distanceFromLine < distanceFromLine) {
					// car is closer to this line than to the last one
					distanceFromLine = distanceFromThisLine;
					iTHLocation =i;
				}
				// if the car was closer to the last line than to this one, we can stop iterate over the lines points
				if (distanceFromThisLine > distanceFromLine) {
					break;
				}
			}
			Log.i(TAG,"11routeLocation"+Integer.toString(this.getIRouteLocation()));
			mIRouteLocation=iTHLocation;
			mDistanceFromI=DirectionalVector.calcDirection(
					LatLng2XYZ(linePoints.get(iTHLocation)),
					carXYZPoint).getVectorSize();
			Log.i(TAG,"12routeLocation"+Integer.toString(this.getIRouteLocation()));
		}
	}
	
	
	public void updateCarDirection() {
		// the prev object of the same car in CarsWorker.cars
		Car carByID = CarsWorker.cars.get(mID);
		if (carByID == null) {
			carByID = this;
		}
		// if car was just initialized 
		// I root location - the I'th segment of the route
		if (carByID.getIRouteLocation() == 10000) {
			Log.i(TAG,"no prev location");
			this.calcIRouteLocationAndDistance();
		} else {
			Log.i(TAG,"save prev location");
			int prevIRouteLocation = carByID.getIRouteLocation();
			float prevDistanceFromI = carByID.getDistanceFromI();
			this.calcIRouteLocationAndDistance();
			// if car is still on the same I-th polyline of the root
			if (prevIRouteLocation == this.getIRouteLocation()) {
				if (prevDistanceFromI < this.getDistanceFromI()) {
					mDirection = "North";
				} else {
					mDirection = "South";
				}
			} else {
				if (prevIRouteLocation < this.getIRouteLocation()) {
					mDirection = "North";
					;
				} else {
					mDirection = "South";
				}
			}
		}
		CarsWorker.cars.put(mID, carByID);
		Log.i(TAG,carByID.getDirection());

	}		

	
	private Position LatLng2XYZ(LatLng latlng) {
		// TODO Auto-generated method stub
		float xPos = (float) 6371000 * (float)Math.cos(latlng.latitude) * (float)Math.cos(latlng.longitude);
		float yPos = (float) 6371000 * (float)Math.cos(latlng.latitude) * (float)Math.sin(latlng.longitude);
		float zPos = (float) 6371000 * (float)Math.sin(latlng.latitude);
		Position position = new Position(xPos, yPos, zPos);
		return position;
	}		
	
}