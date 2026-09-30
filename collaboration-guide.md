# WorkMates Repository Collaboration Guide

## 1. Shared Branches

The repository has three shared branches:

| Branch | Purpose |
| --- | --- |
| `dev` | Development branch. All feature work starts here and is merged back here when done. |
| `test` | Testing branch. Used for integration testing. It only accepts code from `dev`. |
| `main` | Main branch. Holds the stable version. It only accepts code from `test`. |

Rules:

- Do not push directly to any of the three shared branches.
- Do not merge the shared branches into each other locally. Merge them only through Pull Requests (PRs).
- Code must flow in the order `dev -> test -> main`. Do not skip `test` by merging `dev` straight into `main`.

## 2. Feature Branches

When a team member develops a feature, they must create a new feature branch from `dev`, used only for that piece of work.

### 2.1 Naming Rules

Format:

```
<side>/<member>/<feature>
```

- `<side>`: `fe` for frontend, `be` for backend.
- `<member>`: the member's name in lowercase pinyin, e.g. `zhou`.
- `<feature>`: lowercase English words describing the feature, joined with `-`.

Examples:

```
be/zhou/user-register
fe/zhou/job-list-page
be/li/application-status
```

Do not use Chinese characters, spaces, or uppercase letters in branch names.

### 2.2 Creating a Feature Branch

```bash
git checkout dev
git pull origin dev
git checkout -b be/zhou/user-register
git push -u origin be/zhou/user-register
```

### 2.3 Syncing with dev During Development

When others have merged new code into `dev`, sync it into your feature branch before you continue:

```bash
git checkout dev
git pull origin dev
git checkout be/zhou/user-register
git merge dev
```

If there are conflicts, resolve them on your feature branch, not on `dev`.

## 3. Pull Request Workflow

### 3.1 Feature Branch -> dev

1. When the feature is done, sync the latest `dev` first (see 2.3) and make sure everything runs locally.
2. Push the feature branch to the remote.
3. Open a PR on GitHub with `dev` as the base and your feature branch as the compare branch, then merge it.
4. After merging, you can delete the feature branch. Create a new branch from `dev` for the next feature.

### 3.2 dev -> test

Once a batch of features has been merged into `dev`, open a PR with `test` as the base and `dev` as the compare branch. After merging, run tests on `test`.

### 3.3 test -> main

Once testing on `test` passes, open a PR with `main` as the base and `test` as the compare branch.

### 3.4 When Testing Finds a Problem

Do not make fixes directly on `test` or `main`. Create a new feature branch from `dev` for the fix, then go through `feature branch -> dev -> test -> main` again.

## 4. PR Requirements

- The title says what was done, e.g. `[BE] Campus email registration`.
- The description clearly states:
  - What this change does
  - The related feature or task
- One PR covers one feature. Do not mix in unrelated changes.
