package org.ohdsi.webapi.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Value {

    @JsonProperty("value_id")
    public Integer value_id;

    @JsonProperty("value_ordinal")
    public Integer value_ordinal;

    @JsonProperty("metadata_id")
    public Integer metadata_id;

    @JsonProperty("annotation_id")
    public Integer annotation_id;

    @JsonProperty("value_concept_id")
    public Integer value_concept_id;

    @JsonProperty("value_type_concept_id")
    public Integer value_type_concept_id;

    @JsonProperty("value_as_string")
    public String value_as_string;

    @JsonProperty("value_as_number")
    public float value_as_number;
}
