# v1.4 Release Notes

## Summary

`v1.4` is centered on four areas:

- community publishing and discussion
- admin workbench and moderation visibility
- seller analytics and operational improvements
- registration/legal/chat/auth consistency fixes

## User-Facing Changes

### Community
- Added community square, post detail, publish page, and comment flow.
- Added support for text-image style post beautification.
- Added entry points in router and main layout.

### Admin
- Added admin workbench dashboard.
- Added community moderation entry.
- Added dashboard summary, trend, and risk overview APIs.

### Seller
- Expanded seller overview with stronger metrics and trend support.
- Improved seller order management compatibility with newer status handling.

### Account, Auth, and Legal
- Registration now carries agreement metadata for terms/privacy.
- Added legal document pages and current legal content endpoint.
- Improved login/register/error handling consistency.

### Chat and Messaging
- Refined chat session handling, controller/service behavior, and websocket flow.
- Improved unread/session synchronization around the store and chat page.

## Release Checklist

- Exclude runtime files from commit:
  - `uploads/**`
- Verify local startup:
  - backend: `mvn spring-boot:run`
  - frontend: `npm run serve`
- Smoke test these pages:
  - `/login`
  - `/register`
  - `/community`
  - `/community/publish`
  - `/seller/overview`
  - `/admin/workbench`
- Smoke test these APIs:
  - `GET /community/posts`
  - `GET /admin/dashboard/overview`
  - `GET /legal/current`
- Verify route guards for:
  - anonymous users
  - seller-only pages
  - admin-only pages
- Confirm database initializer still starts cleanly on existing data.

## Suggested Tagging

- Maven version: `1.4.0`
- Frontend version: `1.4.0`
- Git tag: `v1.4`

## Known Pre-Release Attention Points

- Current working tree still contains many uncommitted feature files; release should be cut only after final smoke testing.
- Community/admin features introduce several new backend and frontend entry points, so route/API coverage should be checked before push.
