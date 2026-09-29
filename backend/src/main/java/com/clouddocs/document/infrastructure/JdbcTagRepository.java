package com.clouddocs.document.infrastructure;
import com.clouddocs.document.domain.Tag; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Repository; import java.sql.Timestamp; import java.util.*;
@Repository public class JdbcTagRepository { private final JdbcTemplate jdbc; public JdbcTagRepository(JdbcTemplate j){jdbc=j;}
 public Tag insert(UUID tenant,String name){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO tags(id,tenant_id,name) VALUES (?,?,?)",id,tenant,name);return find(id,tenant);}
 public Tag find(UUID id,UUID tenant){return jdbc.query("SELECT * FROM tags WHERE id=? AND tenant_id=?",this::map,id,tenant).stream().findFirst().orElseThrow(()->new com.clouddocs.operations.api.ResourceNotFoundException("Tag not found in this tenant"));}
 public void attach(UUID tenant,UUID doc,UUID tag){jdbc.update("INSERT INTO document_tags(tenant_id,document_id,tag_id) VALUES (?,?,?)",tenant,doc,tag);}
 public void remove(UUID tenant,UUID doc,UUID tag){jdbc.update("DELETE FROM document_tags WHERE tenant_id=? AND document_id=? AND tag_id=?",tenant,doc,tag);}
 public List<Tag> forDocument(UUID tenant,UUID doc){return jdbc.query("SELECT t.* FROM tags t JOIN document_tags dt ON dt.tag_id=t.id AND dt.tenant_id=t.tenant_id WHERE dt.tenant_id=? AND dt.document_id=? ORDER BY t.name",this::map,tenant,doc);}
 private Tag map(java.sql.ResultSet r,int n)throws java.sql.SQLException{Timestamp c=r.getTimestamp("created_at"),u=r.getTimestamp("updated_at");return new Tag(r.getObject("id",UUID.class),r.getObject("tenant_id",UUID.class),r.getString("name"),c.toInstant(),u.toInstant());}
}
