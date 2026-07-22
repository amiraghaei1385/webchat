// استوری (متصل به بک‌اند واقعی)

const StoriesFeature = (() => {

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#00e6d8,#6a5cff)',
    'linear-gradient(135deg,#ff4d8d,#6a5cff)',
    'linear-gradient(135deg,#ffb020,#ff4d8d)',
    'linear-gradient(135deg,#00e6a8,#00e6d8)',
    'linear-gradient(135deg,#6a5cff,#00e6d8)',
  ];

  const STORY_DURATION_MS = 5000;

  let myUserId = null;
  let myOwnStories = [];      // استوری‌های فعال خودِ کاربر (برای حلقه‌ی "استوری من")
  let currentGroupStories = []; // استوری‌های کاربری که در حال حاضر مشاهده می‌شود
  let currentIndex = 0;
  let autoAdvanceTimer = null;
  let selectedMediaType = 'TEXT'; // TEXT | IMAGE | VIDEO
  let selectedFile = null;

  function initials(name) { return (name || '?').trim().charAt(0).toUpperCase(); }

  function getMyUserId() {
    if (!myUserId) myUserId = (typeof StorageManager !== 'undefined') ? StorageManager.getUserId() : null;
    return myUserId;
  }

  // فید استوری از /api/stories/feed می‌آید و بر اساس مالک گروه‌بندی شده
  async function loadStories() {
    try {
      const groups = await Api.Stories.getFeed();
      return Promise.all(groups.map(async g => {
        const profile = await Api.Directory.getUserProfile(g.ownerId);
        return {
          userId: g.ownerId,
          name: profile ? profile.username : g.ownerId,
          hasUnseen: !!g.hasUnseen,
          avatarColor: profile && profile.avatarColor ? profile.avatarColor : 0,
        };
      }));
    } catch (_) {
      return [];
    }
  }

  function buildMyStoryItemHtml(hasStories) {
    const ringClass = hasStories ? 'all-seen' : '';
    return `
      <div class="story-item" id="myStoryItem">
        <div class="story-avatar-ring-wrap">
          <div class="story-avatar-ring ${ringClass}">
            <div class="avatar" style="background:var(--surface-glass-hi);">👤</div>
          </div>
          <div class="story-add-badge" id="myStoryAddBadge">+</div>
        </div>
        <div class="story-item-label" data-i18n="stories.yourStory">Your story</div>
      </div>
    `;
  }

  function buildStoryItemHtml(story, index) {
    const ringClass = story.hasUnseen ? 'has-unseen' : 'all-seen';
    const bg = AVATAR_COLORS[(story.avatarColor ?? index) % AVATAR_COLORS.length];
    return `
      <div class="story-item" data-user-id="${story.userId}">
        <div class="story-avatar-ring ${ringClass}">
          <div class="avatar" style="background:${bg};">${initials(story.name)}</div>
        </div>
        <div class="story-item-label">${Utils.escapeHtml(story.name)}</div>
      </div>
    `;
  }

  async function render(containerEl) {
    const uid = getMyUserId();
    const [stories, mine] = await Promise.all([
      loadStories(),
      uid ? Api.Stories.getUserStories(uid).catch(() => []) : Promise.resolve([]),
    ]);
    myOwnStories = mine || [];

    containerEl.innerHTML = buildMyStoryItemHtml(myOwnStories.length > 0) + stories.map(buildStoryItemHtml).join('');

    document.getElementById('myStoryItem').addEventListener('click', () => {
      if (myOwnStories.length > 0) {
        openViewer(myOwnStories, uid, true);
      } else {
        openAddStory();
      }
    });
    // نگه‌داشتن دکمه‌ی + برای افزودن حتی وقتی کاربر استوری فعال دارد
    const addBadge = document.getElementById('myStoryAddBadge');
    if (addBadge) {
      addBadge.addEventListener('click', (e) => {
        e.stopPropagation();
        openAddStory();
      });
    }

    containerEl.querySelectorAll('.story-item[data-user-id]').forEach(el => {
      el.addEventListener('click', () => openStory(el.dataset.userId));
    });
  }

  // -----------------------------------------------------------
  // ساخت استوری
  // -----------------------------------------------------------

  function getStoryModalEls() {
    return {
      backdrop: document.getElementById('storyModalBackdrop'),
      modal: document.getElementById('addStoryModal'),
      textTab: document.getElementById('storyTypeTextBtn'),
      imageTab: document.getElementById('storyTypeImageBtn'),
      videoTab: document.getElementById('storyTypeVideoBtn'),
      textInput: document.getElementById('storyTextInput'),
      mediaArea: document.getElementById('storyMediaArea'),
      mediaInput: document.getElementById('storyMediaInput'),
      mediaDrop: document.getElementById('storyMediaDrop'),
      previewImg: document.getElementById('storyMediaPreviewImg'),
      previewVideo: document.getElementById('storyMediaPreviewVideo'),
      captionInput: document.getElementById('storyCaptionInput'),
      cancelBtn: document.getElementById('cancelAddStoryBtn'),
      publishBtn: document.getElementById('publishStoryBtn'),
    };
  }

  function resetStoryModal() {
    const els = getStoryModalEls();
    selectedMediaType = 'TEXT';
    selectedFile = null;
    els.textInput.value = '';
    els.captionInput.value = '';
    els.mediaInput.value = '';
    els.previewImg.style.display = 'none';
    els.previewImg.src = '';
    els.previewVideo.style.display = 'none';
    els.previewVideo.src = '';
    setActiveTab('TEXT');
  }

  function setActiveTab(type) {
    const els = getStoryModalEls();
    selectedMediaType = type;
    els.textTab.classList.toggle('active', type === 'TEXT');
    els.imageTab.classList.toggle('active', type === 'IMAGE');
    els.videoTab.classList.toggle('active', type === 'VIDEO');
    els.textInput.style.display = type === 'TEXT' ? 'block' : 'none';
    els.mediaArea.style.display = type === 'TEXT' ? 'none' : 'block';
    els.mediaInput.accept = type === 'VIDEO' ? 'video/*' : 'image/*';
  }

  function openAddStory() {
    resetStoryModal();
    const els = getStoryModalEls();
    els.backdrop.classList.add('open');
    els.modal.classList.add('open');
  }

  function closeAddStoryModal() {
    const els = getStoryModalEls();
    els.backdrop.classList.remove('open');
    els.modal.classList.remove('open');
  }

  function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result || '';
        const commaIdx = result.indexOf(',');
        resolve(commaIdx === -1 ? result : result.slice(commaIdx + 1));
      };
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  function wireAddStoryModalOnce() {
    const els = getStoryModalEls();
    if (els.modal.dataset.wired === '1') return;
    els.modal.dataset.wired = '1';

    els.textTab.addEventListener('click', () => setActiveTab('TEXT'));
    els.imageTab.addEventListener('click', () => setActiveTab('IMAGE'));
    els.videoTab.addEventListener('click', () => setActiveTab('VIDEO'));

    els.cancelBtn.addEventListener('click', closeAddStoryModal);
    els.backdrop.addEventListener('click', closeAddStoryModal);

    els.mediaDrop.addEventListener('click', () => els.mediaInput.click());
    els.mediaInput.addEventListener('change', () => {
      const file = els.mediaInput.files && els.mediaInput.files[0];
      if (!file) return;
      selectedFile = file;
      const url = URL.createObjectURL(file);
      if (selectedMediaType === 'VIDEO') {
        els.previewVideo.src = url;
        els.previewVideo.style.display = 'block';
        els.previewImg.style.display = 'none';
      } else {
        els.previewImg.src = url;
        els.previewImg.style.display = 'block';
        els.previewVideo.style.display = 'none';
      }
    });

    els.publishBtn.addEventListener('click', async () => {
      try {
        if (selectedMediaType === 'TEXT') {
          const text = els.textInput.value.trim();
          if (!text) {
            Toast.show(I18N.t('stories.emptyTextError') || 'متن استوری نمی‌تواند خالی باشد', 'error');
            return;
          }
          await Api.Stories.createText(text);
        } else {
          if (!selectedFile) {
            Toast.show(I18N.t('stories.noFileError') || 'لطفاً یک فایل انتخاب کنید', 'error');
            return;
          }
          const fileBase64 = await readFileAsBase64(selectedFile);
          await Api.Stories.createMedia({
            fileBase64,
            originalFileName: selectedFile.name,
            mimeType: selectedFile.type || 'application/octet-stream',
            mediaType: selectedMediaType,
            caption: els.captionInput.value.trim(),
          });
        }
        Toast.show(I18N.t('stories.published') || 'استوری منتشر شد');
        closeAddStoryModal();
        const container = document.getElementById('storiesRow');
        if (container) render(container);
      } catch (err) {
        Toast.show(err.message || 'خطا در انتشار استوری', 'error');
      }
    });
  }

  // -----------------------------------------------------------
  // مشاهده‌ی استوری
  // -----------------------------------------------------------

  async function openStory(userId) {
    let stories = [];
    try {
      stories = await Api.Stories.getUserStories(userId);
    } catch (err) {
      Toast.show(err.message || 'خطا در دریافت استوری', 'error');
      return;
    }
    if (!stories.length) {
      Toast.show(I18N.t('stories.noActiveStories') || 'استوری فعالی وجود ندارد');
      return;
    }
    openViewer(stories, userId, userId === getMyUserId());
  }

  function getViewerEls() {
    return {
      viewer: document.getElementById('storyViewer'),
      progress: document.getElementById('storyViewerProgress'),
      avatar: document.getElementById('storyViewerAvatar'),
      name: document.getElementById('storyViewerName'),
      time: document.getElementById('storyViewerTime'),
      body: document.getElementById('storyViewerBody'),
      closeBtn: document.getElementById('storyViewerCloseBtn'),
      deleteBtn: document.getElementById('storyViewerDeleteBtn'),
      prevZone: document.getElementById('storyViewerPrevZone'),
      nextZone: document.getElementById('storyViewerNextZone'),
    };
  }

  async function openViewer(stories, ownerId, isMine) {
    currentGroupStories = stories;
    currentIndex = 0;

    const profile = await Api.Directory.getUserProfile(ownerId);
    const els = getViewerEls();
    els.avatar.style.background = AVATAR_COLORS[((profile && profile.avatarColor) || 0) % AVATAR_COLORS.length];
    els.avatar.textContent = initials(profile ? profile.username : ownerId);
    els.name.textContent = isMine ? (I18N.t('stories.yourStory') || 'استوری من') : (profile ? profile.username : ownerId);
    els.deleteBtn.style.display = isMine ? 'inline-block' : 'none';

    buildProgressSegments(stories.length);
    els.viewer.style.display = 'flex';

    wireViewerOnce();
    await showStoryAt(0);
  }

  function buildProgressSegments(count) {
    const progressEl = document.getElementById('storyViewerProgress');
    progressEl.innerHTML = Array.from({ length: count }).map(() =>
      `<div class="story-viewer-progress-seg"><div class="fill"></div></div>`
    ).join('');
  }

  async function showStoryAt(index) {
    clearTimeout(autoAdvanceTimer);
    if (index < 0) { closeViewer(); return; }
    if (index >= currentGroupStories.length) { closeViewer(); return; }
    currentIndex = index;

    const story = currentGroupStories[index];
    const els = getViewerEls();

    // پیشرفت نوار بالا
    const segs = els.progress.querySelectorAll('.story-viewer-progress-seg');
    segs.forEach((seg, i) => {
      seg.classList.toggle('done', i < index);
      const fill = seg.querySelector('.fill');
      fill.style.transition = 'none';
      fill.style.width = i < index ? '100%' : '0%';
    });

    els.time.textContent = Utils.formatRelativeTime ? Utils.formatRelativeTime(story.createdAt) : '';

    // ثبت بازدید (برای استوری‌های دیگران)
    try { await Api.Stories.markAsViewed(story.id); } catch (_) { /* غیرحیاتی */ }

    // محتوا
    els.body.innerHTML = '';
    if (story.mediaType === 'TEXT') {
      const div = document.createElement('div');
      div.className = 'story-viewer-text-slide';
      div.textContent = story.caption || '';
      els.body.appendChild(div);
      startAutoAdvance();
    } else {
      els.body.innerHTML = `<div style="color:#fff;">${I18N.t('common.loading') || '...'}</div>`;
      try {
        const media = await Api.Stories.downloadMedia(story.id);
        const dataUrl = `data:${storyGuessMime(story)};base64,${media.fileBase64}`;
        els.body.innerHTML = '';
        if (story.mediaType === 'VIDEO') {
          const video = document.createElement('video');
          video.src = dataUrl;
          video.autoplay = true;
          video.controls = true;
          video.addEventListener('ended', () => showStoryAt(currentIndex + 1));
          els.body.appendChild(video);
        } else {
          const img = document.createElement('img');
          img.src = dataUrl;
          els.body.appendChild(img);
          startAutoAdvance();
        }
        if (story.caption) {
          const cap = document.createElement('div');
          cap.className = 'story-viewer-caption';
          cap.textContent = story.caption;
          els.body.appendChild(cap);
        }
      } catch (err) {
        els.body.innerHTML = `<div style="color:#fff;">${err.message || 'خطا در بارگذاری رسانه'}</div>`;
        startAutoAdvance();
      }
    }
  }

  function storyGuessMime(story) {
    return story.mediaType === 'VIDEO' ? 'video/mp4' : 'image/jpeg';
  }

  function startAutoAdvance() {
    clearTimeout(autoAdvanceTimer);
    const segs = document.querySelectorAll('#storyViewerProgress .story-viewer-progress-seg');
    const activeFill = segs[currentIndex] ? segs[currentIndex].querySelector('.fill') : null;
    if (activeFill) {
      // اجبار به ری‌فلو تا انیمیشن از صفر شروع شود
      void activeFill.offsetWidth;
      activeFill.style.transition = `width ${STORY_DURATION_MS}ms linear`;
      activeFill.style.width = '100%';
    }
    autoAdvanceTimer = setTimeout(() => showStoryAt(currentIndex + 1), STORY_DURATION_MS);
  }

  function closeViewer() {
    clearTimeout(autoAdvanceTimer);
    const els = getViewerEls();
    els.viewer.style.display = 'none';
    els.body.innerHTML = '';
  }

  function wireViewerOnce() {
    const els = getViewerEls();
    if (els.viewer.dataset.wired === '1') return;
    els.viewer.dataset.wired = '1';

    els.closeBtn.addEventListener('click', closeViewer);
    els.prevZone.addEventListener('click', () => showStoryAt(currentIndex - 1));
    els.nextZone.addEventListener('click', () => showStoryAt(currentIndex + 1));

    els.deleteBtn.addEventListener('click', async () => {
      const story = currentGroupStories[currentIndex];
      if (!story) return;
      try {
        await Api.Stories.delete(story.id);
        Toast.show(I18N.t('stories.deleted') || 'استوری حذف شد');
        currentGroupStories.splice(currentIndex, 1);
        if (!currentGroupStories.length) {
          closeViewer();
        } else {
          buildProgressSegments(currentGroupStories.length);
          await showStoryAt(Math.min(currentIndex, currentGroupStories.length - 1));
        }
        const container = document.getElementById('storiesRow');
        if (container) render(container);
      } catch (err) {
        Toast.show(err.message || 'خطا در حذف استوری', 'error');
      }
    });
  }

  // اتصال مودال ساخت استوری در اولین رندر
  document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('addStoryModal')) wireAddStoryModalOnce();
  });

  return { render };
})();
