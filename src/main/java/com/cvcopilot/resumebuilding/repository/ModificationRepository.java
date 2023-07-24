package com.cvcopilot.resumebuilding.repository;

import com.cvcopilot.resumebuilding.models.Modification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModificationRepository extends JpaRepository<Modification, String> {
  Optional<Modification> findByModificationId(String modificationId);

  Boolean existsByModificationId(String modificationId);
}
