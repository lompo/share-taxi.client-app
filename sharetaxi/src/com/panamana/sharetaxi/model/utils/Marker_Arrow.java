package com.panamana.sharetaxi.model.utils;

import com.google.android.gms.maps.model.Marker;

public class Marker_Arrow {

	public Marker marker;
	public Marker arrow;
	
	public Marker_Arrow(Marker marker, Marker arrow) {
		this.marker = marker;
		this.arrow = arrow;
	}

	public void remove() {
		marker.remove();
		arrow.remove();
	}
	

}