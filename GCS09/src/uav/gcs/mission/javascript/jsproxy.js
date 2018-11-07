var jsproxy = {
    log: function (level, methodName, message) {
        jsproxy.java.javascriptLog(level, methodName, message);
    },

    setMapZoom: function (zoom) {
        try {
            map.googlemap.setZoom(zoom);
        } catch (err) {
            jsproxy.log("ERROR", "jsproxy.setMapZoom()", err);
        }
    },

    setZoomSliderValue: function (zoom) {
        jsproxy.java.setZoomSliderValue(zoom);
    },

    setHomePosition(lat, lng) {
        try {
            map.uav.setHomePosition(lat, lng);
        } catch (err) {
            jsproxy.log("ERROR", "jsproxy.setHomePosition()", err);
        }
    },

    setCurrLocation: function (lat, lng, heading) {
        try {
            if (map.uav == null) {
                map.uav = new Uav();
                map.googlemap.setCenter({lat: lat, lng: lng});
                map.googlemap.setZoom(18);
                jsproxy.java.setZoomSliderValue(18);
            }
            map.uav.currLocation = {lat: lat, lng: lng};
            map.uav.heading = heading;
        } catch (err) {
            jsproxy.log("ERROR", "jsproxy.setCurrLocation()", err);
        }
    },
    manualMake: function (manualAlt) {
        map.manualMake = true;
        map.missionMake = false;
        map.fenceMake = false;
        map.uav.manualAlt = manualAlt;
    },

    manualMove: function (manualLat, manualLng, manualAlt) {
        jsproxy.java.manualMove(manualLat, manualLng, manualAlt);
    },

    setMode: function (mode) {
        if (mode == 4) {
            //guidedMode
            map.uav.setMode(true, false, false, false);
        } else if (mode == 3) {
            //autoMode
            map.uav.setMode(false, true, false, false);
        } else if (mode == 6) {
            //rtlMode
            map.uav.setMode(false, false, true, false);
        } else if (mode == 9) {
            //LandMode
            map.uav.setMode(false, false, false, true);
        }
    },

    missionMake: function () {
        //autoMode
        map.manualMake = false
        map.missionMake = true;
        map.fenceMake = false;
    }
};