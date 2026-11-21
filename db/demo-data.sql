-- ============================================================================
-- WorkMates demo data (Sprint 1 + Sprint 2 system testing / class demo)
--
-- WIPES EVERY ROW in every table of the public schema, then loads fake data.
-- Run it as often as you like. Only run it against a local database.
-- Many scenarios below change data once you run them, so reload this file before repeating a test.
--
--   psql -h localhost -U postgres -d workmates -f db/demo-data.sql
--   (or run the whole file as one script in the IDEA database console)
--
-- Needs the pgcrypto extension (ships with PostgreSQL) to make BCrypt hashes.
-- Every account below has the same password:  Demo1234
--
-- ---------------------------------------------------------------------------
-- ACCOUNTS
-- ---------------------------------------------------------------------------
--   Students (profile: tags / expected rate / free slots)
--     aoife.murphy@student.tus.ie  MAIN DEMO STUDENT. complete; java,python,sql,english,customer service; €14;
--                                  free Mon 17-21, Wed 15-18, Fri 14-18, Sat 9-17. One application in every status.
--     liam.obrien@student.tus.ie   complete; €13.50; free Tue eve, Sat, Sun (hospitality / retail)
--     wei.zhang@student.tus.ie     complete; €18; free Mon-Thu 18-21 (tutoring / data); no phone
--     sophie.walsh@student.tus.ie  EMPTY profile (no tags, rate or slots); no applications, no notifications
--     niamh.kelly@student.tus.ie   PARTIAL profile: tags + slots, NO expected rate; no applications
--     conor.byrne@student.tus.ie   complete; applied to 23 postings (every one except Marketing Intern, Lifeguard)
--     test.student001..105@student.tus.ie  bulk students, all applied to Event Steward (Concerts)
--   Employers
--     lakeside.cafe@example.com          Athlone, 5 postings (Kitchen Porter closed Manual, Summer Barista Filled)
--     shannon.tutoring@example.com       Limerick, 4 open postings
--     brightbyte@example.com             Athlone / Remote, 5 open postings (incl. the full-time one)
--     riverside.hospitality@example.com  Limerick / Remote, 7 open postings; 108 unread notifications ("99+")
--     midlands.fitness@example.com       Athlone, 3 open postings + Open Day Helper (closed, Filled)
--     athlone.events@example.com         NO postings, NO notifications (empty states)
--
-- Open postings: 22, so the job list has a second page (20 per page). 25 postings in total.
--
-- ---------------------------------------------------------------------------
-- SPRINT 1 CHECKS (still valid)
-- ---------------------------------------------------------------------------
--   Fits my time for Aoife: Maths Tutor 75% (Wed 14-18 vs free 15-18, the PRD example), Weekend Barista
--   exactly 50% (kept), Full-time Support Engineer 13% (filtered out). Sophie: the filter is disabled.
--   Weekend Barista: 2 hired of 3 -> lowering the headcount below 2 in the edit form is refused.
--
-- ---------------------------------------------------------------------------
-- SPRINT 2 SCENARIOS  (acceptance criterion -> account -> action -> expected result)
-- Scores below were recomputed from the data with the MatchScoreCalculator formulas.
-- Statuses / notifications are those right after loading; several scenarios change them, reload to reset.
-- ---------------------------------------------------------------------------
-- US 3.3 Rank jobs by match score
--   Aoife, /jobs: top of the list (exact score desc, then newest):
--     1 Junior Java Developer 92% (time 100, skills 3/4, pay 100)   2 Coding Club Mentor 89%
--     3 Gym Receptionist 84%   4 Fitness Class Assistant 83%   5 QA Tester 83%   6 Data Cleaning Assistant 72%
--     7 Sales Assistant (Weekends) 71%   8 Weekend Barista 71%   9 Full-time Support Engineer 71%   10 Maths Tutor 69%
--     bottom: Irish Language Tutor, Stock Room Assistant, Lifeguard (33% each, newest first)
--   Tie -> newest first: Sales Assistant and Weekend Barista have the same exact score (0.710317); Sales
--     Assistant (4 days old) is above Weekend Barista (20 days). Same for Fitness Class (13 d) above QA Tester (14 d).
--   Exact score, not the rounded one: Full-time Support Engineer also shows 71% but is exact 0.708333, so it is 9th.
--   Pay is linear: Weekend Barista €13.50 vs expected €14 -> pay 96%; Lunch Rush Server €13.75 -> 98%.
--   Missing part = 0: Niamh (no rate) has pay 0 everywhere; her top job is Hotel Breakfast Waiter 67% (100/3 of 3/0).
--   Empty profile: Sophie sees every job at 0%, ordered newest first (Event Steward, Junior Java, Maths Tutor, ...),
--     plus the "Complete your profile to get better matches" banner.
--   Profile change: Aoife adds the tag "spring boot" -> reload -> Junior Java Developer becomes 100%.
--   Filters still apply: Aoife + area Remote -> only Junior Java, Data Cleaning, Marketing Intern, React Intern.
-- US 3.4 Job detail with match breakdown
--   Aoife opens Leaving Cert Maths Tutor: 69%, time 75% (Wed 14:00-15:00 not covered), skills 1 of 3, missing
--     maths, tutoring; pay €22.00 vs €14.00. Apply button shown (she has not applied).
--   Aoife opens Junior Java Developer: status "Applied" instead of the Apply button; missing tag: spring boot.
--   Aoife opens Kitchen Porter by its URL (closed, Manual): "This posting is closed", no Apply button.
-- US 4.1 One-click apply
--   Aoife applies to Leaving Cert Maths Tutor -> Applied, toast; Shannon gets "Aoife Murphy applied for ...".
--   Apply again (Swagger: POST /api/postings/{id}/applications) -> "You have already applied to this job".
--   Sophie applies to any job -> refused, missing tags, expectedHourlyRate, timeSlots; link to profile.
--   Niamh applies to Hotel Breakfast Waiter -> refused, missing only expectedHourlyRate.
--   POST to Kitchen Porter (closed) -> "This posting is closed". Any employer token -> refused (students only).
-- US 4.2 Employer views and filters applicants
--   Riverside, Event Steward (Concerts): 106 applicants -> 6 pages of 20. Status filter: Applied 91, Reviewed 7,
--     Interview 4, Rejected 4. Sort by match: 3 at 100% on top (test.student018 Orla Kelly,
--     072 Ronan Nolan, 090 Maeve Burke), lowest 47%; 34 different scores.
--   BrightByte, Junior Java Developer: 7 applicants, one in each of Applied (Aoife, Conor), Reviewed (Wei),
--     Interview (Liam), Offer (test.student002), Declined (test.student003), Rejected (test.student004).
--   Applicant detail: Aoife shows phone 0851234567; Wei shows no phone.
--   Empty state "No applications yet": Riverside -> Marketing Intern; Midlands -> Lifeguard (Evenings).
--   Ownership: Lakeside opening a BrightByte applicant URL -> 404.
-- US 4.3 Employer advances or rejects (BrightByte, Junior Java Developer)
--   Aoife Applied -> Reviewed; Wei Reviewed -> Interview; Liam Interview -> Offer. Each student gets
--     "Junior Java Developer (Internship): your application moved to X".
--   Reject from Applied (Conor), Reviewed (Wei) or Interview (Liam): asks for confirmation, then Rejected.
--   test.student002 (Offer): no reject button; Swagger POST /status REJECTED -> 409 "... Current status: Offer".
--   test.student003 (Declined) / test.student004 (Rejected): final, any change -> 409.
--   Skip a step: Swagger POST /status OFFER on Aoife's Applied -> 409 "This status change is not allowed.
--     Current status: Applied".
--   Closed posting: Lakeside -> Kitchen Porter (closed by hand) -> Wei Interview -> Offer still works.
--   CAUTION: if test.student002 accepts, Junior Java (headcount 1) fills and rejects everyone else; reload after.
-- US 4.4 Student accepts or declines an offer
--   Aoife, Data Cleaning Assistant (Offer) -> Decline -> Declined; BrightByte gets "Aoife Murphy declined ...".
--   Aoife, Coding Club Mentor (Offer) -> Accept -> see US 4.5 scenario S1.
--   Race S2 - Riverside, Bar Staff (Evenings), headcount 2, 1 already hired (test.student001); Liam and Wei
--     both hold an Offer. Liam and Wei accept at the same time (two browsers): only one succeeds. Or in turn:
--     Wei accepts -> posting fills, Liam's Offer becomes Rejected (Position filled); Liam, on a page opened
--     before, clicks Accept -> "Sorry, this position has just been filled".
--   Answer a non-Offer (Swagger, Aoife's Gym Receptionist Interview -> ACCEPTED) -> 409 "... Current status: Interview".
-- US 4.5 Auto-close on fill with auto-reject
--   S1 accept fills - Shannon, Coding Club Mentor, headcount 2, Wei already Accepted. Aoife accepts her Offer ->
--     posting Closed / Filled and gone from /jobs; Liam (Interview) and Conor (Applied) become Rejected by
--     SYSTEM with "Position filled" and get a notification; Shannon gets "Aoife Murphy accepted the offer ..."
--     and "Coding Club Mentor is filled and closed"; applicants page banner: "2 remaining applications were set
--     to Rejected". Applying to it afterwards -> "This posting is closed".
--   S3 headcount edit fills - Lakeside, Weekend Barista, headcount 3: Liam Accepted + Wei Completed = 2 hired,
--     test.student005 Offer, Conor Applied. Edit headcount to 2 -> Closed / Filled, both rejected (the Offer too),
--     2 Position filled notifications + Posting filled to Lakeside. Edit to 1 instead -> refused.
--   Offers do not count: Bar Staff has 1 Accepted + 2 Offers on headcount 2 and is still Open.
--   Already filled: Summer Barista (Liam hired; Aoife from Offer, Wei, Conor rejected by the system) and
--     Open Day Helper (Aoife Completed; Conor rejected) show close reason Filled in My postings.
-- US 4.6 Student tracks application status (Aoife, /applications)
--   10 applications, newest update first: Coding Club Mentor, Gym Receptionist, Junior Java, QA Tester,
--     Data Cleaning, Lunch Rush, Evening Counter, Hotel Breakfast, Open Day Helper, Summer Barista.
--   Groups: In progress 3 (Junior Java Applied, QA Tester Reviewed, Gym Receptionist Interview); Offer 2
--     (Coding Club, Data Cleaning); Accepted 1 (Evening Counter); Finished 4 (Open Day Helper Completed,
--     Lunch Rush Declined, Hotel Breakfast Rejected by employer, Summer Barista Rejected - "Position filled").
--   Detail of Summer Barista: history Applied -> Reviewed -> Interview -> Offer -> Rejected (system), with times;
--     employer contact lakeside.cafe@example.com, 0906401234. Open Day Helper: Midlands has no phone.
--   Paging: Conor has 23 applications -> 2 pages. Empty state: Sophie, Niamh.
-- US 4.7 Mark an application completed
--   Aoife, Evening Counter Assistant (Accepted) -> Mark as completed -> Lakeside notified; posting stays 1 of 2 hired.
--   Lakeside, Weekend Barista -> Liam (Accepted) -> Mark as completed -> Liam notified; still counts as hired.
--   Swagger POST /status COMPLETED on any non-Accepted application -> 409.
-- US 5.1 Notifications on status changes
--   Every seeded history step has a matching notification to the other party (texts as NotificationMessages);
--     nobody has a notification for their own action. Run any scenario above, then check the other party's bell.
-- US 5.2 Notification centre (total / unread right after loading; older than 3 days = read)
--   Aoife 26 / 7 -> 2 pages; one points at a deleted application ("Christmas Market Helper") -> opens
--     "This item is no longer available". Riverside 117 / 108 -> bell shows "99+". Lakeside 21 / 1.
--   Shannon 10 / 4, BrightByte 14 / 8, Midlands 7 / 0, Liam 14 / 7, Wei 18 / 7, Conor 2 / 0.
--   Empty state "You have no notifications yet": Sophie, Niamh, Athlone Events.
-- ============================================================================
 
CREATE EXTENSION IF NOT EXISTS pgcrypto;
 
BEGIN;
 
-- ---------- wipe ----------
 
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE', t);
    END LOOP;
END $$;
 
 
-- ---------- users and profiles ----------
 
CREATE TEMP TABLE seed_user (
    email    text PRIMARY KEY,
    role     text,
    name     text
) ON COMMIT DROP;
 
INSERT INTO seed_user VALUES
    ('aoife.murphy@student.tus.ie',       'STUDENT',  'Aoife Murphy'),
    ('liam.obrien@student.tus.ie',        'STUDENT',  'Liam O''Brien'),
    ('wei.zhang@student.tus.ie',          'STUDENT',  'Wei Zhang'),
    ('sophie.walsh@student.tus.ie',       'STUDENT',  'Sophie Walsh'),
    ('niamh.kelly@student.tus.ie',        'STUDENT',  'Niamh Kelly'),
    ('conor.byrne@student.tus.ie',        'STUDENT',  'Conor Byrne'),
    ('lakeside.cafe@example.com',         'EMPLOYER', 'Lakeside Café'),
    ('shannon.tutoring@example.com',      'EMPLOYER', 'Shannon Tutoring Centre'),
    ('brightbyte@example.com',            'EMPLOYER', 'BrightByte Software'),
    ('riverside.hospitality@example.com', 'EMPLOYER', 'Riverside Hospitality'),
    ('midlands.fitness@example.com',      'EMPLOYER', 'Midlands Fitness Club'),
    ('athlone.events@example.com',        'EMPLOYER', 'Athlone Events Co');
 
-- 105 bulk students test.student001 .. test.student105 (names built from two lists).
INSERT INTO seed_user
SELECT format('test.student%s@student.tus.ie', lpad(i::text, 3, '0')),
       'STUDENT',
       (ARRAY['Ciara','Sean','Orla','Cian','Roisin','Darragh','Aisling','Eoin','Clodagh','Padraig',
              'Grainne','Ronan','Saoirse','Fionn','Maeve'])[1 + (i - 1) % 15]
         || ' ' ||
       (ARRAY['Doyle','Kelly','Ryan','Byrne','Nolan','Burke','Quinn','Lynch','Dunne','Healy'])[1 + (i - 1) / 15]
FROM generate_series(1, 105) AS i;
 
-- One BCrypt hash for all accounts (Spring's BCryptPasswordEncoder accepts the $2a$ prefix).
CREATE TEMP TABLE seed_hash ON COMMIT DROP AS
SELECT crypt('Demo1234', gen_salt('bf', 10)) AS hash;
 
INSERT INTO users (email, password_hash, role, name)
SELECT s.email, h.hash, s.role, s.name
FROM seed_user s
CROSS JOIN seed_hash h
ORDER BY s.role DESC, s.email LIKE 'test.%', s.email;   -- named students, bulk students, employers
 
INSERT INTO student_profiles (user_id, phone, major, year_of_study, expected_hourly_rate)
SELECT u.id, v.phone, v.major, v.year_of_study, v.rate
FROM (VALUES
    ('aoife.murphy@student.tus.ie', '0851234567', 'Software Development',    'Year 3', 14.00),
    ('liam.obrien@student.tus.ie',  '0867654321', 'Business Studies',        'Year 2', 13.50),
    ('wei.zhang@student.tus.ie',    NULL,         'MSc Data Analytics',      'Year 1', 18.00),
    ('sophie.walsh@student.tus.ie', NULL,         'Nursing',                 'Year 1', NULL),
    ('niamh.kelly@student.tus.ie',  '0871112233', 'Hospitality Management',  'Year 2', NULL),
    ('conor.byrne@student.tus.ie',  '0835550101', 'Mechanical Engineering',  'Year 4', 15.00)
) AS v(email, phone, major, year_of_study, rate)
JOIN users u ON u.email = v.email;
 
INSERT INTO student_profiles (user_id, phone, major, year_of_study, expected_hourly_rate)
SELECT u.id, NULL,
       (ARRAY['Applied Computing','Business Studies','Engineering','Sport Science','Nursing',
              'Hospitality Management'])[1 + i % 6],
       'Year ' || (1 + i % 4),
       12 + i % 8                                         -- 12.00 .. 19.00
FROM generate_series(1, 105) AS i
JOIN users u ON u.email = format('test.student%s@student.tus.ie', lpad(i::text, 3, '0'));
 
INSERT INTO employer_profiles (user_id, display_name, bio, contact_email, phone)
SELECT u.id, v.display_name, v.bio, u.email, v.phone
FROM (VALUES
    ('lakeside.cafe@example.com',         'Lakeside Café',
     'Family-run café on the shore of the Shannon. Busy at weekends, friendly team, flexible shifts for students.', '0906401234'),
    ('shannon.tutoring@example.com',      'Shannon Tutoring Centre',
     'Small-group tutoring for Leaving Cert and first-year college students in Limerick.', '0614005678'),
    ('brightbyte@example.com',            'BrightByte Software',
     'Athlone software start-up building scheduling tools. We mentor interns and hire from TUS.', NULL),
    ('riverside.hospitality@example.com', 'Riverside Hospitality',
     'Bars, a hotel restaurant and a retail shop in Limerick city centre.', '0619001122'),
    ('midlands.fitness@example.com',      'Midlands Fitness Club',
     'Gym and swimming pool in Athlone. Evening and weekend shifts.', NULL),
    ('athlone.events@example.com',        'Athlone Events Co',
     NULL, NULL)
) AS v(email, display_name, bio, phone)
JOIN users u ON u.email = v.email;
 
 
-- ---------- student tags and free time slots ----------
-- Slot format: 'DAY hh:mm-hh:mm;DAY hh:mm-hh:mm'  (DAY = MON..SUN, 30-minute steps).
 
CREATE TEMP TABLE seed_student (
    email text PRIMARY KEY,
    tags  text,
    slots text
) ON COMMIT DROP;
 
INSERT INTO seed_student VALUES
    ('aoife.murphy@student.tus.ie', 'java,python,sql,english,customer service',
     'MON 17:00-21:00;WED 15:00-18:00;FRI 14:00-18:00;SAT 09:00-17:00'),
    ('liam.obrien@student.tus.ie',  'barista,customer service,english,irish,retail',
     'TUE 17:00-22:00;SAT 08:00-18:00;SUN 10:00-18:00'),
    ('wei.zhang@student.tus.ie',    'mandarin,english,maths,python,tutoring',
     'MON 18:00-21:00;TUE 18:00-21:00;WED 18:00-21:00;THU 18:00-21:00'),
    ('sophie.walsh@student.tus.ie', '', ''),
    ('niamh.kelly@student.tus.ie',  'hospitality,customer service,english',
     'SAT 07:00-12:00;SUN 07:00-12:00;MON 07:00-11:00'),
    ('conor.byrne@student.tus.ie',  'english,teamwork,retail',
     'SAT 09:00-18:00;SUN 09:00-18:00');
 
-- Bulk students: always english, plus teamwork / events / customer service by number;
-- one Saturday slot from 14:00..22:00 to midnight, so Event Steward scores differ.
INSERT INTO seed_student
SELECT format('test.student%s@student.tus.ie', lpad(i::text, 3, '0')),
       concat_ws(',', 'english',
                 CASE WHEN i % 2 = 0 THEN 'teamwork' END,
                 CASE WHEN i % 3 = 0 THEN 'events' END,
                 CASE WHEN i % 5 = 0 THEN 'customer service' END),
       format('SAT %s:00-24:00', 14 + i % 9)
FROM generate_series(1, 105) AS i;
 
INSERT INTO tags (name)
SELECT DISTINCT trim(t) FROM seed_student, unnest(string_to_array(tags, ',')) AS t WHERE trim(t) <> '';
 
INSERT INTO student_tags (student_id, tag_id)
SELECT u.id, g.id
FROM seed_student s
JOIN users u ON u.email = s.email
CROSS JOIN unnest(string_to_array(s.tags, ',')) AS t
JOIN tags g ON g.name = trim(t);
 
INSERT INTO student_time_slots (student_id, day_of_week, start_minute, end_minute)
SELECT u.id,
       array_position(ARRAY['MON','TUE','WED','THU','FRI','SAT','SUN'], split_part(slot, ' ', 1)),
       split_part(split_part(split_part(slot, ' ', 2), '-', 1), ':', 1)::int * 60
         + split_part(split_part(split_part(slot, ' ', 2), '-', 1), ':', 2)::int,
       split_part(split_part(split_part(slot, ' ', 2), '-', 2), ':', 1)::int * 60
         + split_part(split_part(split_part(slot, ' ', 2), '-', 2), ':', 2)::int
FROM seed_student s
JOIN users u ON u.email = s.email
CROSS JOIN unnest(string_to_array(s.slots, ';')) AS slot
WHERE s.slots <> '';
 
 
-- ---------- postings ----------
-- days_ago orders the list (newest first). closed_days_ago is set only for CLOSED rows.
-- For a FILLED posting, closed_days_ago is the moment the last place was accepted.
-- Titles are unique; the applications section below finds postings by title.
 
CREATE TEMP TABLE seed_posting (
    employer        text,
    title           text PRIMARY KEY,
    job_type        text,
    hourly_rate     numeric(8,2),
    weekly_hours    numeric(4,1),
    area            text,
    address         text,
    description     text,
    headcount       int,
    tags            text,
    slots           text,
    days_ago        int,
    close_reason    text,
    closed_days_ago int
) ON COMMIT DROP;
 
INSERT INTO seed_posting VALUES
-- Lakeside Café (Athlone)
('lakeside.cafe@example.com', 'Weekend Barista', 'PART_TIME', 13.50, 12, 'ATHLONE',
 'Lakeside Café, Church Street, Athlone',
 'Make coffee and serve customers at a busy lakeside café. Full training provided.', 3,
 'barista,customer service,english', 'SAT 09:00-15:00;SUN 09:00-15:00', 20, NULL, NULL),
('lakeside.cafe@example.com', 'Evening Counter Assistant', 'PART_TIME', 14.00, 10, 'ATHLONE',
 'Lakeside Café, Church Street, Athlone',
 'Evening counter shifts: take orders, handle the till and close up.', 2,
 'customer service,english,cash handling', 'THU 17:00-22:00;FRI 17:00-22:00', 15, NULL, NULL),
('lakeside.cafe@example.com', 'Lunch Rush Server', 'PART_TIME', 13.75, 15, 'ATHLONE',
 'Lakeside Café, Church Street, Athlone',
 'Weekday lunch cover. Quick on your feet and good with people.', 2,
 'customer service,english', 'MON 11:30-14:30;TUE 11:30-14:30;WED 11:30-14:30;THU 11:30-14:30;FRI 11:30-14:30', 9, NULL, NULL),
('lakeside.cafe@example.com', 'Kitchen Porter', 'PART_TIME', 13.50, 8, 'ATHLONE',
 'Lakeside Café, Church Street, Athlone',
 'Wash up and keep the kitchen clean during the Saturday and Sunday rush.', 1,
 'teamwork,english', 'SAT 10:00-14:00;SUN 10:00-14:00', 40, 'MANUAL', 10),
('lakeside.cafe@example.com', 'Summer Barista', 'PART_TIME', 13.50, 20, 'ATHLONE',
 'Lakeside Café, Church Street, Athlone',
 'Summer cover for our lakeside terrace.', 1,
 'barista,english', 'MON 09:00-14:00;TUE 09:00-14:00;WED 09:00-14:00;THU 09:00-14:00', 60, 'FILLED', 45),
 
-- Shannon Tutoring Centre (Limerick)
('shannon.tutoring@example.com', 'Leaving Cert Maths Tutor', 'PART_TIME', 22.00, 4, 'LIMERICK',
 'Shannon Tutoring Centre, O''Connell Street, Limerick',
 'Teach small groups of Leaving Cert students. Ideal for maths, science or engineering students.', 2,
 'maths,tutoring,english', 'WED 14:00-18:00', 3, NULL, NULL),
('shannon.tutoring@example.com', 'Irish Language Tutor', 'PART_TIME', 20.00, 6, 'LIMERICK',
 'Shannon Tutoring Centre, O''Connell Street, Limerick',
 'Help second-level students prepare for the oral Irish exam.', 1,
 'irish,tutoring', 'TUE 16:00-19:00;THU 16:00-19:00', 12, NULL, NULL),
('shannon.tutoring@example.com', 'Coding Club Mentor', 'PART_TIME', 18.00, 6, 'LIMERICK',
 'Shannon Tutoring Centre, O''Connell Street, Limerick',
 'Run an after-school coding club for teenagers using Scratch and beginner Python.', 2,
 'java,python,tutoring', 'MON 17:00-20:00;FRI 15:00-18:00', 5, NULL, NULL),
('shannon.tutoring@example.com', 'Front Desk Receptionist', 'PART_TIME', 14.50, 12, 'LIMERICK',
 'Shannon Tutoring Centre, O''Connell Street, Limerick',
 'Welcome families, manage bookings and keep the Excel timetable up to date.', 1,
 'customer service,english', 'MON 09:00-13:00;WED 09:00-13:00;FRI 09:00-13:00', 18, NULL, NULL),
 
-- BrightByte Software (Athlone / Remote)
('brightbyte@example.com', 'Junior Java Developer (Internship)', 'INTERNSHIP', 15.00, 16, 'REMOTE',
 'Remote, with a monthly team day in Athlone',
 'Build REST APIs with Spring Boot on a real product team. Mentoring from senior engineers.', 1,
 'java,spring boot,sql,english', 'MON 17:00-21:00;WED 15:00-18:00;FRI 14:00-18:00;SAT 09:00-14:00', 2, NULL, NULL),
('brightbyte@example.com', 'React Frontend Intern', 'INTERNSHIP', 15.00, 10, 'REMOTE',
 'Remote, with a monthly team day in Athlone',
 'Build UI components with React and TypeScript and review designs with our product team.', 2,
 'react,typescript,english', 'TUE 10:00-15:00;THU 10:00-15:00', 7, NULL, NULL),
('brightbyte@example.com', 'Data Cleaning Assistant', 'PART_TIME', 14.00, 8, 'REMOTE',
 'Remote',
 'Clean and check spreadsheets for our analytics team.', 1,
 'python,data,english', 'SAT 10:00-14:00;SUN 10:00-14:00', 11, NULL, NULL),
('brightbyte@example.com', 'QA Tester', 'PART_TIME', 16.00, 12, 'ATHLONE',
 'BrightByte Software, Athlone Business Park, Athlone',
 'Test new releases, write bug reports and help us automate regression checks.', 1,
 'testing,sql,english', 'MON 17:00-21:00;WED 16:00-20:00;FRI 14:00-18:00', 14, NULL, NULL),
('brightbyte@example.com', 'Full-time Support Engineer', 'FULL_TIME', 21.00, 40, 'ATHLONE',
 'BrightByte Software, Athlone Business Park, Athlone',
 'First-line customer support for our scheduling product. Monday to Friday, office based.', 1,
 'sql,customer service,english', 'MON 09:00-17:00;TUE 09:00-17:00;WED 09:00-17:00;THU 09:00-17:00;FRI 09:00-17:00', 25, NULL, NULL),
 
-- Riverside Hospitality (Limerick / Remote)
('riverside.hospitality@example.com', 'Sales Assistant (Weekends)', 'PART_TIME', 13.50, 16, 'LIMERICK',
 'Riverside Retail, Arthur''s Quay, Limerick',
 'Serve customers, restock shelves and run the till in our city centre shop.', 3,
 'retail,customer service,english', 'SAT 09:00-17:00;SUN 10:00-18:00', 4, NULL, NULL),
('riverside.hospitality@example.com', 'Stock Room Assistant', 'PART_TIME', 14.00, 10, 'LIMERICK',
 'Riverside Retail, Arthur''s Quay, Limerick',
 'Receive deliveries and keep the stock room organised.', 2,
 'retail,teamwork', 'MON 08:00-13:00;TUE 08:00-13:00', 22, NULL, NULL),
('riverside.hospitality@example.com', 'Bar Staff (Evenings)', 'PART_TIME', 15.00, 12, 'LIMERICK',
 'The Treaty Tap, Thomas Street, Limerick',
 'Pull pints and serve drinks on busy Friday and Saturday nights.', 2,
 'bartending,customer service,english', 'FRI 17:00-23:00;SAT 17:00-23:00', 6, NULL, NULL),
('riverside.hospitality@example.com', 'Hotel Breakfast Waiter', 'PART_TIME', 14.00, 12, 'LIMERICK',
 'Riverside Hotel, Shannon Street, Limerick',
 'Early shifts serving breakfast to hotel guests.', 2,
 'hospitality,customer service,english', 'SAT 07:00-11:00;SUN 07:00-11:00;MON 07:00-11:00', 16, NULL, NULL),
('riverside.hospitality@example.com', 'Event Steward (Concerts)', 'PART_TIME', 15.50, 8, 'LIMERICK',
 'Riverside Arena, Limerick',
 'Welcome guests and help with crowd flow at Saturday evening concerts.', 4,
 'events,teamwork,english', 'SAT 16:00-24:00', 1, NULL, NULL),
('riverside.hospitality@example.com', 'Delivery Driver (Part-time)', 'PART_TIME', 15.00, 12, 'LIMERICK',
 'Riverside Retail, Arthur''s Quay, Limerick',
 'Deliver online orders around Limerick. Full driving licence needed.', 1,
 'driving licence,english', 'TUE 17:00-21:00;THU 17:00-21:00;SUN 12:00-16:00', 28, NULL, NULL),
('riverside.hospitality@example.com', 'Marketing Intern', 'INTERNSHIP', 14.00, 12, 'REMOTE',
 'Remote',
 'Create social media posts and help plan our summer promotions.', 1,
 'social media,english', 'WED 10:00-16:00;THU 10:00-16:00', 8, NULL, NULL),
 
-- Midlands Fitness Club (Athlone)
('midlands.fitness@example.com', 'Gym Receptionist', 'PART_TIME', 14.00, 15, 'ATHLONE',
 'Midlands Fitness Club, Dublin Road, Athlone',
 'Check members in, answer questions and keep the front desk tidy.', 2,
 'customer service,english', 'MON 16:00-21:00;WED 16:00-21:00;FRI 16:00-21:00', 10, NULL, NULL),
('midlands.fitness@example.com', 'Lifeguard (Evenings)', 'PART_TIME', 16.50, 10, 'ATHLONE',
 'Midlands Fitness Club, Dublin Road, Athlone',
 'Supervise the pool in the evening. Pool lifeguard qualification required.', 1,
 'lifeguard,first aid,swimming', 'TUE 18:00-21:00;THU 18:00-21:00;SUN 10:00-14:00', 30, NULL, NULL),
('midlands.fitness@example.com', 'Fitness Class Assistant', 'PART_TIME', 14.00, 5, 'ATHLONE',
 'Midlands Fitness Club, Dublin Road, Athlone',
 'Set up the studio and welcome people to Saturday morning classes.', 1,
 'fitness,customer service', 'SAT 09:00-14:00', 13, NULL, NULL),
('midlands.fitness@example.com', 'Open Day Helper', 'PART_TIME', 14.00, 6, 'ATHLONE',
 'Midlands Fitness Club, Dublin Road, Athlone',
 'Greet visitors and run tours of the club at our September open day.', 1,
 'customer service,english', 'SAT 10:00-16:00', 50, 'FILLED', 33);
 
INSERT INTO tags (name)
SELECT DISTINCT trim(t) FROM seed_posting, unnest(string_to_array(tags, ',')) AS t
ON CONFLICT (name) DO NOTHING;
 
INSERT INTO postings (employer_id, title, job_type, hourly_rate, weekly_hours, area, address, description,
                      headcount, status, close_reason, closed_at, created_at, updated_at)
SELECT u.id, p.title, p.job_type, p.hourly_rate, p.weekly_hours, p.area, p.address, p.description,
       p.headcount,
       CASE WHEN p.close_reason IS NULL THEN 'OPEN' ELSE 'CLOSED' END,
       p.close_reason,
       now() - make_interval(days => p.closed_days_ago),
       now() - make_interval(days => p.days_ago),
       now() - make_interval(days => coalesce(p.closed_days_ago, p.days_ago))
FROM seed_posting p
JOIN users u ON u.email = p.employer
ORDER BY p.days_ago DESC;   -- oldest first, so ids grow with creation time
 
INSERT INTO posting_tags (posting_id, tag_id)
SELECT po.id, g.id
FROM seed_posting p
JOIN postings po ON po.title = p.title
CROSS JOIN unnest(string_to_array(p.tags, ',')) AS t
JOIN tags g ON g.name = trim(t);
 
INSERT INTO posting_time_slots (posting_id, day_of_week, start_minute, end_minute)
SELECT po.id,
       array_position(ARRAY['MON','TUE','WED','THU','FRI','SAT','SUN'], split_part(slot, ' ', 1)),
       split_part(split_part(split_part(slot, ' ', 2), '-', 1), ':', 1)::int * 60
         + split_part(split_part(split_part(slot, ' ', 2), '-', 1), ':', 2)::int,
       split_part(split_part(split_part(slot, ' ', 2), '-', 2), ':', 1)::int * 60
         + split_part(split_part(split_part(slot, ' ', 2), '-', 2), ':', 2)::int
FROM seed_posting p
JOIN postings po ON po.title = p.title
CROSS JOIN unnest(string_to_array(p.slots, ';')) AS slot;
 
 
-- ---------- applications ----------
-- One row per application. The status history and the notifications are derived from these rows.
--   status        current status
--   reason        'POSITION_FILLED' only when the system rejected it (posting filled)
--   rejected_from for REJECTED: the status it was rejected from (APPLIED / REVIEWED / INTERVIEW / OFFER)
--   completed_by  for COMPLETED: STUDENT or EMPLOYER
--   applied_ago / updated_ago  how long ago it was created / last changed; the steps in between are spread evenly
-- Rules kept: Accepted + Completed < headcount on OPEN postings; FILLED postings have no application in
-- progress; nobody applies before the posting exists; a system reject happens when the posting closed.
 
CREATE TEMP TABLE seed_application (
    title         text,
    email         text,
    status        text,
    reason        text,
    rejected_from text,
    completed_by  text,
    applied_ago   interval,
    updated_ago   interval
) ON COMMIT DROP;
 
INSERT INTO seed_application VALUES
-- Aoife: one application in every status
('Junior Java Developer (Internship)', 'aoife.murphy@student.tus.ie', 'APPLIED',   NULL, NULL, NULL, '1 day 2 hours',  '1 day 2 hours'),
('QA Tester',                          'aoife.murphy@student.tus.ie', 'REVIEWED',  NULL, NULL, NULL, '10 days',        '2 days'),
('Gym Receptionist',                   'aoife.murphy@student.tus.ie', 'INTERVIEW', NULL, NULL, NULL, '8 days',         '1 day'),
('Coding Club Mentor',                 'aoife.murphy@student.tus.ie', 'OFFER',     NULL, NULL, NULL, '4 days',         '5 hours'),
('Data Cleaning Assistant',            'aoife.murphy@student.tus.ie', 'OFFER',     NULL, NULL, NULL, '9 days',         '2 days 6 hours'),
('Evening Counter Assistant',          'aoife.murphy@student.tus.ie', 'ACCEPTED',  NULL, NULL, NULL, '13 days',        '4 days'),
('Open Day Helper',                    'aoife.murphy@student.tus.ie', 'COMPLETED', NULL, NULL, 'EMPLOYER', '45 days',  '30 days'),
('Lunch Rush Server',                  'aoife.murphy@student.tus.ie', 'DECLINED',  NULL, NULL, NULL, '8 days',         '3 days'),
('Hotel Breakfast Waiter',             'aoife.murphy@student.tus.ie', 'REJECTED',  NULL, 'REVIEWED', NULL, '15 days',  '12 days'),
('Summer Barista',                     'aoife.murphy@student.tus.ie', 'REJECTED',  'POSITION_FILLED', 'OFFER', NULL, '58 days', '45 days'),
-- Liam
('Weekend Barista',                    'liam.obrien@student.tus.ie',  'ACCEPTED',  NULL, NULL, NULL, '18 days',        '14 days'),
('Summer Barista',                     'liam.obrien@student.tus.ie',  'ACCEPTED',  NULL, NULL, NULL, '55 days',        '45 days'),
('Bar Staff (Evenings)',               'liam.obrien@student.tus.ie',  'OFFER',     NULL, NULL, NULL, '5 days',         '1 day'),
('Junior Java Developer (Internship)', 'liam.obrien@student.tus.ie',  'INTERVIEW', NULL, NULL, NULL, '1 day 20 hours', '6 hours'),
('Coding Club Mentor',                 'liam.obrien@student.tus.ie',  'INTERVIEW', NULL, NULL, NULL, '4 days 12 hours','1 day 12 hours'),
('Sales Assistant (Weekends)',         'liam.obrien@student.tus.ie',  'REVIEWED',  NULL, NULL, NULL, '3 days',         '2 days'),
('Leaving Cert Maths Tutor',           'liam.obrien@student.tus.ie',  'APPLIED',   NULL, NULL, NULL, '1 day',          '1 day'),
-- Wei
('Weekend Barista',                    'wei.zhang@student.tus.ie',    'COMPLETED', NULL, NULL, 'STUDENT', '19 days',   '10 days'),
('Coding Club Mentor',                 'wei.zhang@student.tus.ie',    'ACCEPTED',  NULL, NULL, NULL, '4 days 20 hours','2 days'),
('Bar Staff (Evenings)',               'wei.zhang@student.tus.ie',    'OFFER',     NULL, NULL, NULL, '5 days 6 hours', '20 hours'),
('Kitchen Porter',                     'wei.zhang@student.tus.ie',    'INTERVIEW', NULL, NULL, NULL, '30 days',        '12 days'),
('Junior Java Developer (Internship)', 'wei.zhang@student.tus.ie',    'REVIEWED',  NULL, NULL, NULL, '1 day 18 hours', '1 day'),
('Summer Barista',                     'wei.zhang@student.tus.ie',    'REJECTED',  'POSITION_FILLED', 'INTERVIEW', NULL, '50 days', '45 days'),
('Leaving Cert Maths Tutor',           'wei.zhang@student.tus.ie',    'OFFER',     NULL, NULL, NULL, '2 days 12 hours','12 hours'),
-- Bulk students in named scenarios
('Bar Staff (Evenings)',               'test.student001@student.tus.ie', 'ACCEPTED', NULL, NULL, NULL, '5 days 12 hours', '3 days'),
('Junior Java Developer (Internship)', 'test.student002@student.tus.ie', 'OFFER',    NULL, NULL, NULL, '1 day 23 hours',  '3 hours'),
('Junior Java Developer (Internship)', 'test.student003@student.tus.ie', 'DECLINED', NULL, NULL, NULL, '1 day 22 hours',  '2 hours'),
('Junior Java Developer (Internship)', 'test.student004@student.tus.ie', 'REJECTED', NULL, 'APPLIED', NULL, '1 day 21 hours', '1 day 4 hours'),
('Weekend Barista',                    'test.student005@student.tus.ie', 'OFFER',    NULL, NULL, NULL, '12 days',         '3 days');
 
-- Conor applies to every posting except Marketing Intern and Lifeguard (they keep "No applications yet").
-- On FILLED postings the system rejected him when the posting closed.
INSERT INTO seed_application
SELECT p.title, 'conor.byrne@student.tus.ie',
       CASE WHEN p.close_reason = 'FILLED' THEN 'REJECTED' ELSE 'APPLIED' END,
       CASE WHEN p.close_reason = 'FILLED' THEN 'POSITION_FILLED' END,
       CASE WHEN p.close_reason = 'FILLED' THEN 'APPLIED' END,
       NULL,
       make_interval(days => p.days_ago) - interval '12 hours',
       CASE WHEN p.close_reason = 'FILLED' THEN make_interval(days => p.closed_days_ago)
            ELSE make_interval(days => p.days_ago) - interval '12 hours' END
FROM seed_posting p
WHERE p.title NOT IN ('Marketing Intern', 'Lifeguard (Evenings)');
 
-- All 105 bulk students apply to Event Steward (posted 1 day ago): 90 Applied, 7 Reviewed,
-- 4 Interview, 4 Rejected by the employer.
INSERT INTO seed_application
SELECT 'Event Steward (Concerts)', format('test.student%s@student.tus.ie', lpad(i::text, 3, '0')),
       CASE WHEN i <= 90 THEN 'APPLIED' WHEN i <= 97 THEN 'REVIEWED' WHEN i <= 101 THEN 'INTERVIEW' ELSE 'REJECTED' END,
       NULL,
       CASE WHEN i > 101 THEN 'APPLIED' END,
       NULL,
       make_interval(mins => i * 9),
       make_interval(mins => CASE WHEN i <= 90 THEN i * 9 ELSE i * 9 - 60 END)
FROM generate_series(1, 105) AS i;
 
INSERT INTO applications (posting_id, student_id, status, status_reason, created_at, updated_at)
SELECT po.id, u.id, a.status, a.reason, now() - a.applied_ago, now() - a.updated_ago
FROM seed_application a
JOIN postings po ON po.title = a.title
JOIN users u ON u.email = a.email
ORDER BY a.applied_ago DESC;   -- ids grow with application time
 
 
-- ---------- status history ----------
-- Every application walks APPLIED -> REVIEWED -> INTERVIEW -> OFFER -> ACCEPTED -> COMPLETED up to its
-- status; DECLINED leaves after OFFER, REJECTED after rejected_from. Actors follow the state machine:
-- the student applies / accepts / declines, the employer advances or rejects, SYSTEM rejects on fill
-- (actor_user_id NULL), COMPLETED is done by completed_by.
 
INSERT INTO application_status_history (application_id, from_status, to_status, actor_type, actor_user_id,
                                        reason, created_at)
SELECT x.id, x.from_status, x.to_status, x.actor,
       CASE x.actor WHEN 'STUDENT' THEN x.student_id WHEN 'EMPLOYER' THEN x.employer_id END,
       CASE WHEN x.actor = 'SYSTEM' THEN x.reason END,
       CASE WHEN x.total = 1 THEN x.created_at
            ELSE x.created_at + (x.updated_at - x.created_at) * (x.n - 1) / (x.total - 1) END AS at
FROM (
    SELECT s.*,
           CASE WHEN s.to_status IN ('APPLIED', 'ACCEPTED', 'DECLINED') THEN 'STUDENT'
                WHEN s.to_status = 'COMPLETED' THEN s.completed_by
                WHEN s.to_status = 'REJECTED' AND s.reason IS NOT NULL THEN 'SYSTEM'
                ELSE 'EMPLOYER' END AS actor
    FROM (
        SELECT app.*, step.to_status, step.n, cardinality(app.path) AS total,
               lag(step.to_status) OVER (PARTITION BY app.id ORDER BY step.n) AS from_status
        FROM (
            SELECT ap.id, ap.student_id, po.employer_id, ap.created_at, ap.updated_at, a.reason,
                   coalesce(a.completed_by, 'STUDENT') AS completed_by,
                   CASE a.status
                       WHEN 'DECLINED' THEN ARRAY['APPLIED','REVIEWED','INTERVIEW','OFFER','DECLINED']
                       WHEN 'REJECTED' THEN (ARRAY['APPLIED','REVIEWED','INTERVIEW','OFFER'])
                                                [1:array_position(ARRAY['APPLIED','REVIEWED','INTERVIEW','OFFER'], a.rejected_from)]
                                            || 'REJECTED'::text
                       ELSE (ARRAY['APPLIED','REVIEWED','INTERVIEW','OFFER','ACCEPTED','COMPLETED'])
                                [1:array_position(ARRAY['APPLIED','REVIEWED','INTERVIEW','OFFER','ACCEPTED','COMPLETED'], a.status)]
                   END AS path
            FROM seed_application a
            JOIN postings po ON po.title = a.title
            JOIN users u ON u.email = a.email
            JOIN applications ap ON ap.posting_id = po.id AND ap.student_id = u.id
        ) app
        CROSS JOIN unnest(app.path) WITH ORDINALITY AS step(to_status, n)
    ) s
) x
ORDER BY at, x.id;
 
 
-- ---------- notifications ----------
-- One notification per history step for the other party (texts match NotificationMessages), plus
-- POSTING_FILLED to the employer of each FILLED posting. Older than 3 days = read, newer = unread.
 
INSERT INTO notifications (recipient_id, type, message, target_type, target_id, created_at)
SELECT recipient_id, type, message, target_type, target_id, created_at
FROM (
    SELECT CASE WHEN h.actor_type = 'STUDENT' THEN po.employer_id ELSE ap.student_id END AS recipient_id,
           CASE WHEN h.to_status = 'APPLIED'   THEN 'APPLICATION_RECEIVED'
                WHEN h.to_status = 'ACCEPTED'  THEN 'OFFER_ACCEPTED'
                WHEN h.to_status = 'DECLINED'  THEN 'OFFER_DECLINED'
                WHEN h.to_status = 'COMPLETED' THEN 'APPLICATION_COMPLETED'
                WHEN h.actor_type = 'SYSTEM'   THEN 'POSITION_FILLED'
                ELSE 'STATUS_CHANGED' END AS type,
           CASE WHEN h.to_status = 'APPLIED'   THEN su.name || ' applied for ' || po.title
                WHEN h.to_status = 'ACCEPTED'  THEN su.name || ' accepted the offer for ' || po.title
                WHEN h.to_status = 'DECLINED'  THEN su.name || ' declined the offer for ' || po.title
                WHEN h.to_status = 'COMPLETED' THEN po.title || ': the application was marked as completed'
                WHEN h.actor_type = 'SYSTEM'   THEN po.title || ': this position has been filled, so your application was rejected'
                ELSE po.title || ': your application moved to ' || initcap(h.to_status) END AS message,
           'APPLICATION' AS target_type,
           h.application_id AS target_id,
           h.created_at
    FROM application_status_history h
    JOIN applications ap ON ap.id = h.application_id
    JOIN postings po ON po.id = ap.posting_id
    JOIN users su ON su.id = ap.student_id
    UNION ALL
    SELECT employer_id, 'POSTING_FILLED', title || ' is filled and closed', 'POSTING', id, closed_at
    FROM postings
    WHERE close_reason = 'FILLED'
    UNION ALL
    -- Points at an application that does not exist: tests "This item is no longer available".
    SELECT u.id, 'STATUS_CHANGED', 'Christmas Market Helper: your application moved to Reviewed',
           'APPLICATION', 999999, now() - interval '2 days'
    FROM users u
    WHERE u.email = 'aoife.murphy@student.tus.ie'
) n
ORDER BY created_at;
 
UPDATE notifications SET read_at = created_at + interval '1 hour'
WHERE created_at < now() - interval '3 days';
 
COMMIT;
 