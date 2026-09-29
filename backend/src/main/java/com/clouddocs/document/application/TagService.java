package com.clouddocs.document.application;
import com.clouddocs.audit.application.*; import com.clouddocs.audit.domain.AuditAction; import com.clouddocs.document.domain.*; import com.clouddocs.document.infrastructure.JdbcTagRepository; import com.clouddocs.identity.domain.User; import com.clouddocs.tenancy.application.TenantAuthorizationService; import org.springframework.stereotype.Service; import java.util.*;
@Service public class TagService { private final JdbcTagRepository repo; private final DocumentService docs; private final TenantAuthorizationService auth; private final AuditService audit; public TagService(JdbcTagRepository r,DocumentService d,TenantAuthorizationService a,AuditService s){repo=r;docs=d;auth=a;audit=s;}
 public Tag create(User u,UUID tenant,String name){auth.requireWriteAccess(u,tenant);Tag t=repo.insert(tenant,name.trim());return t;}
 public List<Tag> list(User u,UUID tenant,UUID doc){auth.requireMembership(u,tenant);docs.require(tenant,doc);return repo.forDocument(tenant,doc);}
 public void attach(User u,UUID tenant,UUID doc,UUID tag){auth.requireWriteAccess(u,tenant);docs.require(tenant,doc);repo.find(tag,tenant);repo.attach(tenant,doc,tag);audit.record(tenant,u.id(),AuditAction.TAG_ATTACHED,"DOCUMENT_TAG",doc);}
 public void remove(User u,UUID tenant,UUID doc,UUID tag){auth.requireWriteAccess(u,tenant);docs.require(tenant,doc);repo.find(tag,tenant);repo.remove(tenant,doc,tag);audit.record(tenant,u.id(),AuditAction.TAG_REMOVED,"DOCUMENT_TAG",doc);}
}
