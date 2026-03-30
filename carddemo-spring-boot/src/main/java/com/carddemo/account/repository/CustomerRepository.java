package com.carddemo.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carddemo.account.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
