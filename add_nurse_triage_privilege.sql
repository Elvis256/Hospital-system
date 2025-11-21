-- Add Nurse Triage Station Privilege

-- Insert the privilege if it doesn't exist
INSERT IGNORE INTO privilege (id, name, description)
SELECT 
    (SELECT COALESCE(MAX(id), 0) + 1 FROM privilege) as id,
    'NurseTriageStation' as name,
    'Access to Nurse Triage Station for patient vital signs and initial assessment' as description
WHERE NOT EXISTS (
    SELECT 1 FROM privilege WHERE name = 'NurseTriageStation'
);

-- Grant the privilege to super admin (role with id 1, typically Super User/Admin)
INSERT IGNORE INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id
FROM webuser_role r
CROSS JOIN privilege p
WHERE r.name = 'Super User' 
AND p.name = 'NurseTriageStation'
AND NOT EXISTS (
    SELECT 1 FROM role_privilege rp 
    WHERE rp.role_id = r.id AND rp.privilege_id = p.id
);

-- Also grant to users directly if they have other nursing privileges
INSERT IGNORE INTO webuser_privilege (webuser_id, privilege_id)
SELECT DISTINCT wp.webuser_id, p.id
FROM webuser_privilege wp
CROSS JOIN privilege p
INNER JOIN privilege p2 ON wp.privilege_id = p2.id
WHERE p2.name = 'NursingWorkBench'
AND p.name = 'NurseTriageStation'
AND NOT EXISTS (
    SELECT 1 FROM webuser_privilege wp2
    WHERE wp2.webuser_id = wp.webuser_id AND wp2.privilege_id = p.id
);

SELECT 'Nurse Triage Station privilege added successfully' AS result;
