-- Delete all sec_role_permission entries tied to permission 'cohortdefinition:%:generate:%:get'.
-- This will give us a clean slate to start assigning this permission to only the "teamproject" roles:
-- TODO - note that this assumes the whole system is running in "teamproject" mode... i.e. it is not configurable now.
DELETE from ${ohdsiSchema}.sec_role_permission where sec_role_permission.permission_id in 
(
SELECT ${ohdsiSchema}.sec_permission.id
FROM ${ohdsiSchema}.sec_permission
where
 sec_permission.value like 'cohortdefinition:%:generate:%:get'
 );
