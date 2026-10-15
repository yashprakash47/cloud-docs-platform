import React, { useState, useEffect } from 'react';
import {
  api,
  Tenant,
  DocumentItem,
  ProcessingJob,
  Folder,
  Tag,
  getExactMimeType,
  ALLOWED_MIME_TYPES,
  AllowedMimeType,
} from '../services/api';
import './app.css';

export function App() {
  const [healthStatus, setHealthStatus] = useState<string>('CHECKING');
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState<boolean>(false);

  // Folders state
  const [folders, setFolders] = useState<Folder[]>([]);
  const [selectedFolderId, setSelectedFolderId] = useState<string | 'ALL' | 'UNFILED'>('ALL');
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState<boolean>(false);
  const [newFolderName, setNewFolderName] = useState<string>('');
  const [isCreatingFolder, setIsCreatingFolder] = useState<boolean>(false);
  const [folderError, setFolderError] = useState<string | null>(null);

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

  useEffect(() => {
    api.getHealth()
      .then(res => setHealthStatus(res.status))
      .catch(() => setHealthStatus('DOWN'));

    api.getTenants()
      .then(data => {
        setTenants(data);
        if (data.length > 0) setSelectedTenant(data[0]);
      })
      .catch(err => console.error('Failed to load workspaces:', err));
  }, []);

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
        const map = new Map<string, Tag>();
        prev.forEach(t => map.set(t.id, t));
        allFoundTags.forEach(t => map.set(t.id, t));
        return Array.from(map.values());
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
      setNewFolderName('');
      setIsCreateFolderOpen(false);
      await loadFolders(selectedTenant.id);
      setSelectedFolderId(created.id);
    } catch (err) {
      setFolderError(err instanceof Error ? err.message : 'Failed to create folder');
    } finally {
      setIsCreatingFolder(false);
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files ? e.target.files[0] : null;
    if (!file) {
      setSelectedFile(null);
      return;
    }
    const mime = getExactMimeType(file);
    if (!ALLOWED_MIME_TYPES.includes(mime as AllowedMimeType)) {
      setUploadStatus('Invalid file type. Allowed types: text/plain (.txt), application/pdf (.pdf), and image/png (.png).');
      setSelectedFile(null);
      e.target.value = '';
      return;
    }
    setUploadStatus(null);
    setSelectedFile(file);
  }

  function openUploadModal() {
    setUploadFolderId(selectedFolderId !== 'ALL' && selectedFolderId !== 'UNFILED' ? selectedFolderId : '');
    setIsUploadOpen(true);
  }

  async function handleCreateAndUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !selectedFile || !docName.trim()) return;

    const mime = getExactMimeType(selectedFile);
    if (!ALLOWED_MIME_TYPES.includes(mime as AllowedMimeType)) {
      setUploadStatus('Invalid file type. Allowed types: text/plain (.txt), application/pdf (.pdf), and image/png (.png).');
      return;
    }

    setIsUploading(true);
    setUploadStatus('Creating document record...');
    try {
      const folderParam = uploadFolderId ? uploadFolderId : null;
      const doc = await api.createDocument(selectedTenant.id, docName.trim(), docDescription.trim(), folderParam);
      setUploadStatus('Uploading file to S3...');
      await api.uploadDocumentFile(selectedTenant.id, doc.id, selectedFile);

      setUploadStatus('Upload complete!');
      setDocName('');
      setDocDescription('');
      setUploadFolderId('');
      setSelectedFile(null);
      setTimeout(() => {
        setIsUploadOpen(false);
        setUploadStatus(null);
      }, 800);

      await loadDocuments(selectedTenant.id);
    } catch (err) {
      setUploadStatus(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  }

  async function handleAddTag(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedTenant || !activeDocForTag || !newTagName.trim()) return;
    setIsSubmittingTag(true);
    setTagError(null);
    const tagName = newTagName.trim();
    try {
      let tag = knownTags.find(t => t.name.toLowerCase() === tagName.toLowerCase());
      if (!tag) {
        tag = await api.createTag(selectedTenant.id, tagName);
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

  async function handleArchive(docId: string) {
    if (!selectedTenant) return;
    try {
      await api.archiveDocument(selectedTenant.id, docId);
      await loadDocuments(selectedTenant.id);
    } catch (err) {
      alert('Failed to archive document');
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

  const filteredDocuments = documents.filter(doc => {
    if (selectedFolderId === 'ALL') return true;
    if (selectedFolderId === 'UNFILED') return !doc.folderId;
    return doc.folderId === selectedFolderId;
  });

  return (
    <div className="layout">
      <header className="navbar">
        <div className="brand">
          <div className="logo-icon">CD</div>
          <span className="brand-name">CloudDocs Platform</span>
        </div>

        <div className="nav-controls">
          <div className="tenant-picker">
            <label htmlFor="tenant-select">Workspace:</label>
            <select
              id="tenant-select"
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

          <div className="health-badge">
            <span className={`status-dot ${healthStatus.toLowerCase()}`} />
            <span>EC2 Backend: {healthStatus}</span>
          </div>

          <div className="user-profile">
            <span className="user-avatar">A</span>
            <span className="user-email">admin@clouddocs.com</span>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="page-header">
          <div>
            <h1>Document Repository</h1>
            <p className="subtitle">
              Manage documents, organize by folders and tags, and track asynchronous SQS event processing.
            </p>
          </div>
          <button className="primary-btn" onClick={openUploadModal}>
            + Upload Document
          </button>
        </div>

        <div className="content-grid">
          {/* Folders Sidebar */}
          <aside className="folders-sidebar">
            <div className="sidebar-header">
              <h3>Folders</h3>
              <button
                type="button"
                className="folder-btn-new"
                onClick={() => {
                  setIsCreateFolderOpen(true);
                  setNewFolderName('');
                  setFolderError(null);
                }}
                title="Create New Folder"
              >
                + New
              </button>
            </div>

            <div className="folders-list">
              <button
                type="button"
                className={`folder-item ${selectedFolderId === 'ALL' ? 'active' : ''}`}
                onClick={() => setSelectedFolderId('ALL')}
              >
                <span className="folder-name-wrap">📁 All Documents</span>
                <span className="folder-count">{documents.length}</span>
              </button>

              <button
                type="button"
                className={`folder-item ${selectedFolderId === 'UNFILED' ? 'active' : ''}`}
                onClick={() => setSelectedFolderId('UNFILED')}
              >
                <span className="folder-name-wrap">📄 Unfiled</span>
                <span className="folder-count">{documents.filter(d => !d.folderId).length}</span>
              </button>

              {folders.map(folder => (
                <button
                  key={folder.id}
                  type="button"
                  className={`folder-item ${selectedFolderId === folder.id ? 'active' : ''}`}
                  onClick={() => setSelectedFolderId(folder.id)}
                >
                  <span className="folder-name-wrap" title={folder.name}>
                    📁 {folder.name}
                  </span>
                  <span className="folder-count">
                    {documents.filter(d => d.folderId === folder.id).length}
                  </span>
                </button>
              ))}
            </div>
          </aside>

          {/* Document Table */}
          <div className="repo-content">
            <section className="table-container">
              {isLoadingDocs ? (
                <div className="loading-state">Loading documents...</div>
              ) : filteredDocuments.length === 0 ? (
                <div className="empty-state">
                  <p>
                    {selectedFolderId === 'ALL'
                      ? 'No documents found in this workspace.'
                      : 'No documents found in this folder.'}
                  </p>
                  <button className="secondary-btn" onClick={openUploadModal}>
                    Upload a document
                  </button>
                </div>
              ) : (
                <table className="docs-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Description</th>
                      <th>Status</th>
                      <th>Tags</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredDocuments.map(doc => (
                      <tr key={doc.id}>
                        <td className="doc-name">
                          <strong>{doc.name}</strong>
                          {doc.folderId && folders.find(f => f.id === doc.folderId) && (
                            <span className="doc-folder-tag">
                              📁 {folders.find(f => f.id === doc.folderId)?.name}
                            </span>
                          )}
                        </td>
                        <td>{doc.description || '—'}</td>
                        <td>
                          <span className={`badge badge-${doc.status.toLowerCase()}`}>
                            {doc.status}
                          </span>
                        </td>
                        <td className="tags-cell">
                          <div className="tags-wrapper">
                            {(docTags[doc.id] || []).map(tag => (
                              <span key={tag.id} className="tag-chip">
                                #{tag.name}
                                <button
                                  type="button"
                                  className="tag-remove-btn"
                                  onClick={() => handleRemoveTag(doc.id, tag.id)}
                                  title={`Remove ${tag.name}`}
                                >
                                  ×
                                </button>
                              </span>
                            ))}
                            <button
                              type="button"
                              className="add-tag-btn"
                              onClick={() => {
                                setActiveDocForTag(doc);
                                setNewTagName('');
                                setTagError(null);
                              }}
                              title="Add tag"
                            >
                              + Tag
                            </button>
                          </div>
                        </td>
                        <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                        <td className="actions-cell">
                          {doc.currentVersionId && (
                            <button
                              className="text-btn"
                              onClick={() => handleDownload(doc)}
                            >
                              Download
                            </button>
                          )}
                          <button
                            className="text-btn"
                            onClick={() => openJobsDrawer(doc)}
                          >
                            Pipeline Jobs
                          </button>
                          {doc.status !== 'ARCHIVED' && (
                            <button
                              className="text-btn text-danger"
                              onClick={() => handleArchive(doc.id)}
                            >
                              Archive
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </div>
        </div>
      </main>

      {/* Create Folder Modal */}
      {isCreateFolderOpen && (
        <div className="modal-backdrop" onClick={() => setIsCreateFolderOpen(false)}>
          <div className="modal-card modal-small" onClick={e => e.stopPropagation()}>
            <h2>Create Folder</h2>
            <p className="modal-desc">Organize workspace documents into folders.</p>

            <form onSubmit={handleCreateFolder}>
              <div className="form-group">
                <label>Folder Name *</label>
                <input
                  type="text"
                  placeholder="e.g. Invoices, Contracts"
                  value={newFolderName}
                  onChange={e => setNewFolderName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {folderError && <div className="form-error">{folderError}</div>}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-btn"
                  onClick={() => setIsCreateFolderOpen(false)}
                  disabled={isCreatingFolder}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="primary-btn"
                  disabled={isCreatingFolder || !newFolderName.trim()}
                >
                  {isCreatingFolder ? 'Creating...' : 'Create Folder'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Tag Modal */}
      {activeDocForTag && (
        <div className="modal-backdrop" onClick={() => setActiveDocForTag(null)}>
          <div className="modal-card modal-small" onClick={e => e.stopPropagation()}>
            <h2>Add Tag</h2>
            <p className="modal-desc">
              Add a tag to <strong>{activeDocForTag.name}</strong>
            </p>

            <form onSubmit={handleAddTag}>
              <div className="form-group">
                <label>Tag Name *</label>
                <input
                  type="text"
                  placeholder="e.g. Architecture, Q3, Legal"
                  value={newTagName}
                  onChange={e => setNewTagName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {knownTags.filter(t => !(docTags[activeDocForTag.id] || []).some(dt => dt.id === t.id)).length > 0 && (
                <div className="tag-suggestions">
                  <span className="tag-suggestions-title">Existing workspace tags:</span>
                  <div className="suggestion-chips">
                    {knownTags
                      .filter(t => !(docTags[activeDocForTag.id] || []).some(dt => dt.id === t.id))
                      .map(t => (
                        <button
                          key={t.id}
                          type="button"
                          className="suggestion-chip"
                          onClick={() => handleAttachExistingTag(activeDocForTag.id, t)}
                        >
                          + {t.name}
                        </button>
                      ))}
                  </div>
                </div>
              )}

              {tagError && <div className="form-error">{tagError}</div>}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-btn"
                  onClick={() => setActiveDocForTag(null)}
                  disabled={isSubmittingTag}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="primary-btn"
                  disabled={isSubmittingTag || !newTagName.trim()}
                >
                  {isSubmittingTag ? 'Adding...' : 'Add Tag'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Upload Modal */}
      {isUploadOpen && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <h2>Upload Document</h2>
            <p className="modal-desc">
              Files are streamed directly to AWS S3 and trigger a Lambda notification.
            </p>

            <form onSubmit={handleCreateAndUpload}>
              <div className="form-group">
                <label>Document Title *</label>
                <input
                  type="text"
                  placeholder="e.g. Q3 Architecture Overview"
                  value={docName}
                  onChange={e => setDocName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Folder (Optional)</label>
                <select
                  value={uploadFolderId}
                  onChange={e => setUploadFolderId(e.target.value)}
                >
                  <option value="">None (Unfiled)</option>
                  {folders.map(f => (
                    <option key={f.id} value={f.id}>📁 {f.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Description</label>
                <input
                  type="text"
                  placeholder="Optional details or tags"
                  value={docDescription}
                  onChange={e => setDocDescription(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>File *</label>
                <input
                  type="file"
                  accept=".txt,.pdf,.png,text/plain,application/pdf,image/png"
                  onChange={handleFileChange}
                  required
                />
                <small className="form-hint" style={{ display: 'block', marginTop: '4px', fontSize: '12px', color: '#6b7280' }}>
                  Allowed types: text/plain (.txt), application/pdf (.pdf), image/png (.png)
                </small>
              </div>

              {uploadStatus && (
                <div className="upload-progress">{uploadStatus}</div>
              )}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-btn"
                  onClick={() => setIsUploadOpen(false)}
                  disabled={isUploading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="primary-btn"
                  disabled={isUploading || !selectedFile || !docName.trim()}
                >
                  {isUploading ? 'Uploading...' : 'Start Upload'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Async Processing Drawer */}
      {activeDocForJobs && (
        <div className="drawer-backdrop" onClick={() => setActiveDocForJobs(null)}>
          <div className="drawer-card" onClick={e => e.stopPropagation()}>
            <div className="drawer-header">
              <h3>Processing Pipeline Jobs</h3>
              <button className="close-btn" onClick={() => setActiveDocForJobs(null)}>✕</button>
            </div>
            <p className="drawer-sub">
              Document: <strong>{activeDocForJobs.name}</strong>
            </p>

            {isLoadingJobs ? (
              <p>Fetching jobs from SQS / worker store...</p>
            ) : jobs.length === 0 ? (
              <p className="empty-notice">No asynchronous jobs found for this document yet.</p>
            ) : (
              <div className="jobs-list">
                {jobs.map(job => (
                  <div key={job.id} className="job-card">
                    <div className="job-top">
                      <span className={`badge badge-${job.status.toLowerCase()}`}>
                        {job.status}
                      </span>
                      <span className="job-attempts">Attempts: {job.retryCount}</span>
                    </div>
                    <div className="job-id">Job ID: {job.id}</div>
                    {job.errorMessage && (
                      <div className="job-error">{job.errorMessage}</div>
                    )}
                    {job.status === 'FAILED' && (
                      <button
                        className="retry-btn"
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
      )}
    </div>
  );
}
