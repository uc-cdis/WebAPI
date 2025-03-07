-- This script will add cohort-characterization permissions to role_id = 15

INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
with vocab_source as (
 select source_key
 from ${ohdsiSchema}.source s
   inner join ${ohdsiSchema}.source_daimon sd on s.source_id = sd.source_id
 where sd.daimon_type = 1
),
 char_generate_perms as (
 select distinct concat(lcg,mcg,rcg) perm
 from (
 select *
 from (values
		('cohort-characterization:*:generation:')
	) t1111 (lcg)
 cross join
	( select source_key
	  from vocab_source
	) t2222 (mcg)
 cross join
	(values
		(':post')
	) t3333 (rcg)
 ) combined
) -- TODO - consider also adding "cdmresults:EUNOMIA:conceptRecordCount:post" (where EUNOMIA is a vocab_source.source_key example)
SELECT DISTINCT 15 role_id, permission_id
    FROM ${ohdsiSchema}.sec_role_permission srp
       INNER JOIN ${ohdsiSchema}.sec_permission sp ON srp.permission_id = sp.id
    WHERE
	   sp.value IN (select perm from char_generate_perms)
       or
       sp.value IN
          (
		'cohort-characterization:*:exists:get', -- weird one...but is needed / used by UI before saving a new cohort-characterization....
		'feature-analysis:*:exists:get', -- weird one...but is needed / used by UI
		'feature-analysis:*:get' -- TODO - currently needed for accessing standard feature records (and features created by others) - will need extra work to isolate
       )
;

-- COHORT_CHARACTERIZATION_SEC_ROLE is our custom view that returns a list of cohort characterization ids per role
-- when the role has a permission starting with "cohort-characterization:<SOME ID>". E.g. how view output looks like:
--
--  cohort_characterization_id |      sec_role_name
-- ----------------------------+-------------------------
--                     8 	   | /gwas_projects/project2
--                     9       | /gwas_projects/project2
--                   300       | /gwas_projects/project1

DROP VIEW IF EXISTS ${ohdsiSchema}.COHORT_CHARACTERIZATION_SEC_ROLE;
CREATE VIEW ${ohdsiSchema}.COHORT_CHARACTERIZATION_SEC_ROLE AS
  select
    distinct cast(regexp_replace(sec_permission.value,
         '^cohort-characterization:([0-9]+):.*','\1') as integer) as cohort_characterization_id,
    sec_role.name as sec_role_name
  from
    ${ohdsiSchema}.sec_role
    inner join ${ohdsiSchema}.sec_role_permission on sec_role.id = sec_role_permission.role_id
    inner join ${ohdsiSchema}.sec_permission on sec_role_permission.permission_id = sec_permission.id
  where
    sec_permission.value ~ 'cohort-characterization:[0-9]+'
;

-- TODO - solve all todo's above regarding too broad permissions
-- >> see also how this was done for conceptsets in the java code and in the migration script V2.15.0.20240801170500__custom_ctds_more_restricted_read_restricted_role_part2.sql
