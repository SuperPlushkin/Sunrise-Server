package com.sunrise.repository;

import com.sunrise.entity.db.LoginHistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> { }
