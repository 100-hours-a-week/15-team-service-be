ALTER TABLE `user_educations`
MODIFY COLUMN `status` enum(
  'GRADUATED',
  'DEFERRED',
  'ENROLLED',
  'DROPPED',
  'COMPLETED',
  'GRADUATION_POSTPONED',
  'ENROLLING',
  'DROPPED_OUT'
) NOT NULL;

UPDATE `user_educations`
SET `status` = 'GRADUATION_POSTPONED'
WHERE `status` = 'DEFERRED';

UPDATE `user_educations`
SET `status` = 'ENROLLING'
WHERE `status` = 'ENROLLED';

UPDATE `user_educations`
SET `status` = 'DROPPED_OUT'
WHERE `status` = 'DROPPED';

ALTER TABLE `user_educations`
MODIFY COLUMN `status` enum(
  'GRADUATED',
  'GRADUATION_POSTPONED',
  'ENROLLING',
  'DROPPED_OUT',
  'COMPLETED'
) NOT NULL;
