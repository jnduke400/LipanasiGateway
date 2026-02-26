package com.hybrid9.pg.Lipanasi.repositories;


import com.hybrid9.pg.Lipanasi.entities.ApiCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
@Repository
public interface ApiCredentialRepository extends JpaRepository<ApiCredential, Long> {
    ApiCredential findById(long id);
    ApiCredential findByType(String type);

    @Query(value = "SELECT ac.* FROM api_credential ac WHERE ac.type LIKE %?1% AND ac.url LIKE %?2% AND ac.user_key LIKE %?3% AND ac.pass_key LIKE %?4% AND ac.username LIKE %?5% AND ac.password LIKE %?6%", countQuery = "SELECT COUNT(*) FROM api_credential ac WHERE ac.type LIKE %?1% AND ac.url LIKE %?2% AND ac.user_key LIKE %?3% AND ac.pass_key LIKE %?4% AND ac.username LIKE %?5% AND ac.password LIKE %?6%", nativeQuery = true)
    Page<ApiCredential> findAllByTypeAndUrlAndUserKeyAndPassKeyAndUsernameAndPassword(String type, String url, String userKey, String passKey, String username, String password, Pageable pageable);
}