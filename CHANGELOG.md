# Changelog

## v1.4.0 (Draft)

Release focus: community content, compliance/legal flow, admin workbench, seller analytics, and chat/auth hardening.

### Added
- Community module with list, detail, publish, comment, delete, and post beautify flow.
- Admin workbench overview with pending review counters, order trends, risk reminders, and quick actions.
- Community management entry in admin routes and API client split for admin dashboard/workbench.
- Legal document pages and legal source entry points.
- Registration payload support for terms/privacy acceptance and version fields.

### Changed
- Seller overview expanded with richer dashboard data and supporting backend aggregation.
- Chat flow updated around session DTOs, unread handling, and seller/buyer conversation behavior.
- Auth interceptor, login/register flow, and axios handling adjusted to better align route protection and error responses.
- Database initializer extended to provision newer tables/columns more defensively during startup.
- Web config and application settings updated for newer route and upload handling needs.

### Fixed
- Global exception handling improved to reduce unstructured failures.
- Order/admin aggregation queries updated to support seller/admin dashboard views.
- User and chat service/controller paths aligned with the latest registration, session, and websocket behavior.

### Release Notes
- Do not include runtime upload artifacts in the release: `uploads/**`.
- Validate admin, seller, user, and anonymous flows before tagging.
- Tag target: `v1.4`.

