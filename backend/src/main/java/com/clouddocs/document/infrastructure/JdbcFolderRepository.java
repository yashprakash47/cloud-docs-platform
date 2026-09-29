package com.clouddocs.document.infrastructure;
import com.clouddocs.document.domain.Folder;
import com.clouddocs.operations.api.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp; import java.time.Instant; import java.util.*;
@Repository
public class JdbcFolderRepository {
 private final JdbcTemplate jdbc; public JdbcFolderRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Folder insert(UUID tenant, String name, UUID parent){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO folders(id,tenant_id,parent_id,name) VALUES (?,?,?,?)",id,tenant,parent,name);return find(id,tenant);}
 public Folder find(UUID id,UUID tenant){return jdbc.query("SELECT * FROM folders WHERE id=? AND tenant_id=?",this::map,id,tenant).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Folder not found in this tenant"));}
 public List<Folder> list(UUID tenant,UUID parent){return parent==null?jdbc.query("SELECT * FROM folders WHERE tenant_id=? AND parent_id IS NULL ORDER BY name",this::map,tenant):jdbc.query("SELECT * FROM folders WHERE tenant_id=? AND parent_id=? ORDER BY name",this::map,tenant,parent);}
 public void update(UUID id,UUID tenant,String name,UUID parent){jdbc.update("UPDATE folders SET name=?,parent_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",name,parent,id,tenant);}
 private Folder map(java.sql.ResultSet r,int n)throws java.sql.SQLException{Timestamp c=r.getTimestamp("created_at"),u=r.getTimestamp("updated_at");return new Folder(r.getObject("id",UUID.class),r.getObject("tenant_id",UUID.class),r.getObject("parent_id",UUID.class),r.getString("name"),c.toInstant(),u.toInstant());}
}
