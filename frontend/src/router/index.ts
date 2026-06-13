import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import AdminView from '../views/AdminView.vue'
import EditPostView from '../views/EditPostView.vue'
import CategoryNavView from '../views/CategoryNavView.vue'
import KeywordCloudView from '../views/KeywordCloudView.vue'
import AdminCategoryView from '../views/AdminCategoryView.vue'
import AdminKeywordView from '../views/AdminKeywordView.vue'
import AdminAuthorView from '../views/AdminAuthorView.vue'
import AdminReviewView from '../views/AdminReviewView.vue'
import AdminStatsView from '../views/AdminStatsView.vue'
import AdminScheduledView from '../views/AdminScheduledView.vue'
import AuthorDashboardView from '../views/AuthorDashboardView.vue'
import AuthorMyPostsView from '../views/AuthorMyPostsView.vue'
import AuthorCreatePostView from '../views/AuthorCreatePostView.vue'
import AuthorEditPostView from '../views/AuthorEditPostView.vue'
import AuthorStatsView from '../views/AuthorStatsView.vue'
import AuthorSettingsView from '../views/AuthorSettingsView.vue'
import AuthorProfileView from '../views/AuthorProfileView.vue'
import AuthorImageView from '../views/AuthorImageView.vue'
import AuthorReviewFeedbackView from '../views/AuthorReviewFeedbackView.vue'
import ReaderProfileView from '../views/ReaderProfileView.vue'
import ReaderSpaceView from '../views/ReaderSpaceView.vue'
import CollectionView from '../views/CollectionView.vue'
import ReadHistoryView from '../views/ReadHistoryView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/posts/:id', name: 'post-detail', component: PostDetailView },
    { path: '/categories', name: 'categories', component: CategoryNavView },
    { path: '/keywords', name: 'keywords', component: KeywordCloudView },
    { path: '/authors/:id', name: 'author-profile', component: AuthorProfileView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/forgot-password', name: 'forgot-password', component: ForgotPasswordView },
    { path: '/reader/:id', name: 'reader-space', component: ReaderSpaceView },
    // Admin routes
    { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/categories', name: 'admin-categories', component: AdminCategoryView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/keywords', name: 'admin-keywords', component: AdminKeywordView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/authors', name: 'admin-authors', component: AdminAuthorView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/reviews', name: 'admin-reviews', component: AdminReviewView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/scheduled', name: 'admin-scheduled', component: AdminScheduledView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/stats', name: 'admin-stats', component: AdminStatsView, meta: { requiresAuth: true, role: 'ADMIN' } },
    { path: '/admin/posts/:id/edit', name: 'post-edit', component: EditPostView, meta: { requiresAuth: true, role: 'ADMIN' } },
    // Author routes
    { path: '/author', name: 'author-dashboard', component: AuthorDashboardView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/posts', name: 'author-posts', component: AuthorMyPostsView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/posts/create', name: 'author-create-post', component: AuthorCreatePostView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/posts/:id/edit', name: 'author-edit-post', component: AuthorEditPostView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/images', name: 'author-images', component: AuthorImageView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/posts/:id/review-feedback', name: 'author-review-feedback', component: AuthorReviewFeedbackView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/stats', name: 'author-stats', component: AuthorStatsView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    { path: '/author/settings', name: 'author-settings', component: AuthorSettingsView, meta: { requiresAuth: true, role: 'AUTHOR' } },
    // Reader routes
    { path: '/reader', name: 'reader-profile', component: ReaderProfileView, meta: { requiresAuth: true, role: 'READER' } },
    { path: '/reader/collections', name: 'reader-collections', component: CollectionView, meta: { requiresAuth: true, role: 'READER' } },
    { path: '/reader/history', name: 'reader-history', component: ReadHistoryView, meta: { requiresAuth: true, role: 'READER' } },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  await authStore.ensureLoaded()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.role && authStore.userRole !== to.meta.role) {
    return authStore.isAdmin ? { name: 'admin' } : authStore.isAuthor ? { name: 'author-dashboard' } : authStore.isReader ? { name: 'reader-profile' } : { name: 'login' }
  }

  if (to.name === 'login' && authStore.isLoggedIn) {
    return authStore.isAdmin ? { name: 'admin' } : authStore.isAuthor ? { name: 'author-dashboard' } : authStore.isReader ? { name: 'reader-profile' } : { name: 'home' }
  }

  return true
})

export default router
