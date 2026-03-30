package com.carddemo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapped from COBOL copybook CSUSR01Y (SEC-USER-DATA).
 * Backed by VSAM file USRSEC.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    @Column(name = "first_name", length = 20)
    private String firstName;

    @Column(name = "last_name", length = 20)
    private String lastName;

    @Column(name = "password", length = 60)
    private String password;

    @Column(name = "user_type", length = 1)
    private String userType;

    protected User() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public boolean isAdmin() {
        return "A".equalsIgnoreCase(userType);
    }
}
