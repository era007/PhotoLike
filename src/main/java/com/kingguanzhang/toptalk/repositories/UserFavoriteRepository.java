package com.kingguanzhang.toptalk.repositories;

import com.kingguanzhang.toptalk.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite,Long> {
}
