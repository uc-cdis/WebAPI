package org.ohdsi.webapi.metadata;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Metadata {

    @JsonProperty("metadata_id")
    public Integer metadata_id;

    @JsonProperty("metadata_concept_id")
    public Integer metadata_concept_id;

    @JsonProperty("metadata_type_concept_id")
    public Integer metadata_type_concept_id;

    @JsonProperty("metadata_as_string")
    public String metadata_as_string;

    @JsonProperty("target_concept_id")
    public Integer target_concept_id;

    @JsonProperty("target_as_string")
    public String target_as_string;

    @JsonProperty("target_identifier")
    public Integer target_identifier;

    @JsonProperty("security_concept_id")
    public Integer security_concept_id;

}


