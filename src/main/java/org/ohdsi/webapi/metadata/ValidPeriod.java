package org.ohdsi.webapi.metadata;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.sql.Time;
import java.sql.Timestamp;

public class ValidPeriod {

    @JsonProperty("valid_period_id")
    public Integer valid_period_id;

    @JsonProperty("metadata_id")
    public Integer metadata_id;

    @JsonProperty("annotation_id")
    public Integer annotation_id;

    @JsonProperty("reference_id")
    public Integer reference_id;

    @JsonProperty("valid_period_start_datetime")
    public Timestamp valid_period_start_datetime;

    @JsonProperty("valid_period_end_datetime")
    public Timestamp valid_period_end_datetime;
}
