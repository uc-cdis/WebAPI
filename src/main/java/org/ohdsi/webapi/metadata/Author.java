package org.ohdsi.webapi.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Ajit Londhe <alondhe2@its.jnj.com>
 */

public class Author {

    @JsonProperty("author_id")
    public String author_id;

    @JsonProperty("author_concept_id")
    public Integer author_concept_id;

    @JsonProperty("author_human_first_name")
    public String author_human_first_name;

    @JsonProperty("author_human_last_name")
    public String author_human_last_name;

    @JsonProperty("author_human_suffix")
    public String author_human_suffix;

    @JsonProperty("author_description")
    public String author_description;

    @JsonProperty("author_algorithm_name")
    public String author_algorithm_name;

    @JsonProperty("author_algorithm_version")
    public String author_algorithm_version;
}
