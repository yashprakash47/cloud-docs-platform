import React, { useState, useEffect } from 'react';
import {
  api,
  Tenant,
  DocumentItem,
  ProcessingJob,
  Folder,
  Tag,
  Membership,
  getExactMimeType,
  ALLOWED_MIME_TYPES,
  AllowedMimeType,
  getCurrentUserEmail,
  setCurrentUserEmail,
  clearCurrentUserEmail,
} from '../services/api';
import { LandingPage } from './LandingPage';
import { Footer } from './Footer';
import './app.css';

export function App() {
  const [userEmail, setUserEmail] = useState<string | null>(() => getCurrentUserEmail());
  const [healthStatus, setHealthStatus] = useState<string>('CHECKING');
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState<boolean>(false);

  // Search and status filter state
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DRAFT' | 'ARCHIVED'>('ALL');
  const [isMobileNavOpen, setIsMobileNavOpen] = useState<boolean>(false);

  // Folders state
  const [folders, setFolders] = useState<Folder[]>([]);
  const [selectedFolderId, setSelectedFolderId] = useState<string | 'ALL' | 'UNFILED' | 'TRASH'>('ALL');
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState<boolean>(false);
  const [newFolderName, setNewFolderName] = useState<string>('');
  const [isCreatingFolder, setIsCreatingFolder] = useState<boolean>(false);
  const [folderError, setFolderError] = useState<string | null>(null);

  // Preview viewer modal state
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);
  const [previewData, setPreviewData] = useState<{ url: string; contentType: string; textContent?: string } | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState<boolean>(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  // Upload modal state
  const [isUploadOpen, setIsUploadOpen] = useState<boolean>(false);
  const [docName, setDocName] = useState<string>('');
  const [docDescription, setDocDescription] = useState<string>('');
  const [uploadFolderId, setUploadFolderId] = useState<string>('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadStatus, setUploadStatus] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState<boolean>(false);

  // Tags state
  const [docTags, setDocTags] = useState<Record<string, Tag[]>>({});
  const [knownTags, setKnownTags] = useState<Tag[]>([]);
  const [activeDocForTag, setActiveDocForTag] = useState<DocumentItem | null>(null);
  const [newTagName, setNewTagName] = useState<string>('');
  const [isSubmittingTag, setIsSubmittingTag] = useState<boolean>(false);
  const [tagError, setTagError] = useState<string | null>(null);

  // Processing jobs drawer state
  const [activeDocForJobs, setActiveDocForJobs] = useState<DocumentItem | null>(null);
  const [jobs, setJobs] = useState<ProcessingJob[]>([]);
  const [isLoadingJobs, setIsLoadingJobs] = useState<boolean>(false);

  // Members modal state
  const [isMembersOpen, setIsMembersOpen] = useState<boolean>(false);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [isLoadingMembers, setIsLoadingMembers] = useState<boolean>(false);
  const [memberError, setMemberError] = useState<string | null>(null);
  const [newMemberEmail, setNewMemberEmail] = useState<string>('');
  const [newMemberRole, setNewMemberRole] = useState<'COLLABORATOR' | 'VIEWER' | 'TENANT_ADMIN'>('COLLABORATOR');
  const [isAddingMember, setIsAddingMember] = useState<boolean>(false);

  // Theme state
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('clouddocs_theme');
      if (saved === 'light' || saved === 'dark') return saved;
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    } catch {
      return 'light';
    }
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem('clouddocs_theme', theme);
    } catch (e) {
      console.warn('Failed to save theme preference', e);
    }
  }, [theme]);

  function toggleTheme() {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  }

  function handleSignOut() {
    clearCurrentUserEmail();
    setUserEmail(null);
    setDocuments([]);
    setFolders([]);
    setSelectedTenant(null);
  }

  useEffect(() => {
    api.getHealth()
      .then(res => setHealthStatus(res.status))
      .catch(() => setHealthStatus('DOWN'));

    if (userEmail) {
      api.getTenants()
        .then(data => {
          setTenants(data);
          if (data.length > 0) setSelectedTenant(data[0]);
        })
        .catch(err => console.error('Failed to load workspaces:', err));
    }
  }, [userEmail]);

  useEffect(() => {
    if (!selectedTenant) return;
    loadFolders(selectedTenant.id);
    loadDocuments(selectedTenant.id);
  }, [selectedTenant]);

  async function loadFolders(tenantId: string) {
    try {
      const folderList = await api.getFolders(tenantId);
      setFolders(folderList);
    } catch (err) {
      console.error('Failed to load folders:', err);
    }
  }

  async function loadDocuments(tenantId: string) {
    setIsLoadingDocs(true);
    try {
      const page = await api.getDocuments(tenantId);
      const docs = page.content || [];
      setDocuments(docs);

      const tagsMap: Record<string, Tag[]> = {};
      const allFoundTags: Tag[] = [];
      await Promise.all(
        docs.map(async (doc) => {
          try {
            const tags = await api.getTags(tenantId, doc.id);
            tagsMap[doc.id] = tags;
            allFoundTags.push(...tags);
          } catch {
            tagsMap[doc.id] = [];
          }
        })
      );
      setDocTags(tagsMap);

      setKnownTags(prev => {
        const unique = new Map<string, Tag>();
        [...prev, ...allFoundTags].forEach(t => unique.set(t.id, t));
        return Array.from(unique.values());
      });
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setIsLoadingDocs(false);
    }
  }

  async function handleCreateFolder(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !newFolderName.trim()) return;
    setIsCreatingFolder(true);
    setFolderError(null);
    try {
      const created = await api.createFolder(selectedTenant.id, newFolderName.trim());
      setFolders(prev => [...prev, created]);
      setSelectedFolderId(created.id);
      setIsCreateFolderOpen(false);
      setNewFolderName('');
    } catch (err) {
      setFolderError(err instanceof Error ? err.message : 'Failed to create folder');
    } finally {
      setIsCreatingFolder(false);
    }
  }

  function openUploadModal() {
    setDocName('');
    setDocDescription('');
    setUploadFolderId(selectedFolderId !== 'ALL' && selectedFolderId !== 'UNFILED' ? selectedFolderId : '');
    setSelectedFile(null);
    setUploadStatus(null);
    setIsUploadOpen(true);
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const mime = getExactMimeType(file);
      if (!mime || !ALLOWED_MIME_TYPES.includes(mime as AllowedMimeType)) {
        alert('File type is not allowed. Only text/plain (.txt), application/pdf (.pdf), and image/png (.png) are supported.');
        e.target.value = '';
        setSelectedFile(null);
        return;
      }
      setSelectedFile(file);
      if (!docName.trim()) {
        const base = file.name.replace(/\.[^/.]+$/, '');
        setDocName(base);
      }
    }
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !selectedFile || !docName.trim()) return;

    if (selectedFile.size > 10 * 1024 * 1024) {
      alert('File exceeds maximum size limit of 10 MB');
      return;
    }

    const exactMime = getExactMimeType(selectedFile);
    if (!exactMime || !ALLOWED_MIME_TYPES.includes(exactMime as AllowedMimeType)) {
      alert('File type is not allowed. Allowed types: text/plain (.txt), application/pdf (.pdf), and image/png (.png)');
      return;
    }

    setIsUploading(true);
    setUploadStatus('Creating document intent...');

    try {
      const doc = await api.createDocument(
        selectedTenant.id,
        docName.trim(),
        docDescription.trim() || undefined,
        uploadFolderId ? uploadFolderId : null
      );

      setUploadStatus('Uploading file payload...');
      await api.uploadDocumentFile(selectedTenant.id, doc.id, selectedFile);

      setUploadStatus('Upload complete! Refreshing catalog...');
      await loadDocuments(selectedTenant.id);

      setTimeout(() => {
        setIsUploadOpen(false);
        setIsUploading(false);
        setUploadStatus(null);
      }, 500);
    } catch (err: unknown) {
      console.error(err);
      setUploadStatus(err instanceof Error ? err.message : 'Upload failed');
      setIsUploading(false);
    }
  }

  async function handleAddTag(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !activeDocForTag || !newTagName.trim()) return;
    setIsSubmittingTag(true);
    setTagError(null);
    try {
      const trimmed = newTagName.trim();
      let tag = knownTags.find(t => t.name.toLowerCase() === trimmed.toLowerCase());
      if (!tag) {
        tag = await api.createTag(selectedTenant.id, trimmed);
        setKnownTags(prev => [...prev, tag!]);
      }

      const currentDocTags = docTags[activeDocForTag.id] || [];
      if (!currentDocTags.some(t => t.id === tag!.id)) {
        await api.attachTag(selectedTenant.id, activeDocForTag.id, tag.id);
        setDocTags(prev => ({
          ...prev,
          [activeDocForTag.id]: [...(prev[activeDocForTag.id] || []), tag!],
        }));
      }

      setNewTagName('');
      setActiveDocForTag(null);
    } catch (err) {
      setTagError(err instanceof Error ? err.message : 'Failed to add tag');
    } finally {
      setIsSubmittingTag(false);
    }
  }

  async function handleAttachExistingTag(docId: string, tag: Tag) {
    if (!selectedTenant) return;
    try {
      await api.attachTag(selectedTenant.id, docId, tag.id);
      setDocTags(prev => ({
        ...prev,
        [docId]: [...(prev[docId] || []), tag],
      }));
      setActiveDocForTag(null);
    } catch (err) {
      setTagError(err instanceof Error ? err.message : 'Failed to attach tag');
    }
  }

  async function handleRemoveTag(docId: string, tagId: string) {
    if (!selectedTenant) return;
    try {
      await api.removeTag(selectedTenant.id, docId, tagId);
      setDocTags(prev => ({
        ...prev,
        [docId]: (prev[docId] || []).filter(t => t.id !== tagId),
      }));
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to remove tag');
    }
  }

  async function handleDownload(doc: DocumentItem) {
    if (!selectedTenant || !doc.currentVersionId) return;
    try {
      await api.downloadDocument(selectedTenant.id, doc.id, doc.currentVersionId, doc.name);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to download document');
    }
  }

  function getDaysRemaining(deletedAt?: string): number {
    if (!deletedAt) return 30;
    const deletedTime = new Date(deletedAt).getTime();
    const now = Date.now();
    const elapsedDays = (now - deletedTime) / (1000 * 60 * 60 * 24);
    return Math.max(0, Math.ceil(30 - elapsedDays));
  }

  async function openPreview(doc: DocumentItem) {
    if (!selectedTenant || !doc.currentVersionId) return;
    setPreviewDoc(doc);
    setPreviewData(null);
    setIsLoadingPreview(true);
    setPreviewError(null);
    try {
      const data = await api.viewDocumentBlob(selectedTenant.id, doc.id, doc.currentVersionId);
      setPreviewData(data);
    } catch (err) {
      setPreviewError(err instanceof Error ? err.message : 'Failed to load preview');
    } finally {
      setIsLoadingPreview(false);
    }
  }

  function closePreview() {
    if (previewData?.url) {
      window.URL.revokeObjectURL(previewData.url);
    }
    setPreviewDoc(null);
    setPreviewData(null);
    setIsLoadingPreview(false);
    setPreviewError(null);
  }

  async function handleArchive(docId: string) {
    if (!selectedTenant) return;
    try {
      await api.archiveDocument(selectedTenant.id, docId);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert('Failed to archive document');
    }
  }

  async function handleUnarchive(docId: string) {
    if (!selectedTenant) return;
    try {
      await api.unarchiveDocument(selectedTenant.id, docId);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to unarchive document');
    }
  }

  async function handleTrash(docId: string) {
    if (!selectedTenant) return;
    try {
      await api.moveToTrash(selectedTenant.id, docId);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to move document to trash');
    }
  }

  async function handleRestore(docId: string) {
    if (!selectedTenant) return;
    try {
      await api.restoreDocument(selectedTenant.id, docId);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to restore document');
    }
  }

  async function handlePermanentDelete(doc: DocumentItem) {
    if (!selectedTenant) return;
    const confirmMsg = `Are you sure you want to permanently delete "${doc.name}"?\n\nThis will purge all versions and AWS S3 stored files. This action CANNOT be undone.`;
    if (!window.confirm(confirmMsg)) return;

    try {
      await api.permanentlyDeleteDocument(selectedTenant.id, doc.id);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to permanently delete document');
    }
  }

  async function openJobsDrawer(doc: DocumentItem) {
    if (!selectedTenant) return;
    setActiveDocForJobs(doc);
    setIsLoadingJobs(true);
    try {
      const jobList = await api.getProcessingJobs(selectedTenant.id, doc.id);
      setJobs(jobList);
    } catch (err) {
      console.error('Failed to load jobs:', err);
    } finally {
      setIsLoadingJobs(false);
    }
  }

  async function handleRetryJob(jobId: string) {
    if (!selectedTenant || !activeDocForJobs) return;
    try {
      await api.retryProcessingJob(selectedTenant.id, jobId);
      const jobList = await api.getProcessingJobs(selectedTenant.id, activeDocForJobs.id);
      setJobs(jobList);
    } catch (err) {
      alert('Failed to retry job');
    }
  }

  async function loadMemberships(tenantId: string) {
    setIsLoadingMembers(true);
    setMemberError(null);
    try {
      const list = await api.getMemberships(tenantId);
      setMemberships(list);
    } catch (err) {
      setMemberError(err instanceof Error ? err.message : 'Failed to load team members');
    } finally {
      setIsLoadingMembers(false);
    }
  }

  function openMembersModal() {
    if (!selectedTenant) return;
    setIsMembersOpen(true);
    setNewMemberEmail('');
    setNewMemberRole('COLLABORATOR');
    setMemberError(null);
    loadMemberships(selectedTenant.id);
  }

  async function handleAddMember(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !newMemberEmail.trim()) return;
    setIsAddingMember(true);
    setMemberError(null);
    try {
      const created = await api.addMembership(selectedTenant.id, newMemberEmail.trim(), newMemberRole);
      setMemberships(prev => [...prev, created]);
      setNewMemberEmail('');
    } catch (err) {
      setMemberError(err instanceof Error ? err.message : 'Failed to add member');
    } finally {
      setIsAddingMember(false);
    }
  }

  async function handleRemoveMember(membershipId: string) {
    if (!selectedTenant) return;
    try {
      await api.removeMembership(selectedTenant.id, membershipId);
      setMemberships(prev => prev.filter(m => m.id !== membershipId));
    } catch (err) {
      setMemberError(err instanceof Error ? err.message : 'Failed to remove member');
    }
  }

  // Filter documents by folder, status, and search query
  const filteredDocuments = documents.filter(doc => {
    // Recently Deleted / Trash bin view
    if (selectedFolderId === 'TRASH') {
      if (doc.status !== 'TRASH') return false;
    } else {
      // Exclude TRASH documents when viewing 'All Documents' or regular folders
      if (doc.status === 'TRASH') return false;

      // Folder filter
      if (selectedFolderId === 'UNFILED') {
        if (doc.folderId) return false;
      } else if (selectedFolderId !== 'ALL') {
        if (doc.folderId !== selectedFolderId) return false;
      }

      // Status tab filter
      if (statusFilter !== 'ALL') {
        if (doc.status !== statusFilter) return false;
      }
    }

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const nameMatch = doc.name.toLowerCase().includes(q);
      const descMatch = (doc.description || '').toLowerCase().includes(q);
      const folderObj = folders.find(f => f.id === doc.folderId);
      const folderMatch = folderObj ? folderObj.name.toLowerCase().includes(q) : false;
      const tagsMatch = (docTags[doc.id] || []).some(t => t.name.toLowerCase().includes(q));
      if (!nameMatch && !descMatch && !folderMatch && !tagsMatch) return false;
    }

    return true;
  });

  function getFileIcon(filename: string) {
    const ext = filename.split('.').pop()?.toLowerCase() || '';
    if (ext === 'pdf') {
      return (
        <span className="file-badge file-badge-pdf" title="Adobe PDF">
          PDF
        </span>
      );
    }
    if (ext === 'png') {
      return (
        <span className="file-badge file-badge-png" title="PNG Image">
          PNG
        </span>
      );
    }
    if (ext === 'txt') {
      return (
        <span className="file-badge file-badge-txt" title="Plain Text">
          TXT
        </span>
      );
    }
    return (
      <span className="file-badge file-badge-generic" title="Document">
        DOC
      </span>
    );
  }

  if (!userEmail) {
    return (
      <LandingPage
        healthStatus={healthStatus}
        theme={theme}
        onToggleTheme={toggleTheme}
        onLogin={(email: string) => {
          setCurrentUserEmail(email);
          setUserEmail(email);
        }}
      />
    );
  }

  return (
    <div className="app-layout">
      {/* Mobile Top Bar */}
      <header className="mobile-header">
        <button
          type="button"
          className="mobile-toggle-btn"
          onClick={() => setIsMobileNavOpen(!isMobileNavOpen)}
          aria-label="Toggle navigation menu"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
        <div className="mobile-brand">
          <div className="brand-logo-icon">CD</div>
          <span className="brand-logo-name">CloudDocs</span>
        </div>
        <button
          type="button"
          className="icon-action-btn"
          onClick={toggleTheme}
          aria-label={`Toggle to ${theme === 'light' ? 'dark' : 'light'} mode`}
          title={`Toggle to ${theme === 'light' ? 'dark' : 'light'} mode`}
        >
          {theme === 'light' ? '🌙' : '☀️'}
        </button>
      </header>

      {/* Main Sidebar */}
      <aside className={`app-sidebar ${isMobileNavOpen ? 'mobile-expanded' : ''}`}>
        <div className="sidebar-top">
          <div className="brand-header">
            <div className="brand-logo-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
              </svg>
            </div>
            <div className="brand-text">
              <span className="brand-logo-name">CloudDocs</span>
              <span className="brand-subtext">DOCUMENT CLOUD</span>
            </div>
            <button
              type="button"
              className="sidebar-close-btn"
              onClick={() => setIsMobileNavOpen(false)}
              aria-label="Close menu"
            >
              ✕
            </button>
          </div>

          {/* Workspace Switcher */}
          <div className="workspace-card">
            <label htmlFor="sidebar-tenant-select" className="workspace-label">
              CURRENT WORKSPACE
            </label>
            <div className="workspace-select-box">
              <select
                id="sidebar-tenant-select"
                className="workspace-select"
                value={selectedTenant?.id || ''}
                onChange={(e) => {
                  const found = tenants.find(t => t.id === e.target.value);
                  if (found) setSelectedTenant(found);
                }}
              >
                {tenants.map(t => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Sidebar Nav Items */}
          <nav className="sidebar-nav">
            <div className="nav-group">
              <span className="nav-group-title">DOCUMENTS</span>

              <button
                type="button"
                className={`sidebar-link ${selectedFolderId === 'ALL' ? 'active' : ''}`}
                onClick={() => { setSelectedFolderId('ALL'); setIsMobileNavOpen(false); }}
              >
                <span className="link-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                  </svg>
                </span>
                <span className="link-title">All Documents</span>
                <span className="link-pill">{documents.filter(d => d.status !== 'TRASH').length}</span>
              </button>

              <button
                type="button"
                className={`sidebar-link ${selectedFolderId === 'UNFILED' ? 'active' : ''}`}
                onClick={() => { setSelectedFolderId('UNFILED'); setIsMobileNavOpen(false); }}
              >
                <span className="link-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="22 12 16 12 14 15 10 15 8 12 2 12"></polyline>
                    <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"></path>
                  </svg>
                </span>
                <span className="link-title">Unfiled</span>
                <span className="link-pill">{documents.filter(d => d.status !== 'TRASH' && !d.folderId).length}</span>
              </button>

              <button
                type="button"
                className={`sidebar-link ${selectedFolderId === 'TRASH' ? 'active' : ''}`}
                onClick={() => { setSelectedFolderId('TRASH'); setIsMobileNavOpen(false); }}
              >
                <span className="link-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                </span>
                <span className="link-title">🗑️ Recently Deleted</span>
                <span className="link-pill">{documents.filter(d => d.status === 'TRASH').length}</span>
              </button>
            </div>

            {/* Folders Section */}
            <div className="nav-group">
              <div className="nav-group-header">
                <span className="nav-group-title">FOLDERS</span>
                <button
                  type="button"
                  className="add-folder-mini-btn"
                  onClick={() => {
                    setIsCreateFolderOpen(true);
                    setNewFolderName('');
                    setFolderError(null);
                  }}
                  title="Create new folder"
                >
                  + Add
                </button>
              </div>

              <div className="folders-container">
                {folders.length === 0 ? (
                  <span className="nav-empty-hint">No folders yet</span>
                ) : (
                  folders.map(folder => (
                    <button
                      key={folder.id}
                      type="button"
                      className={`sidebar-link ${selectedFolderId === folder.id ? 'active' : ''}`}
                      onClick={() => { setSelectedFolderId(folder.id); setIsMobileNavOpen(false); }}
                    >
                      <span className="link-icon">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                        </svg>
                      </span>
                      <span className="link-title" title={folder.name}>{folder.name}</span>
                      <span className="link-pill">
                        {documents.filter(d => d.status !== 'TRASH' && d.folderId === folder.id).length}
                      </span>
                    </button>
                  ))
                )}
              </div>
            </div>

            {/* Team Section */}
            <div className="nav-group">
              <span className="nav-group-title">ORGANIZATION</span>
              <button
                type="button"
                className="sidebar-link"
                onClick={() => { openMembersModal(); setIsMobileNavOpen(false); }}
              >
                <span className="link-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                  </svg>
                </span>
                <span className="link-title">Team Members</span>
                <span className="link-pill">{memberships.length || '•'}</span>
              </button>
            </div>
          </nav>
        </div>

        {/* User Card at bottom of sidebar */}
        <div className="sidebar-bottom">
          <div className="user-profile-badge">
            <div className="user-avatar-circle">{userEmail ? userEmail.charAt(0).toUpperCase() : 'U'}</div>
            <div className="user-info-text">
              <span className="user-name">{userEmail?.split('@')[0] || 'User'}</span>
              <span className="user-email-text" title={userEmail || ''}>{userEmail || ''}</span>
            </div>
            <button
              type="button"
              className="user-signout-mini-btn"
              onClick={handleSignOut}
              title="Sign Out"
              aria-label="Sign Out"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="app-body">
        {/* Top Header / Bar */}
        <header className="top-header">
          <div className="header-left">
            <div className="search-bar">
              <svg className="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                type="text"
                placeholder="Search documents by name, tag, or description..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="search-input"
              />
              {searchQuery && (
                <button
                  type="button"
                  className="search-clear-btn"
                  onClick={() => setSearchQuery('')}
                  title="Clear search query"
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          <div className="header-right">
            {/* Health Badge */}
            <div className="system-health-pill" title="AWS EC2 Spring Boot Server Status">
              <span className={`health-dot ${healthStatus.toLowerCase()}`} />
              <span className="health-label">EC2: {healthStatus}</span>
            </div>

            {/* User Profile Chip with Sign Out */}
            <div className="header-user-chip">
              <div className="user-avatar-circle small">{userEmail ? userEmail.charAt(0).toUpperCase() : 'U'}</div>
              <span className="header-user-email" title={userEmail || ''}>{userEmail}</span>
              <button
                type="button"
                className="btn-signout"
                onClick={handleSignOut}
                title="Sign out of CloudDocs"
              >
                Sign Out
              </button>
            </div>

            {/* Theme Toggle Button */}
            <button
              type="button"
              className="theme-btn"
              onClick={toggleTheme}
              aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
              title={`Switch to ${theme === 'light' ? 'dark' : 'light'} mode`}
            >
              {theme === 'light' ? (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                </svg>
              ) : (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="5"></circle>
                  <line x1="12" y1="1" x2="12" y2="3"></line>
                  <line x1="12" y1="21" x2="12" y2="23"></line>
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                  <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                  <line x1="1" y1="12" x2="3" y2="12"></line>
                  <line x1="21" y1="12" x2="23" y2="12"></line>
                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                  <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                </svg>
              )}
              <span className="theme-btn-text">{theme === 'light' ? 'Dark Mode' : 'Light Mode'}</span>
            </button>
          </div>
        </header>

        {/* Dashboard Main View */}
        <main className="dashboard-scroll">
          <div className="dashboard-container">
            {/* Page Header */}
            <div className="page-title-banner">
              <div className="title-left">
                <div className="breadcrumb-path">
                  <span>{selectedTenant?.name || 'Workspace'}</span>
                  <span className="breadcrumb-slash">/</span>
                  <span className="breadcrumb-target">
                    {selectedFolderId === 'ALL'
                      ? 'All Documents'
                      : selectedFolderId === 'UNFILED'
                      ? 'Unfiled'
                      : selectedFolderId === 'TRASH'
                      ? 'Recently Deleted'
                      : folders.find(f => f.id === selectedFolderId)?.name || 'Folder'}
                  </span>
                </div>
                <h1 className="page-heading">
                  {selectedFolderId === 'TRASH' ? 'Recently Deleted' : 'Document Repository'}
                </h1>
                <p className="page-subheading">
                  {selectedFolderId === 'TRASH'
                    ? 'Documents in the trash bin are retained for 30 days before permanent deletion from storage.'
                    : 'Organize, secure, and inspect asynchronous event-driven pipelines for multi-tenant documents.'}
                </p>
              </div>

              {selectedFolderId !== 'TRASH' && (
                <div className="title-right">
                  <button
                    type="button"
                    className="btn-upload"
                    onClick={openUploadModal}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                    Upload Document
                  </button>
                </div>
              )}
            </div>

            {/* Filter Tabs Bar (Odoo Style) */}
            {selectedFolderId === 'TRASH' ? (
              <div className="status-tabs-row">
                <div className="status-tabs-list">
                  <div className="trash-banner-info">
                    <span className="trash-banner-icon">🗑️</span>
                    <span><strong>Retention Policy:</strong> Items here are permanently purged after 30 days. You can restore them anytime or purge immediately.</span>
                  </div>
                </div>
                {searchQuery && (
                  <div className="search-match-pill">
                    Filtering for "<strong>{searchQuery}</strong>" ({filteredDocuments.length} results)
                  </div>
                )}
              </div>
            ) : (
              <div className="status-tabs-row">
                <div className="status-tabs-list">
                  <button
                    type="button"
                    className={`status-tab ${statusFilter === 'ALL' ? 'active' : ''}`}
                    onClick={() => setStatusFilter('ALL')}
                  >
                    All Documents <span className="tab-pill">{documents.filter(d => d.status !== 'TRASH').length}</span>
                  </button>
                  <button
                    type="button"
                    className={`status-tab ${statusFilter === 'ACTIVE' ? 'active' : ''}`}
                    onClick={() => setStatusFilter('ACTIVE')}
                  >
                    <span className="status-indicator-dot dot-active" />
                    Active <span className="tab-pill">{documents.filter(d => d.status === 'ACTIVE').length}</span>
                  </button>
                  <button
                    type="button"
                    className={`status-tab ${statusFilter === 'DRAFT' ? 'active' : ''}`}
                    onClick={() => setStatusFilter('DRAFT')}
                  >
                    <span className="status-indicator-dot dot-draft" />
                    Draft <span className="tab-pill">{documents.filter(d => d.status === 'DRAFT').length}</span>
                  </button>
                  <button
                    type="button"
                    className={`status-tab ${statusFilter === 'ARCHIVED' ? 'active' : ''}`}
                    onClick={() => setStatusFilter('ARCHIVED')}
                  >
                    <span className="status-indicator-dot dot-archived" />
                    Archived <span className="tab-pill">{documents.filter(d => d.status === 'ARCHIVED').length}</span>
                  </button>
                </div>

                {searchQuery && (
                  <div className="search-match-pill">
                    Filtering for "<strong>{searchQuery}</strong>" ({filteredDocuments.length} results)
                  </div>
                )}
              </div>
            )}

            {/* Documents Table Card */}
            <div className="dashboard-card">
              {isLoadingDocs ? (
                <div className="state-panel loading">
                  <div className="loading-spinner" />
                  <p>Loading document repository...</p>
                </div>
              ) : filteredDocuments.length === 0 ? (
                <div className="state-panel empty">
                  <div className="empty-icon-circle">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                    </svg>
                  </div>
                  <h3>{selectedFolderId === 'TRASH' ? 'Trash is empty' : 'No documents found'}</h3>
                  <p>
                    {selectedFolderId === 'TRASH'
                      ? 'No deleted documents in the trash bin.'
                      : searchQuery
                      ? `No documents match "${searchQuery}".`
                      : statusFilter !== 'ALL'
                      ? `No documents with status "${statusFilter}" in this folder.`
                      : 'Upload a document (.txt, .pdf, .png) to begin.'}
                  </p>
                  {searchQuery ? (
                    <button
                      type="button"
                      className="btn-clear-search"
                      onClick={() => setSearchQuery('')}
                    >
                      Reset Search Filter
                    </button>
                  ) : selectedFolderId !== 'TRASH' ? (
                    <button
                      type="button"
                      className="btn-upload"
                      onClick={openUploadModal}
                    >
                      + Upload Document
                    </button>
                  ) : null}
                </div>
              ) : (
                <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th style={{ width: '33%' }}>Document</th>
                        <th style={{ width: '13%' }}>Folder</th>
                        <th style={{ width: selectedFolderId === 'TRASH' ? '18%' : '12%' }}>
                          {selectedFolderId === 'TRASH' ? 'Retention Policy' : 'Status'}
                        </th>
                        <th style={{ width: selectedFolderId === 'TRASH' ? '14%' : '18%' }}>Tags</th>
                        <th style={{ width: '10%' }}>{selectedFolderId === 'TRASH' ? 'Deleted' : 'Created'}</th>
                        <th style={{ width: selectedFolderId === 'TRASH' ? '12%' : '14%', textAlign: 'right' }}>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredDocuments.map(doc => {
                        const folder = folders.find(f => f.id === doc.folderId);
                        return (
                          <tr key={doc.id} className="data-row">
                            {/* Document Details */}
                            <td className="cell-doc">
                              <div className="doc-item-group">
                                {getFileIcon(doc.name)}
                                <div className="doc-text-block">
                                  <span className="doc-primary-name">{doc.name}</span>
                                  {doc.description && (
                                    <span className="doc-secondary-desc">{doc.description}</span>
                                  )}
                                </div>
                              </div>
                            </td>

                            {/* Folder */}
                            <td>
                              {folder ? (
                                <span className="folder-chip">
                                  📁 {folder.name}
                                </span>
                              ) : (
                                <span className="folder-none">—</span>
                              )}
                            </td>

                            {/* Status or Retention */}
                            <td>
                              {doc.status === 'TRASH' ? (
                                <span className="retention-badge" title={`Trashed: ${doc.deletedAt ? new Date(doc.deletedAt).toLocaleString() : 'N/A'}`}>
                                  <span className="retention-icon">⏳</span>
                                  <span>Deletes in {getDaysRemaining(doc.deletedAt)} {getDaysRemaining(doc.deletedAt) === 1 ? 'day' : 'days'}</span>
                                </span>
                              ) : (
                                <span className={`status-badge status-${doc.status.toLowerCase()}`}>
                                  <span className="status-dot-mini" />
                                  {doc.status}
                                </span>
                              )}
                            </td>

                            {/* Tags */}
                            <td>
                              <div className="tags-flex">
                                {(docTags[doc.id] || []).map(tag => (
                                  <span key={tag.id} className="tag-token">
                                    #{tag.name}
                                    {doc.status !== 'TRASH' && (
                                      <button
                                        type="button"
                                        className="tag-close-btn"
                                        onClick={() => handleRemoveTag(doc.id, tag.id)}
                                        title={`Remove #${tag.name}`}
                                      >
                                        ×
                                      </button>
                                    )}
                                  </span>
                                ))}
                                {doc.status !== 'TRASH' && (
                                  <button
                                    type="button"
                                    className="tag-add-btn"
                                    onClick={() => {
                                      setActiveDocForTag(doc);
                                      setNewTagName('');
                                      setTagError(null);
                                    }}
                                    title="Add tag to document"
                                  >
                                    + Tag
                                  </button>
                                )}
                              </div>
                            </td>

                            {/* Date */}
                            <td className="cell-date">
                              {new Date(selectedFolderId === 'TRASH' && doc.deletedAt ? doc.deletedAt : doc.createdAt).toLocaleDateString(undefined, {
                                month: 'short',
                                day: 'numeric',
                                year: 'numeric',
                              })}
                            </td>

                            {/* Action Buttons */}
                            <td className="cell-actions">
                              {doc.status === 'TRASH' ? (
                                <div className="action-buttons-group">
                                  {doc.currentVersionId && (
                                    <button
                                      type="button"
                                      className="action-pill-btn view-btn"
                                      onClick={() => openPreview(doc)}
                                      title="Preview document in-app"
                                    >
                                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                      </svg>
                                      <span>View</span>
                                    </button>
                                  )}
                                  <button
                                    type="button"
                                    className="action-pill-btn restore-btn"
                                    onClick={() => handleRestore(doc.id)}
                                    title="Restore document to active status"
                                  >
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
                                      <path d="M3 3v5h5"></path>
                                    </svg>
                                    <span>Restore</span>
                                  </button>
                                  <button
                                    type="button"
                                    className="action-pill-btn delete-forever-btn"
                                    onClick={() => handlePermanentDelete(doc)}
                                    title="Permanently purge document and S3 binaries"
                                  >
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <polyline points="3 6 5 6 21 6"></polyline>
                                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                    </svg>
                                    <span>Delete Forever</span>
                                  </button>
                                </div>
                              ) : (
                                <div className="action-buttons-group">
                                  {doc.currentVersionId && (
                                    <button
                                      type="button"
                                      className="action-pill-btn view-btn"
                                      onClick={() => openPreview(doc)}
                                      title="Preview document in-app"
                                    >
                                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                      </svg>
                                      <span>View</span>
                                    </button>
                                  )}

                                  {doc.currentVersionId && (
                                    <button
                                      type="button"
                                      className="action-pill-btn download-btn"
                                      onClick={() => handleDownload(doc)}
                                      title="Download document payload"
                                    >
                                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                        <polyline points="7 10 12 15 17 10"></polyline>
                                        <line x1="12" y1="15" x2="12" y2="3"></line>
                                      </svg>
                                      <span>Download</span>
                                    </button>
                                  )}

                                  {doc.status === 'ARCHIVED' && (
                                    <button
                                      type="button"
                                      className="action-pill-btn unarchive-btn"
                                      onClick={() => handleUnarchive(doc.id)}
                                      title="Restore document to active status"
                                    >
                                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <polyline points="9 14 4 9 9 4"></polyline>
                                        <path d="M20 20v-7a4 4 0 0 0-4-4H4"></path>
                                      </svg>
                                      <span>Unarchive</span>
                                    </button>
                                  )}

                                  <button
                                    type="button"
                                    className="action-pill-btn pipeline-btn"
                                    onClick={() => openJobsDrawer(doc)}
                                    title="Inspect async SQS processing pipeline"
                                  >
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                                    </svg>
                                    <span>Jobs</span>
                                  </button>

                                  {doc.status !== 'ARCHIVED' && (
                                    <button
                                      type="button"
                                      className="action-pill-btn archive-btn"
                                      onClick={() => handleArchive(doc.id)}
                                      title="Archive document"
                                    >
                                      <span>Archive</span>
                                    </button>
                                  )}

                                  <button
                                    type="button"
                                    className="action-pill-btn delete-btn"
                                    onClick={() => handleTrash(doc.id)}
                                    title="Move to Recently Deleted"
                                  >
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                      <polyline points="3 6 5 6 21 6"></polyline>
                                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                    </svg>
                                    <span>Delete</span>
                                  </button>
                                </div>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
          <Footer />
        </main>
      </div>

      {/* ======================================================================
          MODALS & DRAWERS
          ====================================================================== */}

      {/* New Folder Modal */}
      {isCreateFolderOpen && (
        <div className="modal-backdrop" onClick={() => setIsCreateFolderOpen(false)}>
          <div className="modal-card modal-compact" onClick={e => e.stopPropagation()}>
            <div className="modal-top">
              <h3>Create New Folder</h3>
              <button className="modal-dismiss" onClick={() => setIsCreateFolderOpen(false)}>✕</button>
            </div>
            <p className="modal-caption">Organize workspace documents under a dedicated category.</p>

            <form onSubmit={handleCreateFolder}>
              <div className="form-field">
                <label htmlFor="new-folder-name">Folder Name</label>
                <input
                  id="new-folder-name"
                  type="text"
                  placeholder="e.g. Legal, Invoices, Marketing"
                  value={newFolderName}
                  onChange={e => setNewFolderName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {folderError && <div className="error-alert">{folderError}</div>}

              <div className="modal-button-row">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setIsCreateFolderOpen(false)}
                  disabled={isCreatingFolder}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isCreatingFolder || !newFolderName.trim()}
                >
                  {isCreatingFolder ? 'Creating...' : 'Create Folder'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add / Attach Tag Modal */}
      {activeDocForTag && (
        <div className="modal-backdrop" onClick={() => setActiveDocForTag(null)}>
          <div className="modal-card modal-compact" onClick={e => e.stopPropagation()}>
            <div className="modal-top">
              <h3>Attach Tag: {activeDocForTag.name}</h3>
              <button className="modal-dismiss" onClick={() => setActiveDocForTag(null)}>✕</button>
            </div>
            <p className="modal-caption">Add classification tags to filter and search this document.</p>

            <form onSubmit={handleAddTag}>
              <div className="form-field">
                <label htmlFor="new-tag-name">Tag Name</label>
                <input
                  id="new-tag-name"
                  type="text"
                  placeholder="e.g. Q4-Report, Confidential, Review"
                  value={newTagName}
                  onChange={e => setNewTagName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {tagError && <div className="error-alert">{tagError}</div>}

              {knownTags.length > 0 && (
                <div className="tag-suggestions-box">
                  <span className="suggestions-label">EXISTING WORKSPACE TAGS</span>
                  <div className="suggestions-list">
                    {knownTags
                      .filter(t => !(docTags[activeDocForTag.id] || []).some(dt => dt.id === t.id))
                      .map(t => (
                        <button
                          key={t.id}
                          type="button"
                          className="suggestion-token"
                          onClick={() => handleAttachExistingTag(activeDocForTag.id, t)}
                        >
                          + #{t.name}
                        </button>
                      ))}
                  </div>
                </div>
              )}

              <div className="modal-button-row">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setActiveDocForTag(null)}
                  disabled={isSubmittingTag}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isSubmittingTag || !newTagName.trim()}
                >
                  {isSubmittingTag ? 'Attaching...' : 'Add Tag'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Upload Document Modal */}
      {isUploadOpen && (
        <div className="modal-backdrop" onClick={() => !isUploading && setIsUploadOpen(false)}>
          <div className="modal-card modal-medium" onClick={e => e.stopPropagation()}>
            <div className="modal-top">
              <h3>Upload Document</h3>
              <button
                className="modal-dismiss"
                onClick={() => !isUploading && setIsUploadOpen(false)}
                disabled={isUploading}
              >
                ✕
              </button>
            </div>
            <p className="modal-caption">
              Upload files into CloudDocs with direct-to-storage validation and async SQS processing.
            </p>

            <form onSubmit={handleUpload}>
              <div className="form-field">
                <label htmlFor="upload-doc-name">Document Title *</label>
                <input
                  id="upload-doc-name"
                  type="text"
                  placeholder="e.g. Annual Financial Statement"
                  value={docName}
                  onChange={e => setDocName(e.target.value)}
                  required
                  disabled={isUploading}
                />
              </div>

              <div className="form-field">
                <label htmlFor="upload-doc-desc">Description (Optional)</label>
                <input
                  id="upload-doc-desc"
                  type="text"
                  placeholder="Summary of document purpose and contents"
                  value={docDescription}
                  onChange={e => setDocDescription(e.target.value)}
                  disabled={isUploading}
                />
              </div>

              <div className="form-field">
                <label htmlFor="upload-folder-select">Assign to Folder</label>
                <select
                  id="upload-folder-select"
                  value={uploadFolderId}
                  onChange={e => setUploadFolderId(e.target.value)}
                  disabled={isUploading}
                >
                  <option value="">(No folder — Unfiled)</option>
                  {folders.map(f => (
                    <option key={f.id} value={f.id}>📁 {f.name}</option>
                  ))}
                </select>
              </div>

              {/* File Dropzone */}
              <div className="form-field">
                <label>File Payload *</label>
                <div className={`dropzone-box ${selectedFile ? 'has-file' : ''}`}>
                  <input
                    type="file"
                    id="file-input"
                    className="file-hidden-input"
                    accept=".txt,.pdf,.png"
                    onChange={handleFileSelect}
                    disabled={isUploading}
                  />
                  <label htmlFor="file-input" className="dropzone-label">
                    <div className="dropzone-icon">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="17 8 12 3 7 8"></polyline>
                        <line x1="12" y1="3" x2="12" y2="15"></line>
                      </svg>
                    </div>
                    {selectedFile ? (
                      <div className="selected-file-details">
                        <span className="file-name">{selectedFile.name}</span>
                        <span className="file-size">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
                        <span className="file-change-hint">Click to replace file</span>
                      </div>
                    ) : (
                      <div className="dropzone-text-group">
                        <span className="dropzone-main-text">Click to browse file from device</span>
                        <span className="dropzone-sub-text">Supported formats: .txt, .pdf, .png (Max 10 MB)</span>
                      </div>
                    )}
                  </label>
                </div>
              </div>

              {uploadStatus && (
                <div className="upload-status-indicator">
                  <div className="mini-spin" />
                  <span>{uploadStatus}</span>
                </div>
              )}

              <div className="modal-button-row">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setIsUploadOpen(false)}
                  disabled={isUploading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isUploading || !selectedFile || !docName.trim()}
                >
                  {isUploading ? 'Uploading...' : 'Start Upload'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Team Members Modal */}
      {isMembersOpen && selectedTenant && (
        <div className="modal-backdrop" onClick={() => setIsMembersOpen(false)}>
          <div className="modal-card modal-wide" onClick={e => e.stopPropagation()}>
            <div className="modal-top">
              <h3>Workspace Team: {selectedTenant.name}</h3>
              <button className="modal-dismiss" onClick={() => setIsMembersOpen(false)}>✕</button>
            </div>
            <p className="modal-caption">
              Control member permissions and multi-tenant access boundaries for this workspace.
            </p>

            {memberError && <div className="error-alert">{memberError}</div>}

            {isLoadingMembers ? (
              <div className="loading-state-mini">Loading workspace members...</div>
            ) : memberships.length === 0 ? (
              <div className="empty-state-mini">No members found in this workspace.</div>
            ) : (
              <div className="members-scroll-list">
                {memberships.map(m => {
                  const email = m.userEmail || `User (${m.userId.substring(0, 8)}...)`;
                  const isCurrentAdmin = email === 'admin@clouddocs.com';
                  return (
                    <div key={m.id} className="member-row-item">
                      <div className="member-avatar-box">
                        {email.charAt(0).toUpperCase()}
                      </div>
                      <div className="member-meta-block">
                        <span className="member-email-title">{email}</span>
                        <span className="member-id-subtitle">ID: {m.id.substring(0, 8)}...</span>
                      </div>
                      <div className="member-role-group">
                        <span className={`role-pill role-${m.role.toLowerCase().replace('tenant_', '')}`}>
                          {m.role === 'TENANT_ADMIN' ? 'Admin' : m.role === 'COLLABORATOR' ? 'Collaborator' : 'Viewer'}
                        </span>
                        <button
                          type="button"
                          className="btn-member-remove"
                          disabled={isCurrentAdmin}
                          onClick={() => handleRemoveMember(m.id)}
                          title={isCurrentAdmin ? 'Cannot remove workspace admin' : 'Remove member'}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Add Member Section */}
            <div className="add-member-footer-panel">
              <h4>Add New Team Member</h4>
              <form onSubmit={handleAddMember} className="add-member-grid">
                <input
                  type="email"
                  placeholder="member@company.com"
                  value={newMemberEmail}
                  onChange={e => setNewMemberEmail(e.target.value)}
                  required
                  disabled={isAddingMember}
                  className="add-member-input"
                />
                <select
                  value={newMemberRole}
                  onChange={e => setNewMemberRole(e.target.value as any)}
                  disabled={isAddingMember}
                  className="add-member-select"
                >
                  <option value="COLLABORATOR">COLLABORATOR</option>
                  <option value="VIEWER">VIEWER</option>
                  <option value="TENANT_ADMIN">TENANT_ADMIN</option>
                </select>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isAddingMember || !newMemberEmail.trim()}
                >
                  {isAddingMember ? 'Adding...' : 'Add'}
                </button>
              </form>
            </div>

            <div className="modal-button-row">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setIsMembersOpen(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Processing Pipeline Jobs Drawer */}
      {activeDocForJobs && (
        <div className="drawer-backdrop" onClick={() => setActiveDocForJobs(null)}>
          <div className="drawer-surface" onClick={e => e.stopPropagation()}>
            <div className="drawer-top-bar">
              <div className="drawer-heading-group">
                <h3>Pipeline Execution Jobs</h3>
                <span className="drawer-target-doc">Document: <strong>{activeDocForJobs.name}</strong></span>
              </div>
              <button className="drawer-dismiss-btn" onClick={() => setActiveDocForJobs(null)}>✕</button>
            </div>

            <div className="drawer-scroll-body">
              {isLoadingJobs ? (
                <div className="drawer-loading">
                  <div className="loading-spinner" />
                  <p>Polling SQS event processing jobs...</p>
                </div>
              ) : jobs.length === 0 ? (
                <div className="drawer-empty">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                  </svg>
                  <h4>No asynchronous jobs</h4>
                  <p>No processing jobs registered for this document yet.</p>
                </div>
              ) : (
                <div className="jobs-stack">
                  {jobs.map(job => (
                    <div key={job.id} className="job-record-card">
                      <div className="job-record-header">
                        <span className={`status-pill status-${job.status.toLowerCase()}`}>
                          <span className="pill-dot" />
                          {job.status}
                        </span>
                        <span className="job-attempts-pill">Retries: {job.retryCount}</span>
                      </div>
                      <div className="job-meta-row">
                        <span className="job-id-code">ID: {job.id}</span>
                        <span className="job-date">
                          {new Date(job.updatedAt || job.createdAt).toLocaleTimeString()}
                        </span>
                      </div>
                      {job.errorMessage && (
                        <div className="job-error-box">{job.errorMessage}</div>
                      )}
                      {job.status === 'FAILED' && (
                        <button
                          type="button"
                          className="btn-retry-job"
                          onClick={() => handleRetryJob(job.id)}
                        >
                          Retry SQS Task
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* In-App Document Viewer Modal */}
      {previewDoc && (
        <div className="modal-backdrop" onClick={closePreview}>
          <div
            className="modal-box modal-viewer-box"
            onClick={e => e.stopPropagation()}
          >
            <div className="viewer-header">
              <div className="viewer-title-block">
                <span className="viewer-file-icon">{getFileIcon(previewDoc.name)}</span>
                <div className="viewer-headings">
                  <h3 className="viewer-title">{previewDoc.name}</h3>
                  <span className="viewer-filename">
                    {previewData ? previewData.contentType : 'Loading preview...'}
                  </span>
                </div>
              </div>
              <div className="viewer-actions">
                {previewDoc.currentVersionId && (
                  <button
                    type="button"
                    className="action-pill-btn download-btn"
                    onClick={() => handleDownload(previewDoc)}
                    title="Download document payload"
                  >
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="7 10 12 15 17 10"></polyline>
                      <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    <span>Download</span>
                  </button>
                )}
                <button
                  type="button"
                  className="modal-close-btn"
                  onClick={closePreview}
                  aria-label="Close document preview"
                >
                  ✕
                </button>
              </div>
            </div>

            <div className="viewer-content">
              {isLoadingPreview ? (
                <div className="state-panel loading">
                  <div className="loading-spinner" />
                  <p>Loading document content...</p>
                </div>
              ) : previewError ? (
                <div className="state-panel empty">
                  <p className="error-text">{previewError}</p>
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={() => openPreview(previewDoc)}
                  >
                    Retry
                  </button>
                </div>
              ) : previewData ? (
                <>
                  {previewData.contentType.includes('pdf') || previewDoc.name.toLowerCase().endsWith('.pdf') ? (
                    <iframe
                      src={previewData.url}
                      title={previewDoc.name}
                      className="viewer-iframe"
                    />
                  ) : previewData.contentType.startsWith('image/') || previewDoc.name.toLowerCase().endsWith('.png') ? (
                    <div className="viewer-image-wrap">
                      <img
                        src={previewData.url}
                        alt={previewDoc.name}
                        className="viewer-image"
                      />
                    </div>
                  ) : previewData.textContent !== undefined || previewData.contentType.includes('text') || previewDoc.name.toLowerCase().endsWith('.txt') ? (
                    <div className="viewer-text-wrap">
                      <pre className="viewer-pre">
                        <code>{previewData.textContent || ''}</code>
                      </pre>
                    </div>
                  ) : (
                    <div className="state-panel empty">
                      <p>Inline preview is not supported for this file type.</p>
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={() => handleDownload(previewDoc)}
                      >
                        Download File
                      </button>
                    </div>
                  )}
                </>
              ) : null}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
