package org.ohdsi.webapi.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Reference {

    @JsonProperty("reference_id")
    public Integer reference_id;

    @JsonProperty("reference_concept_id")
    public Integer reference_concept_id;

    @JsonProperty("reference_type_concept_id")
    public Integer reference_type_concept_id;

    @JsonProperty("reference_as_string")
    public String reference_as_string;

    @JsonProperty("threshold_concept_id")
    public Integer threshold_concept_id;

    @JsonProperty("threshold_as_string")
    public String threshold_as_string;

    @JsonProperty("threshold_minimum_value")
    public float threshold_minimum_value;

    @JsonProperty("threshold_maximum_value")
    public float threshold_maximum_value;
}
