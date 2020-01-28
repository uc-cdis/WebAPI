INSERT INTO ${ohdsiSchema}.sec_permission (id, value, description)
SELECT nextval('${ohdsiSchema}.sec_permission_id_seq'), 'source:priorityVocabulary:get', 'Get source with highest priority vocabulary daimon'
;

INSERT INTO ${ohdsiSchema}.sec_permission (id, value, description)
SELECT nextval('${ohdsiSchema}.sec_permission_id_seq'), 'cohort-characterization:generation:*:result:get', 'Get cohort characterization generation results - 2.7.x compatible'
;

INSERT INTO ${ohdsiSchema}.sec_role_permission(role_id, permission_id)
SELECT sr.id, sp.id
FROM ${ohdsiSchema}.sec_permission SP, ${ohdsiSchema}.sec_role sr
WHERE sp.value IN (
  'source:priorityVocabulary:get',
  'cohort-characterization:generation:*:result:get'
)
AND sr.name IN ('Atlas users', 'Moderator')
;