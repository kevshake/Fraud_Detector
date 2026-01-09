# Geographic Risk Heatmap Map Implementation

## Overview

Frontend map visualization for the geographic risk heatmap has been successfully implemented using Leaflet.js, an open-source JavaScript library for interactive maps.

---

## Implementation Details

### 1. Map Library

**Leaflet.js** - Lightweight, open-source mapping library
- Version: 1.9.4
- CDN: `https://unpkg.com/leaflet@1.9.4/dist/leaflet.js`
- CSS: `https://unpkg.com/leaflet@1.9.4/dist/leaflet.css`

### 2. Features Implemented

✅ **Interactive World Map**
- OpenStreetMap tile layer
- Zoom controls (2-6 zoom levels)
- Pan and drag functionality
- Responsive design

✅ **Risk Visualization**
- Color-coded markers based on risk score:
  - **Dark Red (#c0392b)**: Critical risk (75%+)
  - **Red (#e74c3c)**: High risk (50-75%)
  - **Orange (#f39c12)**: Medium risk (25-50%)
  - **Green (#27ae60)**: Low risk (0-25%)

✅ **Marker Sizing**
- Circle markers sized by case count
- Larger circles = more cases
- Minimum radius: 5px, Maximum radius: 30px

✅ **Interactive Popups**
- Click on any country marker to see:
  - Country name
  - Country code
  - Risk score (percentage)
  - Case count
  - Average risk score
  - Risk level (CRITICAL/HIGH/MEDIUM/LOW)

✅ **Time Period Filter**
- Dropdown selector for time periods:
  - Last 30 Days
  - Last 90 Days (default)
  - Last 180 Days
  - Last Year

✅ **Legend**
- Visual legend showing risk level colors
- Positioned at bottom-right of map
- Responsive positioning

---

## Files Modified

### 1. `src/main/resources/static/index.html`

**Added:**
- Leaflet.js CSS and JavaScript libraries
- Map container: `<div id="geographicRiskMap">`
- Period selector dropdown
- Legend container: `<div id="geographicRiskLegend">`

**Location:** Added after Alert Disposition Chart in charts grid

### 2. `src/main/resources/static/js/dashboard.js`

**Added Functions:**
- `initGeographicRiskMap()` - Initialize Leaflet map
- `loadGeographicRiskData()` - Fetch data from API
- `renderGeographicRiskMap(data)` - Render markers on map
- `getRiskColor(riskScore)` - Get color based on risk score
- `getRiskLevel(riskScore)` - Get risk level text
- `updateGeographicRiskLegend(maxRiskScore)` - Update legend
- `getCountryCoordinates()` - Country code to coordinates mapping
- `getCountryName(countryCode)` - Country code to name mapping

**Variables:**
- `geographicRiskMap` - Leaflet map instance
- `geographicRiskMarkers` - Array of marker objects

### 3. `src/main/resources/static/css/dashboard.css`

**Added Styles:**
- `.map-legend` - Legend container styling
- `.legend-title` - Legend title styling
- `.legend-item` - Legend item styling
- `.legend-color` - Color indicator styling
- `.legend-note` - Legend note styling
- `.leaflet-popup-content-wrapper` - Popup customization
- `.leaflet-container` - Map container styling
- Responsive styles for mobile devices

---

## API Integration

**Endpoint:** `GET /analytics/risk/heatmap/geographic`

**Query Parameters:**
- `startDate` - ISO 8601 datetime string
- `endDate` - ISO 8601 datetime string

**Response Format:**
```json
{
  "USA": {
    "id": "USA",
    "type": "GEOGRAPHY",
    "caseCount": 15,
    "averageRiskScore": 0.65
  },
  "GBR": {
    "id": "GBR",
    "type": "GEOGRAPHY",
    "caseCount": 8,
    "averageRiskScore": 0.45
  }
}
```

---

## Country Coverage

**Supported Countries:** 50+ countries with coordinates

Includes major countries:
- North America: USA, Canada, Mexico
- Europe: UK, Germany, France, Italy, Spain, etc.
- Asia: China, Japan, India, South Korea, Singapore, etc.
- Middle East: UAE, Saudi Arabia, Israel, Turkey, etc.
- Africa: South Africa, Egypt, Nigeria, Kenya
- South America: Brazil, Argentina, Chile, Colombia
- Oceania: Australia, New Zealand

**Note:** Countries not in the coordinate mapping will be logged as warnings but won't break the map.

---

## Usage

### Automatic Initialization

The map automatically initializes when:
1. Dashboard loads (`DOMContentLoaded` event)
2. Charts are initialized (`initCharts()` function)
3. User switches to dashboard view

### Manual Refresh

Users can refresh data by:
1. Changing the time period dropdown
2. The map automatically reloads data

### Error Handling

- API errors are handled gracefully
- Failed requests show error notifications
- Map remains functional even if data fetch fails

---

## Customization

### Adding More Countries

To add more countries, update `getCountryCoordinates()` function:

```javascript
function getCountryCoordinates() {
    return {
        // ... existing countries
        'NEW_CODE': [latitude, longitude]
    };
}
```

### Changing Colors

Modify `getRiskColor()` function:

```javascript
function getRiskColor(riskScore) {
    if (riskScore >= 0.75) return '#your-color';
    // ... other thresholds
}
```

### Adjusting Marker Sizes

Modify marker radius calculation in `renderGeographicRiskMap()`:

```javascript
radius: Math.max(5, Math.min(30, (data.caseCount / maxCaseCount) * 30))
```

---

## Performance Considerations

1. **Lazy Loading:** Map only initializes when dashboard view is active
2. **Marker Cleanup:** Old markers are removed before adding new ones
3. **Efficient Rendering:** Uses Leaflet's optimized rendering
4. **Responsive Design:** Adapts to different screen sizes

---

## Browser Compatibility

- ✅ Chrome/Edge (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

---

## Future Enhancements

Potential improvements:
1. **Country Boundaries:** Add country boundary polygons for better visualization
2. **Heatmap Layer:** Use Leaflet heatmap plugin for gradient visualization
3. **Clustering:** Cluster markers when zoomed out
4. **Animation:** Animate marker appearance
5. **Export:** Add export functionality (PNG, PDF)
6. **Drill-down:** Click to see detailed country statistics
7. **Historical View:** Show risk trends over time

---

## Troubleshooting

### Map Not Showing

1. Check browser console for errors
2. Verify Leaflet.js is loaded: `typeof L !== 'undefined'`
3. Check map container exists: `document.getElementById('geographicRiskMap')`
4. Verify API endpoint is accessible

### Markers Not Appearing

1. Check API response format matches expected structure
2. Verify country codes match coordinate mapping
3. Check browser console for coordinate warnings
4. Verify data contains valid risk scores

### Performance Issues

1. Reduce number of countries displayed
2. Increase marker size threshold
3. Use marker clustering for many countries
4. Optimize country coordinate lookup

---

## Screenshots

The map displays:
- World map with country markers
- Color-coded risk levels
- Size-coded case counts
- Interactive popups with details
- Legend showing risk levels
- Time period selector

---

**Status:** ✅ **FULLY IMPLEMENTED AND FUNCTIONAL**

**Last Updated:** January 6, 2026

