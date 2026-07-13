# Ruru Polish Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the remaining user-facing polish issues: avatar/name consistency, video detail autoplay, stuck transcoding labels, less awkward header branding, and cleaner tab labels.

**Architecture:** Keep this as a focused polish pass on the existing Ruru community module. First make the backend responses consistently return the latest profile avatar/display name, then update shared frontend components so every surface uses the same member profile fields. Video playback and transcode status should degrade gracefully: original MP4 can play while HLS is processing, and the UI should show a useful status instead of making all videos look permanently broken.

**Tech Stack:** Spring Boot, MyBatis Plus, MySQL/Flyway, React, TypeScript, TanStack Query, Ant Design, CSS, existing media/transcode service.

---

## File Map

- Modify: `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostResponse.java` - ensure post author fields expose latest avatar/display name.
- Modify: `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java` - map author profile data from `user_profiles` consistently.
- Modify: `backend/src/main/java/com/studyflow/community/comment/dto/PostCommentResponse.java` - ensure comments expose latest avatar/display name.
- Modify: `backend/src/main/java/com/studyflow/community/comment/PostCommentService.java` - map comment author profile data consistently.
- Modify: `backend/src/main/java/com/studyflow/community/member/CommunityMemberService.java` - verify member/profile response is the source of truth for profile card.
- Modify: `frontend/src/components/community/PostCard.tsx` - use author avatar/display name everywhere on feed cards.
- Modify: `frontend/src/pages/PostDetailPage.tsx` - use author avatar/display name, remove stale initials where avatar exists.
- Modify: `frontend/src/pages/VideoWatchPage.tsx` - autoplay video detail page and use original video fallback while transcode is not ready.
- Modify: `frontend/src/pages/CircleFeedPage.tsx` - change top-left brand treatment and remove tab counts.
- Modify: `frontend/src/index.css` - style updated brand/header, avatar consistency, and transcode fallback labels.
- Test: `backend/src/test/java/com/studyflow/community/ProfileDisplayControllerTest.java` - regression tests for profile avatar/name propagation.
- Test manually: browser feed, detail page, member profile page, video watch page.

---

### Task 1: Avatar And Display Name Consistency

**Problem:** After changing avatar/name, some places still show the old default circle letter or username. This usually means those screens use `users.username` or cached author data instead of the current `user_profiles.avatar_url/display_name`.

**Expected result:** Wherever a user appears, both self and other users see the latest saved avatar and display name.

- [ ] **Step 1: Write failing backend regression test**

Create `backend/src/test/java/com/studyflow/community/ProfileDisplayControllerTest.java`.

Test behavior:
- Register user A.
- User A updates profile with `displayName` and `avatarUrl`.
- User A creates a post and comment.
- Public feed, post detail, comment list, and member profile all return that updated display name/avatar.

Run:

```powershell
cd D:\tool\idea\project\jia\study-flow\backend
mvn -B -s docker/maven-settings.xml "-Dtest=ProfileDisplayControllerTest" test
```

Expected before fix: at least one assertion fails because a response still returns default username/letter avatar data.

- [ ] **Step 2: Fix backend author mapping**

Update the service methods that build post/comment DTOs so author display data comes from `user_profiles` first:

```java
String displayName = profile != null && profile.getDisplayName() != null
        ? profile.getDisplayName()
        : user.getUsername();
String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
```

Do not copy this logic into random places if a helper already exists. Prefer one helper per service or a small private mapper.

- [ ] **Step 3: Fix frontend rendering**

Search:

```powershell
cd D:\tool\idea\project\jia\study-flow
rg "Avatar|avatarUrl|displayName|username" frontend/src
```

Update every user display surface to follow this order:

```ts
const name = member.displayName || member.username
const avatar = member.avatarUrl || undefined
```

Surfaces to verify:
- Feed card author area.
- Post detail author area.
- Comment author area.
- Member list/profile card.
- Video watch author panel.
- Header current user menu.

- [ ] **Step 4: Invalidate profile-related caches after saving profile**

After profile save, invalidate:

```ts
queryClient.invalidateQueries({ queryKey: ['community-feed'] })
queryClient.invalidateQueries({ queryKey: ['community-profile'] })
queryClient.invalidateQueries({ queryKey: ['community-members'] })
queryClient.invalidateQueries({ queryKey: ['community-me'] })
```

Use the exact keys already present in the codebase. Do not invent a new key name if an existing one is used.

- [ ] **Step 5: Verify**

Run:

```powershell
cd D:\tool\idea\project\jia\study-flow\backend
mvn -B -s docker/maven-settings.xml "-Dtest=ProfileDisplayControllerTest" test

cd D:\tool\idea\project\jia\study-flow\frontend
npm run build
```

Manual browser check:
- Change avatar as user `haha`.
- Open own profile.
- Open another account/browser and view `haha` profile.
- Check feed card and post detail author avatar.

---

### Task 2: Video Detail Autoplay

**Problem:** Entering the video detail page requires clicking play. User wants it to start directly.

**Expected result:** Video detail page attempts autoplay immediately. Because browser policy may block autoplay with sound, default to muted autoplay if needed, then let the user unmute.

- [ ] **Step 1: Locate player**

Open:

```powershell
rg "video|autoPlay|play\\(" frontend/src/pages frontend/src/components
```

Likely target: `frontend/src/pages/VideoWatchPage.tsx`.

- [ ] **Step 2: Add controlled autoplay**

Use a `videoRef` and try playback after source is ready:

```ts
useEffect(() => {
  const video = videoRef.current
  if (!video || !videoSource) {
    return
  }

  video.muted = true
  const playPromise = video.play()
  if (playPromise) {
    playPromise.catch(() => {
      // Browser blocked autoplay; controls remain visible for manual play.
    })
  }
}, [videoSource])
```

The `<video>` should include:

```tsx
autoPlay
muted
playsInline
controls
```

- [ ] **Step 3: Verify**

Run:

```powershell
cd D:\tool\idea\project\jia\study-flow\frontend
npm run build
```

Manual browser check:
- Open a video detail page.
- Video starts without clicking the custom start button.
- If browser blocks sound autoplay, video starts muted instead of showing a dead screen.

---

### Task 3: Transcoding Status And Fallback Playback

**Problem:** All videos show `转码中`, which makes users think the video is broken. Some videos may be truly processing, but original MP4 should still be playable while HLS/1080P variants are not ready.

**Expected result:** If HLS is ready, play HLS. If HLS is not ready but original video is approved, play original MP4 and show subtle text such as `高清处理中`. If transcode failed, show `转码失败，可重试`.

- [ ] **Step 1: Investigate actual backend statuses**

On server:

```bash
cd ~/study-flow
sudo docker compose logs --tail=200 backend
sudo docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" study_flow -e "select id,file_type,status,transcode_status,hls_manifest_key,created_at from media_files order by id desc limit 20;"
```

Write down whether videos are:
- `PENDING_REVIEW`
- `APPROVED`
- `TRANSCODING`
- `TRANSCODED`
- `FAILED`

- [ ] **Step 2: Fix UI status wording**

In video cards/details:
- `TRANSCODING` + original URL available: show `高清处理中`.
- `TRANSCODED`: show quality badge only if useful.
- `FAILED`: show `转码失败`.
- No status: show nothing.

- [ ] **Step 3: Use original MP4 fallback**

In `VideoWatchPage.tsx`, choose source like this:

```ts
const hlsSource = post.media.find((media) => media.hlsUrl)?.hlsUrl
const originalSource = post.media.find((media) => media.fileType === 'VIDEO')?.url
const videoSource = hlsSource || originalSource
```

Keep this simple until we add a proper HLS player.

- [ ] **Step 4: Verify transcode worker setup**

Check Docker env:

```bash
cd ~/study-flow
sudo docker compose exec backend ffmpeg -version
sudo docker compose logs --tail=200 backend | grep -i transcode
```

If ffmpeg is missing or task queue is disabled, document it in `docs/media-transcode.md` and fix Docker/env in a separate commit.

---

### Task 4: Top-left Brand/Header Redesign

**Problem:** The top-left `R` avatar plus `ruru` text looks like a user card and feels awkward.

**Expected result:** Header reads naturally as navigation, not as a random profile avatar. Prefer a clean `主页` button or a lighter `ruru` wordmark.

- [ ] **Step 1: Pick one UI**

Recommended:

```tsx
<button className="home-wordmark" type="button" onClick={() => navigate('/circle')}>
  主页
</button>
```

Alternative:

```tsx
<button className="home-wordmark" type="button" onClick={() => navigate('/circle')}>
  ruru
</button>
```

Use the recommended `主页` if no new direction is given.

- [ ] **Step 2: Remove avatar-like square**

Modify `frontend/src/pages/CircleFeedPage.tsx` so the left header no longer renders a circular avatar/brand mark.

- [ ] **Step 3: CSS polish**

Add a subtle style:

```css
.home-wordmark {
  border: 0;
  color: var(--sf-primary);
  font-size: 18px;
  font-weight: 800;
  background: transparent;
  cursor: pointer;
}
```

---

### Task 5: Remove Channel Count Numbers

**Problem:** `直播 0 / 图文 4 / 视频 6` beside tabs is not important and makes the header noisy.

**Expected result:** Tabs only show `直播 / 图文 / 视频`.

- [ ] **Step 1: Update tab labels**

Modify `frontend/src/pages/CircleFeedPage.tsx`.

From:

```tsx
直播 <span>{liveCount}</span>
图文 <span>{articleCount}</span>
视频 <span>{videoCount}</span>
```

To:

```tsx
直播
图文
视频
```

- [ ] **Step 2: Remove unused counts if no longer needed**

If TypeScript reports unused variables after removing counts, delete those variables.

- [ ] **Step 3: Verify**

Run:

```powershell
cd D:\tool\idea\project\jia\study-flow\frontend
npm run build
```

---

## Final Verification

Run all targeted checks before committing:

```powershell
cd D:\tool\idea\project\jia\study-flow\backend
mvn -B -s docker/maven-settings.xml "-Dtest=ProfileDisplayControllerTest,BackgroundPresetControllerTest" test

cd D:\tool\idea\project\jia\study-flow\frontend
npm run build

cd D:\tool\idea\project\jia\study-flow
git diff --check
```

Manual browser checklist:
- Updated avatar appears on feed card, post detail, comments, member profile, video watch author panel.
- Another user sees the updated avatar/name.
- Video detail opens and starts playing without clicking the start button.
- Transcoding videos can still play original MP4 when available.
- Header left side no longer looks like a random avatar.
- Channel tabs show no numbers.

## Commit Plan

Use small commits:

```bash
git add backend frontend
git commit -m "fix: sync profile display across community"
git add frontend
git commit -m "fix: improve video playback and feed header"
git push
```

If backend and frontend changes are too intertwined, one commit is acceptable:

```bash
git commit -m "fix: polish community profile and video UX"
```
