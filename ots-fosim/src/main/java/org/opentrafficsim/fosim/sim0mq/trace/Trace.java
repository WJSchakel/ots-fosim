package org.opentrafficsim.fosim.sim0mq.trace;

/**
 * Specification of trace files.
 * @author wjschakel
 */
public enum Trace
{

    /** Acceleration changes and lane changes. */
    ACCELERATION_CHANGE(new Info(Info.ACCELERATION_CHANGE_ID, "Acceleratie veranderingen uitvoer", "Acceleration Changes",
            new String[] {"t (s)", "fromln", "tolane", "from a", "to a", "pos (m)", "v (m/s)", "type", "id"})),

    /** Detector passages. */
    DETECTION(new Info(Info.DETECTION_ID, "Micro detector uitvoer", "Detector Passages",
            new String[] {"pos (m)", "lane", "t (s)", "v (m/s)", "type", "id", "dest"})),

    /** Lane changes. */
    LANE_CHANGE(new Info(Info.LANE_CHANGE_ID, "Strookwisselingen uitvoer", "Lane Changes",
            new String[] {"t(s)", "fromln", "tolane", "pos (m)", "type", "id"})),

    /** Travel time between origin and destination. */
    OD_TRAVEL_TIME(new Info(Info.OD_TRAVEL_TIME_ID, "Trajecttijden tussen herkomst en bestemming uitvoer",
            "Origin To Destination Travel Times", new String[] {"t (s)", "origin", "dest", "tt (s)", "v (m/s)", "type", "id"})),

    /** Travel time between detectors. */
    TRAVEL_TIME(new Info(Info.TRAVEL_TIME_ID, "Trajecttijden tussen detectoren uitvoer", "Detector To Detector Travel Times",
            new String[] {"pos (m)", "lane", "t (s)", "dt (s)", "v (m/s)", "type", "id", "dest"})),

    /** Vehicle samples. */
    VEHICLES(new Info(Info.VEHICLES_ID, "Voertuig monsters", "Vehicle Samples",
            new String[] {"t (s)", "id", "type", "origin", "dest", "lane", "pos (m)", "v (m/s)"}));

    /** Data in trace file specification. */
    final Info data;

    /**
     * Returns the trace file type with given id.
     * @param id id
     * @return trace file type with given id
     */
    public static Trace byId(final String id)
    {
        for (Trace trace : values())
        {
            if (trace.getInfo().id().equals(id))
            {
                return trace;
            }
        }
        throw new IllegalArgumentException("Id " + id + " is not a valid trace id.");
    }

    /**
     * Constructor.
     * @param data data in trace file specification.
     */
    Trace(final Info data)
    {
        this.data = data;
    }

    /**
     * Returns the trace meta data.
     * @return trace meta data.
     */
    public Info getInfo()
    {
        return this.data;
    }

    /**
     * Data of trace specification.
     * @param id id
     * @param dutchName Dutch menu name
     * @param englishName English menu name
     * @param header header fields in trace file
     */
    public record Info(String id, String dutchName, String englishName, String[] header)
    {

        /** Acceleration change id. */
        public static final String ACCELERATION_CHANGE_ID = "AccelerationChange";

        /** Acceleration change id. */
        public static final String DETECTION_ID = "Detection";

        /** Acceleration change id. */
        public static final String LANE_CHANGE_ID = "LaneChange";

        /** Acceleration change id. */
        public static final String OD_TRAVEL_TIME_ID = "ODTravelTime";

        /** Acceleration change id. */
        public static final String TRAVEL_TIME_ID = "TravelTime";

        /** Acceleration change id. */
        public static final String VEHICLES_ID = "Vehicles";

    }

}
