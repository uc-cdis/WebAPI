package org.ohdsi.webapi.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Activity {

    @JsonProperty("activity_id")
    public Integer activity_id;

    @JsonProperty("author_id")
    public String author_id;

    @JsonProperty("metadata_id")
    public Integer metadata_id;

    @JsonProperty("annotation_id")
    public Integer annotation_id;

    @JsonProperty("reference_id")
    public Integer reference_id;

    @JsonProperty("annotation_datetime")
    public Timestamp activity_datetime;

    @JsonProperty("activity_concept_id")
    public Integer activity_concept_id;
}
