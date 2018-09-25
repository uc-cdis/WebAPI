/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ohdsi.webapi.service;

import static java.lang.Character.isJavaLetterOrDigit;
import static java.lang.Character.toLowerCase;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import org.ohdsi.circe.helper.ResourceHelper;

import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.metadata.*;
import org.ohdsi.webapi.rsb.Result;
import org.ohdsi.webapi.source.Source;
import org.ohdsi.webapi.source.SourceDaimon;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Transactional;

/**
 * REST Web Service
 *
 * @author alondhe
 */
@Path("/metadata/")
@Component
public class MetadataService extends AbstractDaoService
{

    private static final String BASE_SQL_PATH = "/resources/metadata/sql";

    private static String getNullableString(Object value)
    {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private final RowMapper<Metadata> metadataRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Metadata metadata = new Metadata();

        metadata.metadata_id = resultSet.getInt("metadata_id");
        metadata.metadata_concept_id = resultSet.getInt("metadata_concept_id");
        metadata.metadata_type_concept_id = resultSet.getInt("metadata_type_concept_id");
        metadata.metadata_as_string = getNullableString(resultSet.getString("metadata_as_string"));
        metadata.target_concept_id = resultSet.getInt("target_concept_id");
        metadata.target_as_string = getNullableString(resultSet.getString("target_as_string"));
        metadata.target_identifier = resultSet.getInt("target_identifier");
        metadata.security_concept_id = resultSet.getInt("security_concept_id");

        return metadata;
    };

    private final RowMapper<Author> authorRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Author author = new Author();

        author.author_id = getNullableString(resultSet.getString("author_id"));
        author.author_concept_id = resultSet.getInt("author_concept_id");
        author.author_algorithm_name = getNullableString(resultSet.getString("author_algorithm_name"));
        author.author_algorithm_version = getNullableString(resultSet.getString("author_algorithm_version"));
        author.author_description = getNullableString(resultSet.getString("author_description"));
        author.author_human_first_name = getNullableString(resultSet.getString("author_human_first_name"));
        author.author_human_last_name = getNullableString(resultSet.getString("author_human_last_name"));
        author.author_human_suffix = getNullableString(resultSet.getString("author_human_suffix"));

        return author;
    };


    private final RowMapper<Activity> activityRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Activity activity = new Activity();

        activity.activity_id = resultSet.getInt("activity_id");
        activity.activity_concept_id = resultSet.getInt("activity_concept_id");
        activity.activity_datetime = resultSet.getTimestamp("activity_datetime");
        activity.annotation_id = resultSet.getInt("annotation_id");
        activity.reference_id = resultSet.getInt("reference_id");
        activity.author_id = getNullableString(resultSet.getString("author_id"));
        activity.metadata_id = resultSet.getInt("metadata_id");

        return activity;
    };


    private final RowMapper<Annotation> annotationRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Annotation annotation = new Annotation();

        annotation.annotation_id = resultSet.getInt("annotation_id");
        annotation.annotation_concept_id = resultSet.getInt("annotation_concept_id");
        annotation.annotation_type_concept_id = resultSet.getInt("annotation_type_concept_id");
        annotation.metadata_id = resultSet.getInt("metadata_id");
        annotation.security_concept_id = resultSet.getInt("security_concept_id");

        return annotation;
    };


    private final RowMapper<ValidPeriod> validPeriodRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final ValidPeriod validPeriod = new ValidPeriod();

        validPeriod.valid_period_id = resultSet.getInt("valid_period_id");
        validPeriod.annotation_id = resultSet.getInt("annotation_id");
        validPeriod.metadata_id = resultSet.getInt("metadata_id");
        validPeriod.reference_id = resultSet.getInt("reference_id");
        validPeriod.valid_period_start_datetime = resultSet.getTimestamp("valid_period_start_datetime");
        validPeriod.valid_period_end_datetime = resultSet.getTimestamp("valid_period_end_datetime");

        return validPeriod;
    };


    private final RowMapper<Value> valueRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Value value = new Value();

        value.value_id = resultSet.getInt("valid_id");
        value.annotation_id = resultSet.getInt("annotation_id");
        value.metadata_id = resultSet.getInt("metadata_id");
        value.value_as_number = resultSet.getFloat("value_as_number");
        value.value_as_string = getNullableString(resultSet.getString("value_as_string"));
        value.value_concept_id = resultSet.getInt("value_concept_id");
        value.value_ordinal = resultSet.getInt("value_ordinal");
        value.value_type_concept_id = resultSet.getInt("value_type_concept_id");

        return value;
    };

    private final RowMapper<Reference> referenceRowMapper = (final ResultSet resultSet, final int arg1) -> {
        final Reference reference = new Reference();

        reference.reference_id = resultSet.getInt("reference_id");
        reference.reference_concept_id = resultSet.getInt("reference_concept_id");
        reference.reference_type_concept_id = resultSet.getInt("reference_type_concept_id");
        reference.reference_as_string = getNullableString(resultSet.getString("reference_as_string"));
        reference.threshold_concept_id = resultSet.getInt("threshold_concept_id");
        reference.threshold_as_string = getNullableString(resultSet.getString("threshold_as_string"));
        reference.threshold_minimum_value = resultSet.getFloat("threshold_minimum_value");
        reference.threshold_maximum_value = resultSet.getFloat("threshold_maximum_value");

        return reference;
    };

    @GET
    @Path("{sourceKey}/activity/get/id/{activityId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Activity> getActivityById(
            @PathParam("sourceKey") String sourceKey,
            @PathParam("activityId") Integer activityId)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = String.format("select * from @metadataDatabaseSchema.activity where activity_id = %d", activityId);

        sql = SqlRender.renderSql(sql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.activityRowMapper);
    }

    @GET
    @Path("{sourceKey}/activity/get/concept/{conceptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Activity> getActivityByConcept(
            @PathParam("sourceKey") String sourceKey,
            @PathParam("conceptId") Integer conceptId)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = String.format("select * from @metadataDatabaseSchema.activity where activity_concept_id = %d", conceptId);

        sql = SqlRender.renderSql(sql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.activityRowMapper);
    }

    @PUT
    @Path("{sourceKey}/activity/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer addActivity(
            @PathParam("sourceKey") String sourceKey,
            Activity activity)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);

        String maxIdSql = "select coalesce(max(activity_id), 0) + 1 from @metadataDatabaseSchema.activity";
        maxIdSql = SqlRender.renderSql(maxIdSql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        maxIdSql = SqlTranslate.translateSql(maxIdSql, source.getSourceDialect());
        activity.activity_id = getSourceJdbcTemplate(source).queryForObject(maxIdSql, new Object[] { }, Integer.class );

        String sql = ResourceHelper.GetResourceAsString(BASE_SQL_PATH + "/addActivity.sql");

        sql = SqlRender.renderSql(sql, new String[] {
                        "metadataDatabaseSchema",
                        "activityId",
                        "authorId",
                        "metadataId",
                        "annotationId",
                        "referenceId",
                        "activityDatetime",
                        "activityConceptId"
                },
                new String[] {
                        tableQualifier,
                        String.valueOf(activity.activity_id),
                        String.valueOf(activity.author_id),
                        String.valueOf(activity.metadata_id),
                        String.valueOf(activity.annotation_id),
                        String.valueOf(activity.reference_id),
                        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(activity.activity_datetime),
                        String.valueOf(activity.activity_concept_id)
                });
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        try {
            getSourceJdbcTemplate(source).execute(sql);
            return activity.activity_id;
        }
        catch (DataAccessException e) {
            throw e;
        }

    }

    @GET
    @Path("{sourceKey}/author/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Author> getAllAuthors(
            @PathParam("sourceKey") String sourceKey)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = "select * from @metadataDatabaseSchema.author";

        sql = SqlRender.renderSql(sql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.authorRowMapper);
    }


    @GET
    @Path("{sourceKey}/author/get/concept/{conceptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Author> getAuthorsByConcept(
            @PathParam("sourceKey") String sourceKey,
            @PathParam("conceptId") Integer conceptId)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = String.format("select * from @metadataDatabaseSchema.author where author_concept_id = %d", conceptId);

        sql = SqlRender.renderSql(sql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.authorRowMapper);
    }


    @GET
    @Path("{sourceKey}/author/get/id/{authorId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Author> getAuthorById(
            @PathParam("sourceKey") String sourceKey,
            @PathParam("authorId") String authorId)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = String.format("select top 1 * from @metadataDatabaseSchema.author where author_id = '%s';", authorId);

        sql = SqlRender.renderSql(sql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.authorRowMapper);
    }

    @PUT
    @Path("{sourceKey}/author/add")
    @Produces(MediaType.APPLICATION_JSON)
    public String addAuthor(
            @PathParam("sourceKey") String sourceKey,
            Author newAuthor)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);

        String maxIdSql = "select coalesce(max(author_id), 0) + 1 from @metadataDatabaseSchema.author";
        maxIdSql = SqlRender.renderSql(maxIdSql, new String[] {"metadataDatabaseSchema"}, new String[] {tableQualifier});
        maxIdSql = SqlTranslate.translateSql(maxIdSql, source.getSourceDialect());

        String newAuthorId = newAuthor.author_id;

        if (newAuthorId.equals(""))
        {
            newAuthorId = String.valueOf(getSourceJdbcTemplate(source).queryForObject(maxIdSql,
                    new Object[] { }, Integer.class ));
        }

        String sql = ResourceHelper.GetResourceAsString(BASE_SQL_PATH + "/addAuthor.sql");

        sql = SqlRender.renderSql(sql, new String[] {
                "metadataDatabaseSchema",
                "authorId",
                "authorConceptId",
                "authorHumanFirstName",
                "authorHumanLastName",
                "authorHumanSuffix",
                "authorDescription",
                "authorAlgorithmName",
                "authorAlgorithmVersion"
                },
                new String[] {
                        tableQualifier,
                        newAuthorId,
                        String.valueOf(newAuthor.author_concept_id),
                        newAuthor.author_human_first_name,
                        newAuthor.author_human_last_name,
                        newAuthor.author_human_suffix,
                        newAuthor.author_description,
                        newAuthor.author_algorithm_name,
                        newAuthor.author_algorithm_version});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        try {
            getSourceJdbcTemplate(source).execute(sql);
            Activity activity = new Activity();

            Date today = new java.util.Date();
            activity.activity_datetime = new Timestamp(today.getTime());
            activity.author_id = newAuthorId;
            activity.activity_concept_id = 999;
            addActivity(sourceKey, activity);

            return newAuthorId;
        }
        catch (DataAccessException e) {
            throw e;
        }
    }

    @PUT
    @Path("{sourceKey}/author/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public String updateAuthor(
            @PathParam("sourceKey") String sourceKey,
            Author author)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);
        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);

        String sql = ResourceHelper.GetResourceAsString(BASE_SQL_PATH + "/updateAuthor.sql");
        sql = SqlRender.renderSql(sql, new String[] {
                "metadataDatabaseSchema",
                "authorId",
                "authorConceptId",
                "authorHumanFirstName",
                "authorHumanLastName",
                "authorHumanSuffix",
                "authorDescription",
                "authorAlgorithmName",
                "authorAlgorithmVersion"
                },
                new String[] {
                        tableQualifier,
                        author.author_id,
                        String.valueOf(author.author_concept_id),
                        author.author_human_first_name,
                        author.author_human_last_name,
                        author.author_human_suffix,
                        author.author_description,
                        author.author_algorithm_name,
                        author.author_algorithm_version
        });
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        try {
            getSourceJdbcTemplate(source).execute(sql);
            Activity activity = new Activity();

            Date today = new java.util.Date();
            activity.activity_datetime = new Timestamp(today.getTime());
            activity.author_id = author.author_id;
            activity.activity_concept_id = 9999;
            addActivity(sourceKey, activity);
            return author.author_id;
        }
        catch (DataAccessException e) {
            throw e;
        }
    }

    @GET
    @Path("{sourceKey}/metadata/get/{typeConceptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Metadata> getMetadataByType(
            @PathParam("sourceKey") String sourceKey,
            @PathParam("typeConceptId") Integer typeConceptId)
    {
        Source source = getSourceRepository().findBySourceKey(sourceKey);

        String tableQualifier = source.getTableQualifier(SourceDaimon.DaimonType.Metadata);
        String sql = "select * from @metadataDatabaseSchema.metadata " +
                "where metadata_type_concept_id = @typeConceptId";

        sql = SqlRender.renderSql(sql,
                new String[] {"metadataDatabaseSchema", "typeConceptId"},
                new String[] {tableQualifier, String.valueOf(typeConceptId)});
        sql = SqlTranslate.translateSql(sql, source.getSourceDialect());

        return getSourceJdbcTemplate(source).query(sql, this.metadataRowMapper);
    }
}


