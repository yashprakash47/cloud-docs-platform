export interface User {
  id: string;
  email: string;
  displayName: string;
  status: string;
}

export interface Tenant {
  id: string;
  name: string;
  createdAt: string;
}

export interface DocumentItem {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
  currentVersionId?: string;
  folderId?: string;
  createdAt: string;
  updatedAt: string;
  archivedAt?: string;
}

export interface DocumentVersion {
  id: string;
  tenantId: string;
  documentId: string;
  versionNumber: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  checksum: string;
  createdAt: string;
}

export interface ProcessingJob {
  id: string;
  tenantId: string;
  documentId: string;
  status: 'QUEUED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED';
  retryCount: number;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Folder {
  id: string;
  tenantId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface Tag {
  id: string;
  tenantId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface Membership {
  id: string;
  tenantId: string;
  userId: string;
  userEmail?: string;
  role: 'TENANT_ADMIN' | 'COLLABORATOR' | 'VIEWER';
  createdAt: string;
}

const DEFAULT_EMAIL = 'admin@clouddocs.com';

function getHeaders(customHeaders: Record<string, string> = {}) {
  return {
    'X-User-Email': DEFAULT_EMAIL,
    ...customHeaders,
  };
}

export const ALLOWED_MIME_TYPES = ['text/plain', 'application/pdf', 'image/png'] as const;
export type AllowedMimeType = (typeof ALLOWED_MIME_TYPES)[number];

export const ALLOWED_FILE_EXTENSIONS = ['.txt', '.pdf', '.png'] as const;

export function getExactMimeType(file: File): string {
  const rawType = (file.type || '').toLowerCase().split(';')[0].trim();
  if (ALLOWED_MIME_TYPES.includes(rawType as AllowedMimeType)) {
    return rawType;
  }
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (ext === 'txt') return 'text/plain';
  if (ext === 'pdf') return 'application/pdf';
  if (ext === 'png') return 'image/png';
  return rawType;
}

export function extractFilenameFromContentDisposition(disposition: string | null, fallback: string = 'document'): string {
  if (!disposition) return fallback;

  // RFC 5987 / RFC 6266 filename* (UTF-8 encoded)
  const filenameStarMatch = disposition.match(/filename\*\s*=\s*(?:UTF-8|utf-8)''([^;\n]+)/i);
  if (filenameStarMatch && filenameStarMatch[1]) {
    try {
      return decodeURIComponent(filenameStarMatch[1].trim().replace(/^['"]|['"]$/g, ''));
    } catch {
      return filenameStarMatch[1].trim().replace(/^['"]|['"]$/g, '');
    }
  }

  // Standard filename="..." or filename=...
  const filenameMatch = disposition.match(/filename\s*=\s*(?:"([^"]+)"|'([^']+)'|([^;\n]+))/i);
  if (filenameMatch) {
    const val = filenameMatch[1] || filenameMatch[2] || filenameMatch[3];
    if (val) {
      return val.trim();
    }
  }

  return fallback;
}

const API_BASE = '';

export const api = {
  async getHealth(): Promise<{ status: string }> {
    const res = await fetch(`${API_BASE}/actuator/health`);
    if (!res.ok) throw new Error('Health check failed');
    return res.json();
  },

  async getCurrentUser(): Promise<User> {
    const res = await fetch(`${API_BASE}/api/v1/me`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch user');
    return res.json();
  },

  async getTenants(): Promise<Tenant[]> {
    const res = await fetch(`${API_BASE}/api/v1/tenants`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch workspaces');
    return res.json();
  },

  async createTenant(name: string): Promise<Tenant> {
    const res = await fetch(`${API_BASE}/api/v1/tenants`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ name }),
    });
    if (!res.ok) throw new Error('Failed to create workspace');
    return res.json();
  },

  async getDocuments(tenantId: string): Promise<PageResponse<DocumentItem>> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch documents');
    return res.json();
  },

  async createDocument(tenantId: string, name: string, description?: string, folderId?: string | null): Promise<DocumentItem> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ name, description, folderId: folderId ?? null }),
    });
    if (!res.ok) throw new Error('Failed to create document');
    return res.json();
  },

  async archiveDocument(tenantId: string, documentId: string): Promise<DocumentItem> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/archive`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to archive document');
    return res.json();
  },

  async uploadDocumentFile(tenantId: string, documentId: string, file: File): Promise<DocumentVersion> {
    const exactContentType = getExactMimeType(file);
    if (!ALLOWED_MIME_TYPES.includes(exactContentType as AllowedMimeType)) {
      throw new Error('File type is not allowed. Allowed types: text/plain (.txt), application/pdf (.pdf), and image/png (.png)');
    }

    const intentRes = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/uploads/intents`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        originalFilename: file.name,
        contentType: exactContentType,
        sizeBytes: file.size,
      }),
    });
    if (!intentRes.ok) {
      const err = await intentRes.json().catch(() => null);
      throw new Error(err?.message || 'Failed to initiate upload');
    }
    const intent = await intentRes.json();

    const filePayload = file.type === exactContentType
      ? file
      : new File([file], file.name, { type: exactContentType });

    const formData = new FormData();
    formData.append('file', filePayload);
    const uploadRes = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/uploads/intents/${intent.id}/content`, {
      method: 'POST',
      headers: getHeaders(),
      body: formData,
    });
    if (!uploadRes.ok) {
      const err = await uploadRes.json().catch(() => null);
      throw new Error(err?.message || 'Failed to upload file content');
    }

    const completeRes = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/uploads/intents/${intent.id}/complete`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!completeRes.ok) {
      const err = await completeRes.json().catch(() => null);
      throw new Error(err?.message || 'Failed to finalize file upload');
    }
    return completeRes.json();
  },

  async downloadDocument(tenantId: string, documentId: string, versionId: string, fallbackName?: string): Promise<{ blob: Blob; filename: string }> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/versions/${versionId}/content`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(`Download failed with status ${res.status}`);
    const blob = await res.blob();
    const disposition = res.headers.get('content-disposition');
    const filename = extractFilenameFromContentDisposition(disposition, fallbackName || 'document');
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    }, 100);
    return { blob, filename };
  },

  async getProcessingJobs(tenantId: string, documentId: string): Promise<ProcessingJob[]> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/processing-jobs`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch processing jobs');
    return res.json();
  },

  async retryProcessingJob(tenantId: string, jobId: string): Promise<ProcessingJob> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/processing-jobs/${jobId}/retry`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to retry processing job');
    return res.json();
  },

  async getFolders(tenantId: string, parentId?: string | null): Promise<Folder[]> {
    const query = parentId ? `?parentId=${encodeURIComponent(parentId)}` : '';
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/folders${query}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch folders');
    return res.json();
  },

  async createFolder(tenantId: string, name: string, parentId?: string | null): Promise<Folder> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/folders`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ name, parentId: parentId ?? null }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to create folder');
    }
    return res.json();
  },

  async getTags(tenantId: string, documentId: string): Promise<Tag[]> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/tags`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch tags');
    return res.json();
  },

  async createTag(tenantId: string, name: string): Promise<Tag> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/tags`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ name }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to create tag');
    }
    return res.json();
  },

  async attachTag(tenantId: string, documentId: string, tagId: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/tags/${tagId}`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to attach tag');
    }
  },

  async removeTag(tenantId: string, documentId: string, tagId: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/documents/${documentId}/tags/${tagId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to remove tag');
    }
  },

  async getMemberships(tenantId: string): Promise<Membership[]> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/memberships`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch memberships');
    const data = await res.json();
    return data.map((m: any) => ({
      ...m,
      role: m.role === 'MEMBER' ? 'COLLABORATOR' : m.role,
    }));
  },

  async addMembership(tenantId: string, email: string, role: string): Promise<Membership> {
    const backendRole = role === 'COLLABORATOR' ? 'MEMBER' : role;
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/memberships`, {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ email, role: backendRole }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to add member');
    }
    const data = await res.json();
    return {
      ...data,
      role: data.role === 'MEMBER' ? 'COLLABORATOR' : data.role,
    };
  },

  async removeMembership(tenantId: string, membershipId: string): Promise<void> {
    const res = await fetch(`${API_BASE}/api/v1/tenants/${tenantId}/memberships/${membershipId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => null);
      throw new Error(err?.message || 'Failed to remove member');
    }
  },
};
