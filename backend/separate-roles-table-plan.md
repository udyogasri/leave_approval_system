# Plan: Separate Roles Table (DB Normalization)

## Overview

Currently, the user's role is stored as an `@Enumerated(EnumType.STRING)` column directly on the `system_users` table. The goal is to extract roles into a separate `roles` table (`id`, `name`) and replace the inline column with a FK relationship — while keeping the behaviour identical: one user has exactly one role, roles are fixed (ADMIN, MANAGER, EMPLOYEE), and all Spring Security `@PreAuthorize` checks continue to work unchanged.

**Scope:** Entity layer, repository layer, service layer, mapper, and DB seeding. No controller changes. No DTO changes. No BPMN/Camunda changes.

**Non-goals:** Role CRUD API, multi-role users, permission tables.

---

## Sub-Tasks

---

### Sub-Task 1 — Create the `Role` JPA Entity

**Intent**  
Replace the current `Role` enum with a proper JPA `@Entity` mapped to a `roles` table. The entity keeps `id` (PK) and `name` (unique string). A separate `RoleName` enum is introduced to retain compile-time safety for the three fixed values (ADMIN, MANAGER, EMPLOYEE) across the rest of the codebase.

**Expected Outcomes**
- `src/main/java/com/app/leaveapprovalsystem/entity/RoleName.java` — new enum with `ADMIN`, `MANAGER`, `EMPLOYEE`
- `src/main/java/com/app/leaveapprovalsystem/entity/Role.java` — converted from plain enum to `@Entity` with fields `id` (Long PK) and `name` (`RoleName`, unique, not null)
- Old `Role` enum values are gone from `Role.java`

**Todo List**
1. Create `RoleName.java` enum in the `entity` package with values `ADMIN`, `MANAGER`, `EMPLOYEE`
2. Rewrite `Role.java` as a `@Entity @Table(name = "roles")` with:
   - `@Id @GeneratedValue Long id`
   - `@Enumerated(EnumType.STRING) @Column(unique=true, nullable=false) RoleName name`
   - No-args constructor, all-args constructor, getters (Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`)

**Relevant Context**
- Current `Role.java`: `src/main/java/com/app/leaveapprovalsystem/entity/Role.java`
- All files that import `Role` will need updating in later sub-tasks

**Status:** [ ] pending

---

### Sub-Task 2 — Update `User` Entity to Reference `Role` via FK

**Intent**  
Change the `role` field in `User` from `@Enumerated(EnumType.STRING) private Role role` (inline enum column) to a `@ManyToOne` FK pointing to the `roles` table. One user → one role row.

**Expected Outcomes**
- `User.java` `role` field is now `@ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "role_id", nullable = false) private Role role`
- `getAuthorities()` updated to use `role.getName().name()` instead of `role.name()`
- No other behaviour changes — Spring Security authorities still emit `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`

**Todo List**
1. Replace the `@Enumerated` + `@Column` annotation block on `role` with `@ManyToOne(fetch = FetchType.EAGER)` + `@JoinColumn(name = "role_id", nullable = false)`
2. Change field type from `Role` (old enum) to `Role` (new entity) — no field rename needed
3. Update `getAuthorities()`: change `role.name()` → `role.getName().name()`

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/entity/User.java` lines 42-44, 87-89
- FetchType must be EAGER so Spring Security can read the role without an open session

**Status:** [ ] pending

---

### Sub-Task 3 — Add `RoleRepository` and DB Seeding

**Intent**  
Provide a Spring Data repository for the `Role` entity and seed the three fixed roles (`ADMIN`, `MANAGER`, `EMPLOYEE`) into the `roles` table on application startup so they are always present before any user is registered.

**Expected Outcomes**
- `src/main/java/com/app/leaveapprovalsystem/repository/RoleRepository.java` — new repository with `findByName(RoleName name)` method
- `src/main/java/com/app/leaveapprovalsystem/config/DataInitializer.java` — new `@Component` implementing `ApplicationRunner` that inserts the 3 roles if they don't already exist

**Todo List**
1. Create `RoleRepository.java` extending `JpaRepository<Role, Long>` with method `Optional<Role> findByName(RoleName name)`
2. Create `DataInitializer.java` as `@Component` with `@RequiredArgsConstructor`, inject `RoleRepository`, implement `ApplicationRunner.run()`:
   - For each `RoleName` value, call `roleRepository.findByName(name)` — if absent, save a new `Role` entity
   - Use `@Transactional`

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/repository/UserRepository.java` — follow same package and style
- `src/main/java/com/app/leaveapprovalsystem/config/SecurityConfig.java` — same config package for `DataInitializer`

**Status:** [ ] pending

---

### Sub-Task 4 — Update `UserRepository` Role Query Methods

**Intent**  
The two role-based query methods in `UserRepository` currently accept `Role` (old enum). They must now accept `RoleName` and query via the joined `roles` table.

**Expected Outcomes**
- `existsByRole(Role role)` → `existsByRole_Name(RoleName name)`
- `findByRole(Role role, Pageable pageable)` → `findByRole_Name(RoleName name, Pageable pageable)`

**Todo List**
1. Replace `existsByRole(Role role)` with `existsByRole_Name(RoleName name)`
2. Replace `findByRole(Role role, Pageable pageable)` with `findByRole_Name(RoleName name, Pageable pageable)`
3. Update the import — remove old `Role` enum import, add `RoleName` import

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/repository/UserRepository.java` lines 17, 19
- Spring Data JPA derived query for nested field: `findByRole_Name` traverses `user.role.name`

**Status:** [ ] pending

---

### Sub-Task 5 — Update `AuthService` to Look Up Role Entity

**Intent**  
`AuthService.register()` currently does `Role role = parseRole(dto.getRole())` where `parseRole` returns a `Role` enum value. It must now look up the `Role` entity from the database using `RoleRepository.findByName(RoleName)`. All `role == Role.ADMIN` comparisons must change to `role.getName() == RoleName.ADMIN`.

**Expected Outcomes**
- `parseRole()` private method removed; replaced with `findRole(String roleStr)` that resolves a `Role` entity from DB
- All `role == Role.ADMIN/MANAGER/EMPLOYEE` comparisons updated to `role.getName() == RoleName.ADMIN/MANAGER/EMPLOYEE`
- `userRepository.existsByRole(Role.ADMIN)` → `userRepository.existsByRole_Name(RoleName.ADMIN)`
- `user.getRole().name()` in `LoginResponseDTO` builder → `user.getRole().getName().name()`

**Todo List**
1. Inject `RoleRepository` into `AuthService`
2. Replace `parseRole(String)` with `findRole(String)` that does `RoleName rn = RoleName.valueOf(roleStr.toUpperCase())` then `roleRepository.findByName(rn).orElseThrow(...)`
3. Update all 4 `role == Role.X` comparisons to `role.getName() == RoleName.X`
4. Update `userRepository.existsByRole(Role.ADMIN)` → `userRepository.existsByRole_Name(RoleName.ADMIN)`
5. Update `user.getRole().name()` → `user.getRole().getName().name()` in the login response builder
6. Fix imports: add `RoleName`, `RoleRepository`; remove old `Role` enum import (now `Role` is entity)

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/service/AuthService.java` lines 41-91, 110-115, 125, 132-138
- `requireAdminToken()` at line 122 checks `"ROLE_ADMIN"` string — no change needed there

**Status:** [ ] pending

---

### Sub-Task 6 — Update `UserService` and `UserController`

**Intent**  
`UserService.getUsersByRole()` accepts `Role role` parameter and calls `userRepository.findByRole()`. `UserController` parses the path variable string to `Role.valueOf()`. Both must switch to `RoleName`.

**Expected Outcomes**
- `UserService.getUsersByRole(Role role)` → `getUsersByRole(RoleName roleName)`
- Internal call updated to `userRepository.findByRole_Name(roleName, pageable)`
- `UserController` path variable parsing: `Role.valueOf(role.toUpperCase())` → `RoleName.valueOf(role.toUpperCase())`
- Controller now passes `RoleName` to service

**Todo List**
1. In `UserService`: change method signature and internal repository call; update import
2. In `UserController`: change `Role.valueOf(...)` → `RoleName.valueOf(...)` and update the `getUsersByRole(...)` call argument; update import

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/service/UserService.java` lines 30-31
- `src/main/java/com/app/leaveapprovalsystem/controller/UserController.java` line 48

**Status:** [ ] pending

---

### Sub-Task 7 — Update `UserMapper` and `UserResponseDTO`

**Intent**  
`UserMapper` converts `user.getRole().name()` to a String for `UserResponseDTO`. With the new entity, this must change to `user.getRole().getName().name()`.

**Expected Outcomes**
- `UserMapper.java` mapping expression updated from `user.getRole().name()` to `user.getRole().getName().name()`
- `UserResponseDTO.role` field remains `String` — no change needed
- `LoginResponseDTO.role` remains `String` — the change in AuthService (Sub-Task 5) already covers it

**Todo List**
1. In `UserMapper.java`: update the `@Mapping` expression from `java(user.getRole().name())` to `java(user.getRole().getName().name())`

**Relevant Context**
- `src/main/java/com/app/leaveapprovalsystem/mapper/UserMapper.java` line 11

**Status:** [ ] pending

---

### Sub-Task 8 — Update `application.yaml` and Verify Hibernate DDL

**Intent**  
Confirm Hibernate `ddl-auto: update` will correctly create the new `roles` table and add the `role_id` FK column to `system_users`. No manual SQL migration is needed. Optionally add a note in the README.

**Expected Outcomes**
- `application.yaml` remains unchanged (`ddl-auto: update` handles schema evolution)
- On startup: Hibernate creates `roles(id, name)`, adds `role_id` FK column on `system_users`, `DataInitializer` seeds the 3 rows
- Old `role` VARCHAR column on `system_users` is no longer managed by Hibernate (left as dead column — `ddl-auto: update` does not drop columns)
- README updated to reflect new DB schema

**Todo List**
1. Confirm `ddl-auto: update` is set — no change needed
2. Note: the old `role` column on `system_users` will persist in the DB as an orphaned column — this is safe and expected with `update` strategy
3. Update the Database Schema section of `README.md` to show `role_id FK → roles` instead of `role ENUM`

**Relevant Context**
- `src/main/resources/application.yaml` line 10
- `README.md` lines 74-88

**Status:** [ ] pending

---

## File Change Summary

| File | Action |
|---|---|
| `entity/Role.java` | Rewrite — enum → JPA entity |
| `entity/RoleName.java` | Create — new enum (ADMIN, MANAGER, EMPLOYEE) |
| `entity/User.java` | Modify — inline enum column → @ManyToOne FK |
| `repository/RoleRepository.java` | Create — findByName query |
| `repository/UserRepository.java` | Modify — query methods use RoleName |
| `config/DataInitializer.java` | Create — seeds 3 roles on startup |
| `service/AuthService.java` | Modify — role lookup via RoleRepository |
| `service/UserService.java` | Modify — parameter type RoleName |
| `controller/UserController.java` | Modify — parse to RoleName |
| `mapper/UserMapper.java` | Modify — getName().name() |
| `README.md` | Modify — DB schema section |

**No changes needed:**  
`LeaveService`, `LeaveController`, `AuthController`, all DTOs (except through service changes), `SecurityConfig`, `JwtUtil`, `CustomUserDetailsService`, BPMN files.
