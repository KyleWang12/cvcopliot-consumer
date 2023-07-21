package com.cvcopilot.resumebuilding.repository;

import com.cvcopilot.resumebuilding.models.userProfile.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileRepository extends MongoRepository<UserProfile, Long> {
}
