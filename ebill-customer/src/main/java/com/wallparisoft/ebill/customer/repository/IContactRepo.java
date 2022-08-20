package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContactRepo extends JpaRepository<Contact, Long> {

}
