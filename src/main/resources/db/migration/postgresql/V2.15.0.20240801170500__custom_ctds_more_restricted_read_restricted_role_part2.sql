-- Delete set of too broad conceptset permissions from role 15:
DELETE from ${ohdsiSchema}.sec_role_permission srp
where srp.role_id = 15 AND srp.permission_id in
(
  Select sp.id from ${ohdsiSchema}.sec_permission sp
  where sp.value IN
  (
		'conceptset:*:expression:*:get', -- taken over from role 10...This one was too broad
		'conceptset:*:version:get', -- taken over from role 10...This one was too broad
		'conceptset:*:copy-name:get' -- taken over from role 10...This one was too broad
  )
)
;


-- CONCEPT_SET_SEC_ROLE is our custom view that returns a list of conceptset ids per role
-- as long as that role has a permission starting with "conceptset:"" for that id. E.g. :
--      concept_set_id   |      sec_role_name      
-- ----------------------+-------------------------
--                     1 | /gwas_projects/project2
--                     2 | /gwas_projects/project2
--                    30 | /gwas_projects/project1

DROP VIEW IF EXISTS ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE;
CREATE VIEW ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE AS
  select
    distinct cast(regexp_replace(sec_permission.value,
         '^conceptset:([0-9]+):.*','\1') as integer) as concept_set_id,
    sec_role.name as sec_role_name
  from
    ${ohdsiSchema}.sec_role
    inner join ${ohdsiSchema}.sec_role_permission on sec_role.id = sec_role_permission.role_id
    inner join ${ohdsiSchema}.sec_permission on sec_role_permission.permission_id = sec_permission.id
  where
    sec_permission.value ~ 'conceptset:[0-9]+'
;

-- Below we create new "expression:*:get", "version:get", "copy-name:get" permissions specific to each conceptset (step 1), and
-- then tie these new permissions to the right role, according to the conceptset id vs role name
-- mapping found in CONCEPT_SET_SEC_ROLE (step 2).

-- step 1. create the sec_permission records:
INSERT INTO ${ohdsiSchema}.sec_permission (value, description)
select
 concat('conceptset:', concept_set_id, ':expression:*:get'),
 'expression:*:get permission, specific to this conceptset'
from ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE
ON CONFLICT (value)
DO NOTHING;

INSERT INTO ${ohdsiSchema}.sec_permission (value, description)
select
 concat('conceptset:', concept_set_id, ':version:get'),
 'version:get permission, specific to this conceptset'
from ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE
ON CONFLICT (value)
DO NOTHING;

INSERT INTO ${ohdsiSchema}.sec_permission (value, description)
select
 concat('conceptset:', concept_set_id, ':copy-name:get'),
 'copy-name:get permission, specific to this conceptset'
from ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE
ON CONFLICT (value)
DO NOTHING;


-- step 2. insert sec_role_permissions:
INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
Select 
 sec_role.id,
 sec_permission.id
from
 ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE 
 join ${ohdsiSchema}.sec_role on CONCEPT_SET_SEC_ROLE.sec_role_name = sec_role.name
 join ${ohdsiSchema}.sec_permission on concat('conceptset:', CONCEPT_SET_SEC_ROLE.concept_set_id, ':expression:*:get') = sec_permission.value
ON CONFLICT (role_id, permission_id)
DO NOTHING;

INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
Select 
 sec_role.id,
 sec_permission.id
from
 ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE 
 join ${ohdsiSchema}.sec_role on CONCEPT_SET_SEC_ROLE.sec_role_name = sec_role.name
 join ${ohdsiSchema}.sec_permission on concat('conceptset:', CONCEPT_SET_SEC_ROLE.concept_set_id, ':version:get') = sec_permission.value
ON CONFLICT (role_id, permission_id)
DO NOTHING;

INSERT INTO ${ohdsiSchema}.sec_role_permission (role_id, permission_id)
Select 
 sec_role.id,
 sec_permission.id
from
 ${ohdsiSchema}.CONCEPT_SET_SEC_ROLE 
 join ${ohdsiSchema}.sec_role on CONCEPT_SET_SEC_ROLE.sec_role_name = sec_role.name
 join ${ohdsiSchema}.sec_permission on concat('conceptset:', CONCEPT_SET_SEC_ROLE.concept_set_id, ':copy-name:get') = sec_permission.value
ON CONFLICT (role_id, permission_id)
DO NOTHING;
