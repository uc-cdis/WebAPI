package org.ohdsi.webapi.metadata;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Annotation {

    @JsonProperty("annotation_id")
    public Integer annotation_id;

    @JsonProperty("metadata_id")
    public Integer metadata_id;

    @JsonProperty("annotation_concept_id")
    public Integer annotation_concept_id;

    @JsonProperty("annotation_type_concept_id")
    public Integer annotation_type_concept_id;

    @JsonProperty("security_concept_id")
    public Integer security_concept_id;
}
