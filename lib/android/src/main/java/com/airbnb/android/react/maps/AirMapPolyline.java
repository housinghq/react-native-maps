package com.airbnb.android.react.maps;

import android.content.Context;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class AirMapPolyline extends AirMapFeature {

  private PolylineOptions polylineOptions;
  private Polyline polyline;

  private List<LatLng> coordinates;
  private int color;
  private float width;
  private boolean tappable;
  private boolean showBezierCurve;
  private boolean geodesic;
  private float zIndex;
  private Cap lineCap = new RoundCap();
  private ReadableArray patternValues;
  private List<PatternItem> pattern;

  public AirMapPolyline(Context context) {
    super(context);
  }

  public void setCoordinates(ReadableArray coordinates) {
    this.coordinates = new ArrayList<>(coordinates.size());
    for (int i = 0; i < coordinates.size(); i++) {
      ReadableMap coordinate = coordinates.getMap(i);
      this.coordinates.add(i,
          new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude")));
    }
    if (polyline != null) {
      polyline.setPoints(this.coordinates);
    }
  }

  public void setColor(int color) {
    this.color = color;
    if (polyline != null) {
      polyline.setColor(color);
    }
  }

  public void setWidth(float width) {
    this.width = width;
    if (polyline != null) {
      polyline.setWidth(width);
    }
  }

  public void setZIndex(float zIndex) {
    this.zIndex = zIndex;
    if (polyline != null) {
      polyline.setZIndex(zIndex);
    }
  }

  public void setTappable(boolean tapabble) {
    this.tappable = tapabble;
    if (polyline != null) {
      polyline.setClickable(tappable);
    }
  }
  public void setShowBezierCurve(boolean showBezierCurve) {
    this.showBezierCurve = showBezierCurve;
  }

  public void setGeodesic(boolean geodesic) {
    this.geodesic = geodesic;
    if (polyline != null) {
      polyline.setGeodesic(geodesic);
    }
  }

  public void setLineCap(Cap cap) {
    this.lineCap = cap;
    if (polyline != null) {
      polyline.setStartCap(cap);
      polyline.setEndCap(cap);
    }
    this.applyPattern();
  }

  public void setLineDashPattern(ReadableArray patternValues) {
    this.patternValues = patternValues;
    this.applyPattern();
  }

  private void applyPattern() {
    if(patternValues == null) {
      return;
    }
    this.pattern = new ArrayList<>(patternValues.size());
    for (int i = 0; i < patternValues.size(); i++) {
      float patternValue = (float) patternValues.getDouble(i);
      boolean isGap = i % 2 != 0;
      if(isGap) {
        this.pattern.add(new Gap(patternValue));
      }else {
        PatternItem patternItem = null;
        boolean isLineCapRound = this.lineCap instanceof RoundCap;
        if(isLineCapRound) {
          patternItem = new Dot();
        }else {
          patternItem = new Dash(patternValue);
        }
        this.pattern.add(patternItem);
      }
    }
    if(polyline != null) {
      polyline.setPattern(this.pattern);
    }
  }

  public PolylineOptions getPolylineOptions() {
    if (polylineOptions == null) {
      polylineOptions = createPolylineOptions();
    }
    return polylineOptions;
  }

  private PolylineOptions createPolylineOptions() {
    PolylineOptions options = new PolylineOptions();

    options.color(color);
    options.width(width);
    options.geodesic(geodesic);
    options.zIndex(zIndex);
    options.startCap(lineCap);
    options.endCap(lineCap);
    options.pattern(this.pattern);

    if(this.showBezierCurve){
      LatLng init = this.coordinates.get(0);
      LatLng end = this.coordinates.get(this.coordinates.size()-1);

      double distanceBetween = SphericalUtil.computeDistanceBetween(init, end);
      double lineHeadingInit = SphericalUtil.computeHeading(init, end);

      double lineHeading1, lineHeading2;
      if (lineHeadingInit < 0) {
        lineHeading1 = lineHeadingInit + 45;
        lineHeading2 = lineHeadingInit + 135;
      } else {
        lineHeading1 = lineHeadingInit + -45;
        lineHeading2 = lineHeadingInit + -135;
      }

      LatLng pA = SphericalUtil.computeOffset(init, distanceBetween / 2.5, lineHeading1);
      LatLng pB = SphericalUtil.computeOffset(end, distanceBetween / 2.5, lineHeading2);


      LatLng curveLatLng = null;
      for (double t = 0.0; t < 1.01; t += 0.01) {
        // P = (1−t)3P1 + 3(1−t)2tP2 +3(1−t)t2P3 + t3P4; for 4 points
        double arcX = (1 - t) * (1 - t) * (1 - t) * init.latitude
                + 3 * (1 - t) * (1 - t) * t * pA.latitude
                + 3 * (1 - t) * t * t * pB.latitude
                + t * t * t * end.latitude;
        double arcY = (1 - t) * (1 - t) * (1 - t) * init.longitude
                + 3 * (1 - t) * (1 - t) * t * pA.longitude
                + 3 * (1 - t) * t * t * pB.longitude
                + t * t * t * end.longitude;

        curveLatLng = new LatLng(arcX, arcY);
        options.add(curveLatLng);
      }

    }else{
      options.addAll(coordinates);
    }


    return options;
  }

  @Override
  public Object getFeature() {
    return polyline;
  }

  @Override
  public void addToMap(GoogleMap map) {
    polyline = map.addPolyline(getPolylineOptions());
    polyline.setClickable(this.tappable);
  }

  @Override
  public void removeFromMap(GoogleMap map) {
    polyline.remove();
  }
}
