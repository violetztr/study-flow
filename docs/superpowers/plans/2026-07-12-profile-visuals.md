# Profile Visuals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add editable avatars, personal space backgrounds, system preset personal backgrounds, and a site homepage video background.

**Architecture:** Keep media files out of the backend server. User-uploaded avatars and custom background images use the existing R2 media upload flow, while system preset backgrounds live in the frontend `public` directory with stable URLs. User profile metadata stores only URLs and lightweight type hints. User-uploaded media is saved as stable `/api/media/files/{id}` URLs so expiring R2 signatures are generated on demand.

**Tech Stack:** Spring Boot, Flyway, MyBatis-Plus, React, TypeScript, Ant Design, Cloudflare R2.

---

### Task 1: Static Visual Assets

**Files:**
- Create: `frontend/public/system-backgrounds/profile/road.png`
- Create: `frontend/public/system-backgrounds/profile/silhouette.mp4`
- Create: `frontend/public/system-backgrounds/profile/cabin.mp4`
- Create: `frontend/public/system-backgrounds/site/home-hero.mp4`

- [x] Copy the local files into stable ASCII filenames.
- [x] Use these public URLs in frontend code:
  - `/system-backgrounds/profile/road.png`
  - `/system-backgrounds/profile/silhouette.mp4`
  - `/system-backgrounds/profile/cabin.mp4`
  - `/system-backgrounds/site/home-hero.mp4`

### Task 2: Backend Profile Background Metadata

**Files:**
- Create: `backend/src/main/resources/db/migration/V19__add_profile_background_fields.sql`
- Modify: `backend/src/main/java/com/studyflow/community/member/UserProfile.java`
- Modify: `backend/src/main/java/com/studyflow/community/member/dto/UserProfileRequest.java`
- Modify: `backend/src/main/java/com/studyflow/community/member/dto/CommunityMemberResponse.java`
- Modify: `backend/src/main/java/com/studyflow/community/member/CommunityMemberService.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityFoundationControllerTest.java`

- [x] Write a failing test proving `/api/community/members/me/profile` saves and returns `avatarUrl`, `profileBackgroundUrl`, and `profileBackgroundType`.
- [x] Run that test and confirm it fails because the new fields do not exist.
- [x] Add Flyway fields:

```sql
ALTER TABLE user_profiles
    ADD COLUMN profile_background_url VARCHAR(500);

ALTER TABLE user_profiles
    ADD COLUMN profile_background_type VARCHAR(20) DEFAULT 'IMAGE';
```

- [x] Add Java fields and DTO record fields.
- [x] Update profile save logic to persist these fields.
- [x] Run the targeted backend test and confirm it passes.

### Task 2.5: Stable Media File URLs

**Files:**
- Modify: `backend/src/main/java/com/studyflow/media/MediaController.java`
- Modify: `backend/src/main/java/com/studyflow/media/MediaService.java`
- Modify: `backend/src/main/java/com/studyflow/media/dto/MediaUploadCompleteResponse.java`
- Modify: `backend/src/main/java/com/studyflow/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/studyflow/media/MediaControllerTest.java`

- [x] Add a failing test that upload completion returns `/api/media/files/{id}`.
- [x] Add a failing test that `/api/media/files/{id}` redirects to a short-lived R2 signed URL.
- [x] Return stable media URLs from upload completion.
- [x] Add public media redirect for visible media only.
- [x] Permit public reads for `/api/media/files/*`.

### Task 3: Frontend Profile Editing UI

**Files:**
- Modify: `frontend/src/api/community.ts`
- Modify: `frontend/src/api/media.ts`
- Modify: `frontend/src/pages/MemberProfilePage.tsx`
- Modify: `frontend/src/index.css`

- [x] Add `profileBackgroundUrl` and `profileBackgroundType` to frontend profile types.
- [x] Add an edit panel only when viewing your own profile.
- [x] Avatar upload accepts images only and saves the stable media URL to `avatarUrl`.
- [x] Custom background upload accepts images only and saves the stable media URL plus `IMAGE`.
- [x] Preset background buttons save one of the `/system-backgrounds/profile/...` URLs.
- [x] Render image backgrounds as CSS background image and video backgrounds as muted loop video.
- [x] Keep existing follow/profile tabs behavior unchanged.
- [x] Run frontend build and lint.

### Task 4: Site Homepage Background

**Files:**
- Modify: `frontend/src/pages/CircleFeedPage.tsx`
- Modify: `frontend/src/index.css`

- [x] Render `/system-backgrounds/site/home-hero.mp4` as a muted loop background behind the feed shell.
- [x] Keep cards readable with a translucent overlay.
- [x] Ensure mobile layout still displays content first.
- [x] Run frontend build and lint.

### Task 5: Verification and Git

**Files:**
- All changed files.

- [x] Run targeted backend test.
- [x] Run full backend `mvn test`.
- [x] Run frontend `npm run build`.
- [x] Run frontend `npm run lint`.
- [x] Run `git diff --check`.
- [ ] Commit with `feat: add profile visual customization`.
- [ ] Push to GitHub.
