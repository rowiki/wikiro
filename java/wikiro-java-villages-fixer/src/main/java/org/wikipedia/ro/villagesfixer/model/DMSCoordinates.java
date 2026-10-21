package org.wikipedia.ro.villagesfixer.model;

import lombok.Data;

@Data
public class DMSCoordinates {
    private DMSValue longitude;
    private DMSValue latitude;
    public DMSCoordinates(DMSValue latitude, DMSValue longitude) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
