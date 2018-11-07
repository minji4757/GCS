var map = {
    //Google Map 객체 저장 필드
    googlemap: null,

    //Uav 객체 저장 필드
    uav: null,

    //지도상에서 클릭하는 경우의 종류
    manualMake: false,
    missionMake: false,
    fenceMake: false,

    //지도 다운로드, 지도 이벤트 처리, Uav 드로잉 시작
    init: function () {
        //지도 생성
        map.googlemap = new google.maps.Map(
            document.getElementById('map'),
            {
                center: {lat: 35.788870, lng: -117.266334},
                zoom: 3,
                mapTypeControl: false,
                mapTypeId: "satellite",
                streetViewControl: false,
                zoomControl: false,
                rotateControl: false,
                fullscreenControl: false
            }
        );

        //마우스 휠로 확대 레벨을 변경했을 경우
        document.getElementById("map").addEventListener("wheel", function () {
            var zoom = map.googlemap.getZoom();
            if (zoom < 3) {
                zoom = 3;
                map.googlemap.setZoom(zoom);
            }
            jsproxy.setZoomSliderValue(zoom);
        });

        //지도가 모두 다운로드되었을 때 매 1초 단위로 드론을 드로잉
        google.maps.event.addListenerOnce(map.googlemap, "idle", function () {
            map.uavDraw.start();
        });

        //지도에서 마우스로 클릭할 경우
        /* map.googlemap.addListener('click', function (ev) { //event
            이렇게 써도됑
         })*/
        google.maps.event.addListener(map.googlemap, "click", function (ev) {
            try {
                if (map.manualMake == true) {
                    map.uav.manualMove(ev.latLng.lat(), ev.latLng.lng())
                } else if (map.missionMake == true) {
                    var missionItem = {
                        seq: map.uav.missionMarkers.length,
                        command: "WAYPOINT",
                        param1: 0,
                        param2: 0,
                        param3: 0,
                        param4: 0,
                        x: ev.latLng.lat(),
                        y: ev.latLng.lng(),
                        z: 0
                    };
                    map.uav.missionMarkerMake(missionItem);
                } else if (map.fenceMake == true) {

                }
            } catch (err) {
                jsproxy.log("ERROR", err);
            }
        });
    },

    uavDraw: {
        count: 1,
        start: function () {
            setInterval(
                function () {
                    if (map.uav != null) {
                        map.uav.drawUav();
                        if (map.uavDraw.count == 3) {
                            //map.googlemap.setCenter(map.uav.currLocation);
                            map.googlemap.panTo(map.uav.currLocation);
                            map.uavDraw.count = 1;
                        } else {
                            ++map.uavDraw.count;
                        }
                    }
                },
                1000
            );
        }
    }
};