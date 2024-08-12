-- todo remove the role "source user" from all users or transform that role in non-system 

delete from ${ohdsiSchema}.sec_role_permission where role_id = 15;

INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
with vocab_source as (
 select source_key
 from ${ohdsiSchema}.source s
   inner join ${ohdsiSchema}.source_daimon sd on s.source_id = sd.source_id 
 where sd.daimon_type = 1
), vocab_perms as (
 select distinct concat(l,m,r) perm
 from (
 select *
 from (values 
		('vocabulary:')
	) t1 (l)
 cross join 
	( select source_key 
	  from vocab_source
	) t2 (m)
 cross join 
	(values
		(':*:get'),
		(':compare:post'),
		(':concept:*:ancestorAndDescendant:get'),
		(':concept:*:get'),
		(':concept:*:related:get'),
		(':included-concepts:count:post'),
		(':lookup:identifiers:ancestors:post'),
		(':lookup:identifiers:post'),
		(':lookup:mapped:post'),
		(':lookup:recommended:post'),
		(':lookup:sourcecodes:post'),
		(':optimize:post'),
		(':resolveConceptSetExpression:post'),
		(':search:*:get'),
		(':search:post')
	) t3 (r)
 ) combined
),
 source_perms as (
 select distinct concat(ls,ms,rs) perm
 from (
 select *
 from (values 
		('source:')
	) t11 (ls)
 cross join 
	( select source_key 
	  from vocab_source
	) t22 (ms)
 cross join 
	(values
		(':access')
	) t33 (rs)
 ) combined
),
 generate_perms as (
 select distinct concat(lg,mg,rg) perm
 from (
 select *
 from (values 
		('cohortdefinition:*:generate:')
	) t111 (lg)
 cross join 
	( select source_key 
	  from vocab_source
	) t222 (mg)
 cross join 
	(values
		(':get')
	) t333 (rg)
 ) combined
)
SELECT DISTINCT 15 role_id, permission_id
    FROM ${ohdsiSchema}.sec_role_permission srp  
       INNER JOIN ${ohdsiSchema}.sec_permission sp ON srp.permission_id = sp.id      
    WHERE 
       sp.value IN (select perm from vocab_perms) 
	   or
	   sp.value IN (select perm from source_perms)
       or
	   sp.value IN (select perm from generate_perms)
       or
       sp.value IN 
          (
		'cohort-characterization:byTags:post',
		'cohort-characterization:check:post',
		'cohort-characterization:get',
		'cohort-characterization:import:post',
		'cohort-characterization:post',
		'cohortanalysis:get',
		'cohortanalysis:post',
		'cohortdefinition:byTags:post',
		'cohortdefinition:check:post',
		'cohortdefinition:checkv2:post',
		'cohortdefinition:get',
		'cohortdefinition:post',
		'cohortdefinition:printfriendly:cohort:post',
		'cohortdefinition:printfriendly:conceptsets:post',
		'cohortdefinition:sql:post',
		'comparativecohortanalysis:get',
		'comparativecohortanalysis:post',
		'conceptset:byTags:post',
		'conceptset:check:post',
		'conceptset:get',
		'conceptset:post',
		'configuration:edit:ui',
		'estimation:check:post',
		'estimation:get',
		'estimation:import:post',
		'estimation:post',
		'executionservice:execution:run:post',
		'feasibility:get',
		'feature-analysis:aggregates:get',
		'feature-analysis:get',
		'feature-analysis:post',
		'ir:byTags:post',
		'ir:check:post',
		'ir:design:post',
		'ir:get',
		'ir:post',
		'ir:sql:post',
		'job:execution:get',
		'job:get',
		'notifications:get',
		'notifications:viewed:get',
		'notifications:viewed:post',
		'pathway-analysis:byTags:post',
		'pathway-analysis:check:post',
		'pathway-analysis:get',
		'pathway-analysis:import:post',
		'pathway-analysis:post',
		'plp:get',
		'plp:post',
		'prediction:get',
		'prediction:import:post',
		'prediction:post',
		'reusable:byTags:post',
		'reusable:get',
		'reusable:post',
		'source:daimon:priority:get',
		'source:priorityVocabulary:get',
		'sqlrender:translate:post',
		'tag:get',
		'tag:multiAssign:post',
		'tag:multiUnassign:post',
		'tag:post',
		'tag:search:get',
		'cohortdefinition:*:exists:get', -- weird one...but is needed / used by UI before saving a new cohortdefinition....
		'conceptset:*:exists:get', -- weird one...but is needed / used by UI before saving a new conceptset....
		'conceptset:*:expression:*:get', -- TODO - taken over from role 10...This one is probably too broad and will need further fixes.
		'conceptset:*:version:get', -- TODO - taken over from role 10...This one is probably too broad and will need further fixes.
		'conceptset:*:copy-name:get' -- TODO - taken over from role 10...This one is probably too broad and will need further fixes.
       )
;


-- COHORT_DEFINITION_SEC_ROLE is our custom view that returns a list of cohort definition ids per role
-- as long as that role has a permission starting with "cohortdefinition:"" for that id. E.g. :
--  cohort_definition_id |      sec_role_name      
-- ----------------------+-------------------------
--                     8 | /gwas_projects/project2
--                     9 | /gwas_projects/project2
--                   300 | /gwas_projects/project1

DROP VIEW IF EXISTS ${ohdsiSchema}.COHORT_DEFINITION_SEC_ROLE;
CREATE VIEW ${ohdsiSchema}.COHORT_DEFINITION_SEC_ROLE AS
  select
    distinct cast(regexp_replace(sec_permission.value,
         '^cohortdefinition:([0-9]+):.*','\1') as integer) as cohort_definition_id,
    sec_role.name as sec_role_name
  from
    ${ohdsiSchema}.sec_role
    inner join ${ohdsiSchema}.sec_role_permission on sec_role.id = sec_role_permission.role_id
    inner join ${ohdsiSchema}.sec_permission on sec_role_permission.permission_id = sec_permission.id
  where
    sec_permission.value ~ 'cohortdefinition:[0-9]+'
;

-- Below we create new "copy:get" permissions specific to each cohort definition (step 1), and
-- then tie these new permissions to the right role, according to the cohort definition id vs role name
-- mapping found in COHORT_DEFINITION_SEC_ROLE (step 2).

SELECT setval('${ohdsiSchema}.sec_permission_sequence', (select max(id)+1 from ${ohdsiSchema}.sec_permission), false);

-- 1. create the sec_permission records:
INSERT INTO ${ohdsiSchema}.sec_permission (value, description)
select 
 concat('cohortdefinition:', cohort_definition_id, ':copy:get'),
 'Copy the specified cohort definition'
from ${ohdsiSchema}.COHORT_DEFINITION_SEC_ROLE
ON CONFLICT (value)
DO NOTHING;

-- 2. insert sec_role_permissions:
INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
Select 
 sec_role.id,
 sec_permission.id
from
 ${ohdsiSchema}.COHORT_DEFINITION_SEC_ROLE 
 join ${ohdsiSchema}.sec_role on COHORT_DEFINITION_SEC_ROLE.sec_role_name = sec_role.name
 join ${ohdsiSchema}.sec_permission on concat('cohortdefinition:', COHORT_DEFINITION_SEC_ROLE.cohort_definition_id, ':copy:get') = sec_permission.value
ON CONFLICT (role_id, permission_id)
DO NOTHING;


-- CTDS/"team project" feature specific - keep only "admin" role assignment... i.e. remove all other
-- role assignments for all users:
DELETE from ${ohdsiSchema}.sec_user_role where role_id != 2; -- role 2 is the standard "admin" role
