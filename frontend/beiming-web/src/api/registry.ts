export type ApiMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type ApiRegistryEntry = {
  method: ApiMethod;
  pathTemplate: string;
  moduleKey: string;
  scope: "public" | "me" | "admin" | "ops" | "platform" | "node";
  authRequired: boolean;
  requiredRoles: string[];
  requiredPermissions: string[];
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  confirmText: string | null;
  pageRoute: string;
  uiSurface: "list" | "detail" | "form" | "action" | "summary" | "audit" | "diagnostic";
  degradeMode: "PARTIAL_DEGRADE" | "PAGE_ERROR" | "ACTION_ERROR" | "AUTH_REDIRECT" | "FORBIDDEN";
  sensitiveFieldPolicy: string;
  testCaseGroup: string;
};

export const API_REGISTRY: ApiRegistryEntry[] = [
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/events",
    "moduleKey": "activity",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/activity",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/events/{activityIdOrSlug}",
    "moduleKey": "activity",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/activity",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/events/{activityId}/result",
    "moduleKey": "activity",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/activity",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/calendar-summary",
    "moduleKey": "activity",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/activity",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/me/registrations",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/me/registrations/{registrationId}",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/me/events/{activityId}/registrations",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/me/registrations/{registrationId}/cancel",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/me/events/{activityId}/check-in",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/me/rewards",
    "moduleKey": "activity",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/activity",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/admin/events",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/admin/events",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/submit",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/approve",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/reject",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/request-changes",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/publish",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/open-registration",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/close-registration",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/start",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/complete",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/offline",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/archive",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/delete",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/registrations",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/confirm",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/reject",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/promote",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/cancel",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/check-in",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/registrations/{registrationId}/no-show",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/result",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/result/publish",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/rewards",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/rewards/{rewardId}/issue",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/activity/admin/rewards/{rewardId}/revoke",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/activity/admin/events/{activityId}/contribution-candidates",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/admin/audit-logs",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/admin/ops/summary",
    "moduleKey": "activity",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/activity",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ACTIVITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/overview",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "summary",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/modules",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/modules/{moduleKey}",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/todos",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/todos/{todoId}",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/metrics/summary",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "summary",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/audit-logs",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/settings",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/admin/settings",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/admin/ops/summary",
    "moduleKey": "admin",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/admin",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ADMIN"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/health",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/ops/summary",
    "moduleKey": "alerting",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/sources",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/sources/{sourceId}",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/rules",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/rules/{ruleId}",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/alerting/rules",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/rules/{ruleId}",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/rules/{ruleId}/enable",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/rules/{ruleId}/disable",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/alerting/rules/{ruleId}/evaluate",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/alerts",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/alerts/{alertId}",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/alerts/{alertId}/acknowledge",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/alerts/{alertId}/close",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/silences",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/alerting/silences",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/silences/{silenceId}/cancel",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/routes",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/alerting/routes",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/alerting/routes/{routeId}",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/alerting/routes/{routeId}/test",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/deliveries",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/alerting/audit-logs",
    "moduleKey": "alerting",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/alerting",
    "uiSurface": "audit",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ALERTING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/session/verify",
    "moduleKey": "api-gateway",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/health",
    "moduleKey": "api-gateway",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/admin/ops/summary",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/admin/routes",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/admin/routes/{routeId}",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/admin/upstreams",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/gateway/admin/upstreams/{serviceKey}/health-refresh",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/gateway/admin/request-logs",
    "moduleKey": "api-gateway",
    "scope": "platform",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/platform/api-gateway",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-API_GATEWAY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/leaderboard",
    "moduleKey": "attendance",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/attendance",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/me/account",
    "moduleKey": "attendance",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/me/ledger",
    "moduleKey": "attendance",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/me/contributions",
    "moduleKey": "attendance",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/me/ranking",
    "moduleKey": "attendance",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/accounts",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/accounts/{accountId}",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/initializations",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/accounts/{accountId}/adjustments",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/ledger/{ledgerId}/reverse",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/contributions",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/attendance/admin/contributions/{contributionId}",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/monthly-runs/preview",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/monthly-runs",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/monthly-runs/{runId}",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/removal-candidates",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/attendance/admin/removal-candidates/{candidateId}/confirm",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/attendance/admin/removal-candidates/{candidateId}/dismiss",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/attendance/admin/leaderboard/rebuild",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/audit-logs",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/attendance/admin/ops/summary",
    "moduleKey": "attendance",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/attendance",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ATTENDANCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/register",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/login",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/logout",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/me",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/session/verify",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/me/sessions",
    "moduleKey": "auth",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "DELETE",
    "pathTemplate": "/api/v1/auth/me/sessions/{sessionId}",
    "moduleKey": "auth",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/me/password",
    "moduleKey": "auth",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/password-reset/request",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/password-reset/confirm",
    "moduleKey": "auth",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/auth/me/minecraft-binding",
    "moduleKey": "auth",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "DELETE",
    "pathTemplate": "/api/v1/auth/me/minecraft-binding",
    "moduleKey": "auth",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/admin/users",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/admin/users/{userId}",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/auth/admin/users/{userId}",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/auth/admin/users/{userId}/roles",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/admin/invitations",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/auth/admin/invitations",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/auth/admin/invitations/{invitationId}/disable",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/auth",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/auth/admin/invitations/{invitationId}/usage-records",
    "moduleKey": "auth",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/auth",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-AUTH"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/health",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/ops/summary",
    "moduleKey": "backup-recovery",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/domains",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/policies",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/policies/{policyId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/backup-recovery/policies",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/policies/{policyId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/policies/{policyId}/enable",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/policies/{policyId}/disable",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/backup-recovery/jobs",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/jobs",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/jobs/{jobId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/jobs/{jobId}/cancel",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/backup-points",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/backup-points/{backupPointId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/backup-recovery/backup-points/{backupPointId}/verify",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/backup-recovery/restore-drills",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/restore-drills",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/restore-drills/{drillId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/backup-recovery/restore-requests",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/restore-requests",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/restore-requests/{restoreRequestId}",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/restore-requests/{restoreRequestId}/approve",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/backup-recovery/restore-requests/{restoreRequestId}/reject",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/backup-recovery/audit-logs",
    "moduleKey": "backup-recovery",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/backup-recovery",
    "uiSurface": "audit",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-BACKUP_RECOVERY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/activity/calendar-summary",
    "moduleKey": "calendar",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/calendar",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/events",
    "moduleKey": "calendar",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/calendar",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/events/{eventId}",
    "moduleKey": "calendar",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/calendar",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/month",
    "moduleKey": "calendar",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/calendar",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/upcoming",
    "moduleKey": "calendar",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/calendar",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/me/watchlist",
    "moduleKey": "calendar",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/calendar/me/events/{eventId}/watch",
    "moduleKey": "calendar",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/calendar/me/events/{eventId}/unwatch",
    "moduleKey": "calendar",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/admin/events",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/calendar/admin/events",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/submit",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/approve",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/reject",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/publish",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/offline",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/archive",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/calendar/admin/events/{eventId}/delete",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/calendar/admin/sync/activity",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/admin/audit-logs",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/calendar/admin/ops/summary",
    "moduleKey": "calendar",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/calendar",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CALENDAR"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/releases",
    "moduleKey": "changelog",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/changelog",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/releases/{releaseIdOrSlug}",
    "moduleKey": "changelog",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/changelog",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/versions/latest",
    "moduleKey": "changelog",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/changelog",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/tags",
    "moduleKey": "changelog",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/changelog",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/changes",
    "moduleKey": "changelog",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/changelog",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/me/bookmarks",
    "moduleKey": "changelog",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/changelog/me/releases/{releaseId}/bookmark",
    "moduleKey": "changelog",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/changelog/me/releases/{releaseId}/unbookmark",
    "moduleKey": "changelog",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/admin/releases",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/changelog/admin/releases",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/submit",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/approve",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/reject",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/request-changes",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/publish",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/offline",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/archive",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/delete",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/changelog/admin/releases/{releaseId}/calendar-sync",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/admin/audit-logs",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/changelog/admin/ops/summary",
    "moduleKey": "changelog",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/changelog",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CHANGELOG"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/health",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/ops/summary",
    "moduleKey": "cloudreve-sync",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/providers",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/providers/{providerId}",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cloudreve-sync/providers",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cloudreve-sync/providers/{providerId}",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cloudreve-sync/providers/{providerId}/disable",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cloudreve-sync/providers/{providerId}/enable",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/files",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/shares",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cloudreve-sync/shares/resolve",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cloudreve-sync/sync-jobs",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/sync-jobs",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/sync-jobs/{jobId}",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cloudreve-sync/sync-jobs/{jobId}/cancel",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cloudreve-sync/audit-logs",
    "moduleKey": "cloudreve-sync",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cloudreve-sync",
    "uiSurface": "audit",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CLOUDREVE_SYNC"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/boards",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/boards/{boardId}",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/posts",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/posts/{postId}",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/posts/{postId}/comments",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/polls/{pollId}",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/search",
    "moduleKey": "community",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/community",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/me/posts/{postId}",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/submit",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/withdraw",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/comments",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/me/comments/{commentId}",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/me/comments/{commentId}/archive",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/like",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "DELETE",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/like",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/comments/{commentId}/like",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "DELETE",
    "pathTemplate": "/api/v1/community/me/comments/{commentId}/like",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/favorite",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "DELETE",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/favorite",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/polls/{pollId}/votes",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/posts/{postId}/reports",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/comments/{commentId}/reports",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/me/reports",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/tickets",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/me/tickets",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/me/tickets/{ticketId}",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/me/tickets/{ticketId}",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/me/tickets/{ticketId}/close",
    "moduleKey": "community",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/boards",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/admin/boards",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/boards/{boardId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/boards/{boardId}/archive",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/posts",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/approve",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/reject",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/request-changes",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/offline",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/archive",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/posts/{postId}/delete",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/comments",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/comments/{commentId}/approve",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/reports",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/reports/{reportId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/reports/{reportId}/assign",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/reports/{reportId}/resolve",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/reports/{reportId}/dismiss",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/tickets",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/tickets/{ticketId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/tickets/{ticketId}/assign",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/admin/tickets/{ticketId}/messages",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/tickets/{ticketId}/status",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/admin/penalties",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/penalties/{penaltyId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/penalties/{penaltyId}/revoke",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/community/admin/polls",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/polls/{pollId}",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/polls/{pollId}/open",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/community/admin/polls/{pollId}/close",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/community",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/audit-logs",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/community/admin/ops/summary",
    "moduleKey": "community",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/community",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-COMMUNITY"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/home",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/items",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/items/{contentId}",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/items/by-slug/{slug}",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/items/{contentId}/preview",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/categories",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/tags",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/topics",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/topics/{topicId}",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/topics/by-slug/{slug}",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/seo",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/seo/sitemap",
    "moduleKey": "content",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/content",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/items",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/items",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/preview-token",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/submit-review",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/approve",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/reject",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/request-changes",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/publish",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/offline",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/archive",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/delete",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/versions",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/versions/{version}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/versions/{version}/restore",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/items/{contentId}/audit-logs",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/home",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/content/admin/home",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/home/preview",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/home/publish",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/home/rollback",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/categories",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/categories",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/categories/{categoryId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/categories/{categoryId}/archive",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/tags",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/tags",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/tags/{tagId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/tags/{tagId}/archive",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/topics",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/content/admin/topics",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}/publish",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}/offline",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}/archive",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/topics/{topicId}/delete",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/seo",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/seo/{seoId}",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/content/admin/seo/by-route",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/content/admin/seo/{seoId}/disable",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/content",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/content/admin/ops/summary",
    "moduleKey": "content",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/content",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CONTENT"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/health",
    "moduleKey": "cross-platform-notification",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/ops/summary",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers/{providerId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers/{providerId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers/{providerId}/enable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers/{providerId}/disable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/providers/{providerId}/archive",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/capabilities",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/capabilities/{capabilityId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/enable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/disable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/template-mappings/{mappingId}/archive",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}/enable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}/disable",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}/archive",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/routes/{routeId}/test",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/deliveries",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/deliveries",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/retry",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/deliveries/{deliveryId}/cancel",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/attempts",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/attempts/{attemptId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/receivers",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/receivers/{receiverId}",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/cross-platform-notification/admin/audit-logs",
    "moduleKey": "cross-platform-notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/cross-platform-notification",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-CROSS_PLATFORM_NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/exams/me/sessions",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/me/sessions/current",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/me/sessions",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/me/sessions/{sessionId}/paper",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/exams/me/sessions/{sessionId}/answers",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/exams/me/sessions/{sessionId}/submit",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/me/sessions/{sessionId}/supplement",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/me/sessions/{sessionId}/result",
    "moduleKey": "exam",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/exams",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/sessions",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/manual-review",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/result-correction",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/request-supplement",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/cancel",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/question-bank/questions",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/question-bank/questions/{questionId}/versions",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/exams/admin/question-bank/questions",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/question-bank/questions/{questionId}",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/question-bank/questions/{questionId}/archive",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/paper-templates",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/exams/admin/paper-templates",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/paper-templates/{templateId}",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/paper-templates/{templateId}/publish-preview",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/paper-templates/{templateId}/publish",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/exams/admin/paper-templates/{templateId}/archive",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/exam",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/audit-logs",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/ops/summary",
    "moduleKey": "exam",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/exam",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-EXAM"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/home",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/categories",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/articles",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/articles/{guideId}",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/articles/by-slug/{slug}",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/search",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/commands",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/external-channels",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/rules/current",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/rules/versions/{ruleVersion}",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/guides/articles/{guideId}/feedback",
    "moduleKey": "guide",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/guides",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/articles",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/guides/admin/articles",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/submit-review",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/approve",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/reject",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/request-changes",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/publish",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/offline",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/archive",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/delete",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/versions",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/versions/{version}",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/versions/{version}/restore",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/guides/admin/feedback/{feedbackId}/resolve",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/articles/{guideId}/audit-logs",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/guides/admin/ops/summary",
    "moduleKey": "guide",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/guide",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-GUIDE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/featured",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/{materialId}",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/by-slug/{slug}",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/categories",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/{materialId}/assets",
    "moduleKey": "material",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/materials",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/materials/me/upload-sessions",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/me/upload-sessions/{uploadSessionId}/complete",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/materials/me/submissions",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/me/submissions",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/me/submissions/{materialId}",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/me/submissions/{materialId}",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/me/submissions/{materialId}/submit-review",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/me/submissions/{materialId}/withdraw",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/me/submissions/{materialId}/resubmit",
    "moduleKey": "material",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/items",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/approve",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/reject",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/request-changes",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/feature",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/unfeature",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/offline",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/archive",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/delete",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/categories",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/materials/admin/categories",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/categories/{categoryId}",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/categories/{categoryId}/archive",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/assets",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/materials/admin/assets/{assetId}/security-status",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/items/{materialId}/audit-logs",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/materials/admin/ops/summary",
    "moduleKey": "material",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/material",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-MATERIAL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/node-daemon/tasks",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/heartbeat",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/tasks/{taskId}/node-result",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/health",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/ops/summary",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/capabilities",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/node-daemon/registration/handshake",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/runtime/snapshot",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/node-daemon/runtime/heartbeat",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/tasks",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/tasks/{nodeRequestId}",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/node-daemon/tasks/{nodeRequestId}/cancel",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/tasks/{nodeRequestId}/result",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/files",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "FILE_MANAGE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/node-daemon/files/read",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "FILE_MANAGE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/node-daemon/logs/query",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/node-daemon/audit-logs",
    "moduleKey": "node-daemon",
    "scope": "node",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NODE_DAEMON"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/notifications/admin/messages",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/notifications/admin/messages/from-template",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/me",
    "moduleKey": "notification",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/notifications",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/me/unread-count",
    "moduleKey": "notification",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/notifications",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/me/{notificationId}",
    "moduleKey": "notification",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/notifications",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/me/{notificationId}/read",
    "moduleKey": "notification",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/notifications",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/me/read-all",
    "moduleKey": "notification",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/notifications",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/me/{notificationId}/archive",
    "moduleKey": "notification",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/me/notifications",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/messages",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/messages/{notificationId}",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/templates",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/templates/{templateId}",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/notifications/admin/templates/preview",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/notifications/admin/templates",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/admin/templates/{templateId}",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/admin/templates/{templateId}/disable",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/notifications/admin/templates/{templateId}/enable",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/messages/{notificationId}/audit-logs",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/notifications/admin/ops/summary",
    "moduleKey": "notification",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/notification",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-NOTIFICATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/me/progress",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/onboarding/me/start",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/me/profile-confirmation",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/me/rules-confirmation",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/me/direction",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/onboarding/me/advance",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/me/next-action",
    "moduleKey": "onboarding",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/onboarding",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/applications",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}/exam-handoff",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}/reset",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}/block",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/onboarding/admin/applications/{applicationId}/unblock",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/audit-logs",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/onboarding/admin/ops/summary",
    "moduleKey": "onboarding",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/onboarding",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONBOARDING"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/health",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/overview",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/providers",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/providers/{providerId}",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/worlds",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/layers",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/markers",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/regions",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/embed",
    "moduleKey": "online-map",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/map",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/ops/summary",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/providers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/online-map/admin/providers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}/enable",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}/disable",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}/archive",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}/health/refresh",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/providers/{providerId}/health/snapshots",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/worlds",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/online-map/admin/worlds/{worldId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/layers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/online-map/admin/layers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/layers/{layerId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/layers/{layerId}/archive",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/markers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/online-map/admin/markers",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/markers/{markerId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/markers/{markerId}/archive",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/regions",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/online-map/admin/regions",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/regions/{regionId}",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/online-map/admin/regions/{regionId}/archive",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/online-map",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/online-map/admin/audit-logs",
    "moduleKey": "online-map",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/online-map",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-ONLINE_MAP"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/overview",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "summary",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/assets",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/assets/{assetId}",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/capabilities",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/metrics/latest",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/containers",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "CONTAINER_OPERATE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/vms",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "VM_OPERATE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/minecraft-instances",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/files",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "FILE_MANAGE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/nodes",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/disable",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/enable",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/heartbeat",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/files/read",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "FILE_MANAGE"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/nodes/{nodeId}/logs/query",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/tasks",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/tasks",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/tasks/{taskId}",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-control/tasks/{taskId}/cancel",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-control/tasks/{taskId}/node-result",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/approvals",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-control/approvals/{approvalId}/approve",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-control/approvals/{approvalId}/reject",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/audit-logs",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-control/ops/summary",
    "moduleKey": "ops-control",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_CONTROL"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/health",
    "moduleKey": "ops-image-market",
    "scope": "ops",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [
      "NODE_READ"
    ],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/ops/images",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}/enable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}/disable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}/archive",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/providers/{providerId}/health-refresh",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/images",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/images",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}/publish",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}/block",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}/archive",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}/versions",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/images/{imageId}/versions",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}/approve",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}/deprecate",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}/block",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}/archive",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/enable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/disable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/compatibility-profiles/{profileId}/archive",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates/{templateId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates/{templateId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates/{templateId}/enable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates/{templateId}/disable",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/templates/{templateId}/archive",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/scans",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/scans/{scanId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/versions/{imageVersionId}/scans",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/pull-plans",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/pull-plans/{planId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/ops-image-market/admin/pull-plans",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/pull-plans/{planId}/approve",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/ops-image-market/admin/pull-plans/{planId}/cancel",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/cache-snapshots",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/cache-snapshots/{snapshotId}",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/ops-image-market/admin/audit-logs",
    "moduleKey": "ops-image-market",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/ops-image-market",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-OPS_IMAGE_MARKET"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/health",
    "moduleKey": "plugin-integration",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "diagnostic",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/ops/summary",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}/enable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}/disable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}/archive",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/instances",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/instances/{instanceId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/capabilities",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas/{schemaId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas/{schemaId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas/{schemaId}/enable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/event-schemas/{schemaId}/disable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/events/ingest",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/events",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/events/{eventId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/events/{eventId}/replay",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules/{ruleId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules/{ruleId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules/{ruleId}/enable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/route-rules/{ruleId}/disable",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/plugin-integration/admin/sync-tasks",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/sync-tasks",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/sync-tasks/{taskId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/sync-tasks/{taskId}/cancel",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/providers/{providerId}/health-snapshots",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/object-mappings",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/object-mappings/{mappingId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/plugin-integration/admin/object-mappings/{mappingId}",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/plugin-integration/admin/object-mappings/{mappingId}/archive",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/plugin-integration/admin/audit-logs",
    "moduleKey": "plugin-integration",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/plugin-integration",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PLUGIN_INTEGRATION"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/members",
    "moduleKey": "profile",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/members",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/members/{memberId}",
    "moduleKey": "profile",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/members",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/me",
    "moduleKey": "profile",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/members",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/me",
    "moduleKey": "profile",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/members",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/admin/members",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/profile/admin/members/activate",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}/status",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/admin/groups",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/profile/admin/groups",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/admin/groups/{groupId}",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/admin/groups/{groupId}/archive",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}/milestones",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "PUT",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}/work-snapshots",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}/audit-logs",
    "moduleKey": "profile",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/profile",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-PROFILE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/resources/{resourceId}/versions/{versionId}/download",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/audit-logs",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/ops/summary",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/{resourceId}",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/by-slug/{slug}",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/categories",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/{resourceId}/versions",
    "moduleKey": "resource",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/resources",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/items",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/resources/admin/items",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/submit-review",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/approve",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/reject",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/request-changes",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/publish",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/offline",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/archive",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/delete",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/versions",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/versions",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/versions/{versionId}",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/disable",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/items/{resourceId}/versions/{versionId}/enable",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/resources/admin/categories",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/resources/admin/categories",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/categories/{categoryId}",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/resources/admin/categories/{categoryId}/archive",
    "moduleKey": "resource",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/resource",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-RESOURCE"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/admin/audit-logs",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/admin/ops/summary",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/overview",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "summary",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/instances",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/instances/{instanceId}",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "detail",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/lines",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/history/snapshots",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/outages",
    "moduleKey": "server-status",
    "scope": "public",
    "authRequired": false,
    "requiredRoles": [],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/status",
    "uiSurface": "list",
    "degradeMode": "PARTIAL_DEGRADE",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/admin/sources",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/server-status/admin/sources",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/sources/{sourceId}",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/sources/{sourceId}/disable",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/sources/{sourceId}/enable",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/server-status/admin/sources/{sourceId}/refresh",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/admin/lines",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/server-status/admin/lines",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/lines/{lineId}",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/lines/{lineId}/disable",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/lines/{lineId}/enable",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/server-status/admin/outages",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/server-status/admin/outages",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/outages/{outageId}",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/outages/{outageId}/acknowledge",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/outages/{outageId}/resolve",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/server-status/admin/outages/{outageId}/archive",
    "moduleKey": "server-status",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "HIGH",
    "confirmText": "SOURCE_CONTRACT_CONFIRMATION",
    "pageRoute": "/admin/server-status",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-SERVER_STATUS"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/exams/admin/sessions/{sessionId}/whitelist-handoff",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/profile/admin/members/activate",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/profile/admin/members/{memberId}/status",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/whitelist/me/applications",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/me/applications/current",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/me/applications",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}/submit",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}/supplement",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}/withdraw",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/me/applications/{applicationId}/result",
    "moduleKey": "whitelist",
    "scope": "me",
    "authRequired": true,
    "requiredRoles": [
      "USER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/me/whitelist",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/applications",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "list",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/assign",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/request-supplement",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/approve",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/reject",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "MEDIUM",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "PATCH",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/remove",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "POST",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/reopen",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "action",
    "degradeMode": "ACTION_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/applications/{applicationId}/attendance-handoff",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "detail",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/audit-logs",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "audit",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  },
  {
    "method": "GET",
    "pathTemplate": "/api/v1/whitelist/admin/ops/summary",
    "moduleKey": "whitelist",
    "scope": "admin",
    "authRequired": true,
    "requiredRoles": [
      "HELPER",
      "ADMIN",
      "OWNER"
    ],
    "requiredPermissions": [],
    "riskLevel": "LOW",
    "confirmText": null,
    "pageRoute": "/admin/whitelist",
    "uiSurface": "diagnostic",
    "degradeMode": "PAGE_ERROR",
    "sensitiveFieldPolicy": "REDACT_CONTRACT_SENSITIVE_FIELDS",
    "testCaseGroup": "FEA-WHITELIST"
  }
];

export const API_REGISTRY_TOTAL = API_REGISTRY.length;
